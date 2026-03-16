package com.guestbot.telegram.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.guestbot.core.entity.Hotel;
import com.guestbot.telegram.session.ConversationSession;
import com.guestbot.telegram.session.SessionManager;
import com.guestbot.telegram.session.SessionState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageHandler {

    private final SessionManager sessionManager;
    private final AiHandler aiHandler;
    private final BookingFlowHandler bookingFlowHandler;

    public void handle(Hotel hotel, Long chatId, String text, JsonNode rawMessage) {
        ConversationSession session = sessionManager.getOrCreate(chatId);

        // Команды обрабатываются всегда
        if (text.startsWith("/")) {
            handleCommand(hotel, chatId, text, session);
            return;
        }

        // Отель ещё не выбран
        if (hotel == null) {
            if (session.getState() == SessionState.SELECTING_HOTEL) {
                aiHandler.handleHotelSelection(chatId, text, session);
            } else {
                aiHandler.handleDiscovery(chatId, text, session);
            }
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

    private void handleCommand(Hotel hotel, Long chatId, String text, ConversationSession session) {
        switch (text.split(" ")[0]) {
            case "/start" -> {
                sessionManager.clearSession(chatId);
                if (hotel != null) {
                    aiHandler.sendWelcome(hotel, chatId);
                } else {
                    aiHandler.sendPlatformWelcome(chatId);
                }
            }
            case "/help" -> {
                if (hotel != null) {
                    aiHandler.sendHelp(hotel, chatId);
                } else {
                    aiHandler.sendPlatformWelcome(chatId);
                }
            }
            case "/cancel" -> {
                sessionManager.clearSession(chatId);
                aiHandler.sendMessage(chatId, "Хорошо, начнём сначала. Расскажите, что вы ищете?");
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
            || state == SessionState.COLLECTING_CHECK_OUT;
    }
}
