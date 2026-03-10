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
        ConversationSession session = sessionManager.getOrCreate(hotel.getId(), chatId);

        // Команды всегда обрабатываются независимо от состояния
        if (text.startsWith("/")) {
            handleCommand(hotel, chatId, text, session);
            return;
        }

        // Если эскалировано владельцу — не отвечаем боту
        if (session.getState() == SessionState.ESCALATED_TO_OWNER) {
            return;
        }

        // Если в процессе сбора данных бронирования
        if (isCollectingBookingData(session.getState())) {
            bookingFlowHandler.handle(hotel, chatId, text, session);
            return;
        }

        // Всё остальное — через Claude
        aiHandler.handle(hotel, chatId, text, session);
    }

    private void handleCommand(Hotel hotel, Long chatId, String text,
                               ConversationSession session) {
        switch (text.split(" ")[0]) {
            case "/start" -> {
                sessionManager.clearSession(hotel.getId(), chatId);
                aiHandler.sendWelcome(hotel, chatId);
            }
            case "/help" -> aiHandler.sendHelp(hotel, chatId);
            case "/cancel" -> {
                sessionManager.clearSession(hotel.getId(), chatId);
                aiHandler.sendMessage(hotel.getTelegramBotToken(), chatId,
                    "Хорошо, начнём сначала. Чем могу помочь?");
            }
            default -> aiHandler.handle(hotel, chatId, text, session);
        }
    }

    private boolean isCollectingBookingData(SessionState state) {
        return state == SessionState.COLLECTING_GUEST_NAME
            || state == SessionState.COLLECTING_GUEST_PHONE
            || state == SessionState.COLLECTING_CHECK_IN
            || state == SessionState.COLLECTING_CHECK_OUT;
    }
}
