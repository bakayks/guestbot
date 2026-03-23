package com.guestbot.telegram.dispatcher;

import com.fasterxml.jackson.databind.JsonNode;
import com.guestbot.core.entity.Hotel;
import com.guestbot.core.exception.ResourceNotFoundException;
import com.guestbot.service.hotel.HotelService;
import com.guestbot.telegram.handler.AiHandler;
import com.guestbot.telegram.handler.BookingFlowHandler;
import com.guestbot.telegram.handler.CallbackHandler;
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
    private final CallbackHandler callbackHandler;
    private final AiHandler aiHandler;
    private final BookingFlowHandler bookingFlowHandler;

    public void dispatch(JsonNode update) {
        if (update.has("message")) {
            handleMessage(update.get("message"));
        } else if (update.has("callback_query")) {
            callbackHandler.handle(update.get("callback_query"));
        }
    }

    private void handleMessage(JsonNode message) {
        Long chatId = message.get("chat").get("id").asLong();

        // Пользователь поделился контактом (кнопка "Поделиться номером")
        if (message.has("contact")) {
            JsonNode contact = message.get("contact");
            if (contact.has("phone_number")) {
                String phone = contact.get("phone_number").asText();
                log.info("Contact shared from chatId={}: {}", chatId, phone);
                ConversationSession session = sessionManager.getOrCreate(chatId);
                if (session.getState() == com.guestbot.telegram.session.SessionState.COLLECTING_GUEST_PHONE) {
                    bookingFlowHandler.handleContactShared(chatId, phone, session);
                } else {
                    session.setGuestPhone(phone.replaceAll("[^+\\d]", ""));
                    sessionManager.save(session);
                }
            }
            return;
        }

        String text = message.has("text") ? message.get("text").asText() : "";

        log.info("Message from chatId={}: {}", chatId, text);

        ConversationSession session = sessionManager.getOrCreate(chatId);

        // Отель уже выбран — проверяем что он ещё существует и активен
        Hotel hotel = null;
        if (session.getHotelId() != null) {
            try {
                hotel = hotelService.getById(session.getHotelId());
                if (!hotel.getBotActive()) {
                    log.debug("Bot is inactive for hotel {}", hotel.getId());
                    return;
                }
            } catch (ResourceNotFoundException e) {
                log.warn("Hotel {} from session not found, resetting session for chatId={}", session.getHotelId(), chatId);
                sessionManager.clearSession(chatId);
                aiHandler.sendPlatformWelcome(chatId);
                return;
            }
        }

        messageHandler.handle(hotel, chatId, text, message);
    }
}
