package com.guestbot.telegram.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.guestbot.core.entity.Conversation;
import com.guestbot.core.entity.Hotel;
import com.guestbot.service.conversation.ConversationService;
import com.guestbot.service.hotel.HotelService;
import com.guestbot.telegram.session.ConversationSession;
import com.guestbot.telegram.session.SessionManager;
import com.guestbot.telegram.session.SessionState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageHandler {

    private static final Set<String> REPLY_BUTTONS = Set.of(
        "📅 Забронировать", "🛏 Номера и цены", "❓ Помощь", "🔄 Сменить отель", "❌ Отмена"
    );

    private final SessionManager sessionManager;
    private final AiHandler aiHandler;
    private final BookingFlowHandler bookingFlowHandler;
    private final TelegramClient telegramClient;
    private final ConversationService conversationService;
    private final HotelService hotelService;

    public void handle(Hotel hotel, Long chatId, String text, JsonNode rawMessage) {
        ConversationSession session = sessionManager.getOrCreate(chatId);

        // Slash-команды обрабатываются всегда
        if (text.startsWith("/")) {
            handleCommand(hotel, chatId, text, session);
            return;
        }

        // Reply-кнопки обрабатываются всегда (кроме шагов сбора данных — только "❌ Отмена")
        if (REPLY_BUTTONS.contains(text)) {
            handleReplyButton(hotel, chatId, text, session);
            return;
        }

        // Отель ещё не выбран — всегда продолжаем discovery-диалог
        // Выбор отеля происходит только через inline-кнопки (CallbackHandler)
        if (hotel == null) {
            aiHandler.handleDiscovery(chatId, text, session);
            return;
        }

        if (session.getState() == SessionState.ESCALATED_TO_OWNER) {
            return;
        }

        if (isCollectingBookingData(session.getState())) {
            bookingFlowHandler.handle(hotel, chatId, text, session);
            return;
        }

        // Отель выбран — обычный чат по этому отелю
        aiHandler.handle(hotel, chatId, text, session);
    }

    private void handleReplyButton(Hotel hotel, Long chatId, String text, ConversationSession session) {
        switch (text) {
            case "📅 Забронировать" -> {
                if (hotel != null) bookingFlowHandler.startBookingFlow(hotel, chatId);
                else telegramClient.sendMessage(chatId,
                    "Сначала выберите гостиницу — нажмите на кнопку с её названием в сообщении выше.");
            }
            case "🛏 Номера и цены" -> {
                if (hotel != null) bookingFlowHandler.browseRooms(hotel, chatId);
                else telegramClient.sendMessage(chatId,
                    "Сначала выберите гостиницу — нажмите на кнопку с её названием в сообщении выше.");
            }
            case "❓ Помощь" -> aiHandler.sendHelp(hotel, chatId);
            case "🔄 Сменить отель" -> {
                sessionManager.clearSession(chatId);
                aiHandler.sendPlatformWelcome(chatId);
            }
            case "❌ Отмена" -> {
                sessionManager.updateState(chatId, SessionState.IDLE);
                telegramClient.sendMessage(chatId,
                    "Бронирование отменено.",
                    TelegramClient.removeKeyboard());
                if (hotel != null) aiHandler.sendWelcome(hotel, chatId);
            }
        }
    }

    private void handleCommand(Hotel hotel, Long chatId, String text, ConversationSession session) {
        switch (text.split(" ")[0]) {
            case "/start" -> {
                if (hotel != null) {
                    // Уже внутри отеля — просто сбрасываем booking flow
                    sessionManager.updateState(chatId, SessionState.IDLE);
                    aiHandler.sendWelcome(hotel, chatId);
                } else {
                    // Проверяем есть ли недавний диалог в БД
                    Optional<Conversation> recent = conversationService.findRecentConversation(chatId);
                    if (recent.isPresent()) {
                        Conversation conv = recent.get();
                        Hotel recentHotel = null;
                        try {
                            recentHotel = hotelService.getById(conv.getHotel().getId());
                        } catch (Exception ignored) {}

                        if (recentHotel != null && recentHotel.getBotActive()) {
                            String hotelName = recentHotel.getName();
                            String guestInfo = conv.getGuestName() != null
                                ? "\n👤 " + conv.getGuestName() : "";

                            var keyboard = TelegramClient.inlineKeyboard(List.of(List.of(
                                TelegramClient.btn("✅ Продолжить", "resume:" + recentHotel.getId()),
                                TelegramClient.btn("🔄 Начать заново", "restart_session")
                            )));

                            telegramClient.sendMessage(chatId,
                                "👋 С возвращением!\n\n" +
                                "Вы недавно общались с нами:\n" +
                                "🏨 *" + hotelName + "*" + guestInfo + "\n\n" +
                                "Продолжить с того места?",
                                keyboard);
                            return;
                        }
                    }
                    sessionManager.clearSession(chatId);
                    aiHandler.sendPlatformWelcome(chatId);
                }
            }
            case "/help" -> aiHandler.sendHelp(hotel, chatId);
            case "/cancel" -> {
                sessionManager.updateState(chatId, SessionState.IDLE);
                if (hotel != null) {
                    telegramClient.sendMessage(chatId, "Действие отменено.", TelegramClient.removeKeyboard());
                    aiHandler.sendWelcome(hotel, chatId);
                } else {
                    sessionManager.clearSession(chatId);
                    telegramClient.sendMessage(chatId,
                        "Хорошо, начнём сначала. Расскажите, что вы ищете?",
                        TelegramClient.removeKeyboard());
                }
            }
            default -> {
                if (hotel != null) {
                    aiHandler.handle(hotel, chatId, text, session);
                } else {
                    aiHandler.handleDiscovery(chatId, text, session);
                }
            }
        }
    }

    private boolean isCollectingBookingData(SessionState state) {
        return state == SessionState.COLLECTING_GUEST_NAME
            || state == SessionState.COLLECTING_GUEST_PHONE
            || state == SessionState.COLLECTING_CHECK_IN
            || state == SessionState.COLLECTING_CHECK_OUT
            || state == SessionState.SELECTING_ROOM;
    }
}
