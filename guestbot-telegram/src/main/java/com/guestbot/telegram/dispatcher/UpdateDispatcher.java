package com.guestbot.telegram.dispatcher;

import com.fasterxml.jackson.databind.JsonNode;
import com.guestbot.core.entity.Hotel;
import com.guestbot.service.hotel.HotelService;
import com.guestbot.telegram.handler.MessageHandler;
import com.guestbot.telegram.session.ConversationSession;
import com.guestbot.telegram.session.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateDispatcher {

    private final HotelService hotelService;
    private final MessageHandler messageHandler;
    private final SessionManager sessionManager;

    public void dispatch(JsonNode update) {
        if (update.has("message")) {
            handleMessage(update.get("message"));
        } else if (update.has("callback_query")) {
            // TODO: обработка inline кнопок
            log.debug("Callback query received");
        }
    }

    private void handleMessage(JsonNode message) {
        Long chatId = message.get("chat").get("id").asLong();
        String text = message.has("text") ? message.get("text").asText() : "";

        log.debug("Message from chatId={}: {}", chatId, text);

        ConversationSession session = sessionManager.getOrCreate(chatId);

        // Отель уже выбран — проверяем что он ещё активен
        Hotel hotel = null;
        if (session.getHotelId() != null) {
            hotel = hotelService.getById(session.getHotelId());
            if (!hotel.getBotActive()) {
                log.debug("Bot is inactive for hotel {}", hotel.getId());
                return;
            }
        }

        messageHandler.handle(hotel, chatId, text, message);
    }
}
