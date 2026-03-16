package com.guestbot.telegram.handler;

import com.guestbot.core.entity.Hotel;
import com.guestbot.service.claude.ClaudeService;
import com.guestbot.service.hotel.HotelService;
import com.guestbot.telegram.session.ConversationSession;
import com.guestbot.telegram.session.SessionManager;
import com.guestbot.telegram.session.SessionState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiHandler {

    private final TelegramClient telegramClient;
    private final SessionManager sessionManager;
    private final HotelService hotelService;
    private final ClaudeService claudeService;

    // ── Discovery: гость ещё не выбрал отель ───────────────────────────────

    public void sendPlatformWelcome(Long chatId) {
        telegramClient.sendMessage(chatId,
            "👋 Добро пожаловать!\n\n" +
            "Я помогу вам найти и забронировать гостиницу.\n\n" +
            "Расскажите, что вы ищете:\n" +
            "📍 Город или регион\n" +
            "📅 Даты заезда и выезда\n" +
            "👥 Количество гостей\n" +
            "💰 Примерный бюджет\n\n" +
            "Или просто напишите в свободной форме!");
    }

    public void handleDiscovery(Long chatId, String text, ConversationSession session) {
        telegramClient.sendTyping(chatId);

        List<Hotel> hotels = hotelService.getActiveBotHotels();
        if (hotels.isEmpty()) {
            telegramClient.sendMessage(chatId,
                "К сожалению, сейчас нет доступных гостиниц. Попробуйте позже.");
            return;
        }

        String reply = claudeService.discover(hotels, session.getHistory(), text);
        telegramClient.sendMessage(chatId, reply);
        sessionManager.addMessage(chatId, "user", text);
        sessionManager.addMessage(chatId, "assistant", reply);

        sessionManager.updateState(chatId, SessionState.SELECTING_HOTEL);
        sessionManager.addMessage(chatId, "system", "hotels:" +
            hotels.stream().map(h -> h.getId() + "=" + h.getName())
                           .reduce((a, b) -> a + "," + b).orElse(""));
    }

    // ── Выбор отеля из списка ────────────────────────────────────────────────

    public void handleHotelSelection(Long chatId, String text, ConversationSession session) {
        List<Hotel> hotels = hotelService.getActiveBotHotels();

        try {
            int idx = Integer.parseInt(text.trim()) - 1;
            if (idx < 0 || idx >= hotels.size()) throw new NumberFormatException();

            Hotel chosen = hotels.get(idx);
            sessionManager.setHotel(chatId, chosen.getId());
            sendWelcome(chosen, chatId);
        } catch (NumberFormatException e) {
            telegramClient.sendMessage(chatId,
                "Пожалуйста, введите номер гостиницы из списка (например: *1*)");
        }
    }

    // ── Чат по конкретному отелю ─────────────────────────────────────────────

    public void handle(Hotel hotel, Long chatId, String text, ConversationSession session) {
        telegramClient.sendTyping(chatId);

        String reply = claudeService.chat(hotel, session.getHistory(), text);
        telegramClient.sendMessage(chatId, reply);
        sessionManager.addMessage(chatId, "user", text);
        sessionManager.addMessage(chatId, "assistant", reply);
    }

    public void sendWelcome(Hotel hotel, Long chatId) {
        String welcome = hotel.getWelcomeMessage() != null
            ? hotel.getWelcomeMessage()
            : "✅ Вы выбрали *" + hotel.getName() + "*!\n\n" +
              "Могу помочь с:\n" +
              "• Информацией о номерах и ценах\n" +
              "• Проверкой доступности\n" +
              "• Оформлением бронирования\n\n" +
              "Задайте ваш вопрос!";

        telegramClient.sendMessage(chatId, welcome);
    }

    public void sendHelp(Hotel hotel, Long chatId) {
        telegramClient.sendMessage(chatId,
            "Я могу помочь вам:\n\n" +
            "🏨 Рассказать о гостинице и номерах\n" +
            "📅 Проверить доступность на ваши даты\n" +
            "📋 Оформить бронирование\n" +
            "💳 Принять оплату\n\n" +
            "/start - Начать сначала\n" +
            "/cancel - Отменить текущее действие");
    }

    public void sendMessage(Long chatId, String text) {
        telegramClient.sendMessage(chatId, text);
    }
}
