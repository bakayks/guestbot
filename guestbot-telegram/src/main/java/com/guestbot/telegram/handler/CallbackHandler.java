package com.guestbot.telegram.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.guestbot.core.entity.Hotel;
import com.guestbot.service.hotel.HotelService;
import com.guestbot.telegram.session.ConversationSession;
import com.guestbot.telegram.session.SessionManager;
import com.guestbot.telegram.session.SessionState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CallbackHandler {

    private final TelegramClient telegramClient;
    private final SessionManager sessionManager;
    private final HotelService hotelService;
    private final AiHandler aiHandler;
    private final BookingFlowHandler bookingFlowHandler;

    public void handle(JsonNode callbackQuery) {
        String callbackQueryId = callbackQuery.get("id").asText();
        Long chatId = callbackQuery.get("message").get("chat").get("id").asLong();
        String data = callbackQuery.has("data") ? callbackQuery.get("data").asText() : "";

        telegramClient.answerCallbackQuery(callbackQueryId);

        log.info("Callback from chatId={}: {}", chatId, data);

        ConversationSession session = sessionManager.getOrCreate(chatId);

        if (data.startsWith("hotel:")) {
            handleHotelSelect(chatId, data);

        } else if (data.equals("book")) {
            if (session.getHotelId() == null) return;
            Hotel hotel = hotelService.getById(session.getHotelId());
            bookingFlowHandler.startBookingFlow(hotel, chatId);

        } else if (data.equals("help")) {
            Hotel hotel = session.getHotelId() != null
                ? hotelService.getById(session.getHotelId()) : null;
            aiHandler.sendHelp(hotel, chatId);

        } else if (data.equals("change_hotel")) {
            sessionManager.clearSession(chatId);
            aiHandler.sendPlatformWelcome(chatId);

        } else if (data.equals("cancel_booking")) {
            sessionManager.updateState(chatId, SessionState.IDLE);
            telegramClient.sendMessage(chatId,
                "Бронирование отменено.",
                TelegramClient.removeKeyboard());
            if (session.getHotelId() != null) {
                Hotel hotel = hotelService.getById(session.getHotelId());
                aiHandler.sendWelcome(hotel, chatId);
            }

        } else if (data.equals("confirm_booking")) {
            handleConfirmBooking(chatId, session);

        } else {
            log.warn("Unknown callback data: {}", data);
        }
    }

    private void handleHotelSelect(Long chatId, String data) {
        try {
            Long hotelId = Long.parseLong(data.substring(6));
            Hotel hotel = hotelService.getById(hotelId);
            sessionManager.setHotel(chatId, hotelId);
            aiHandler.sendWelcome(hotel, chatId);
        } catch (Exception e) {
            log.error("Failed to select hotel from callback: {}", data, e);
        }
    }

    private void handleConfirmBooking(Long chatId, ConversationSession session) {
        // TODO: вызов BookingService.create и отправка ссылки на оплату
        telegramClient.sendMessage(chatId,
            "✅ Бронирование подтверждено!\n\n" +
            "Ссылка на оплату будет отправлена в ближайшее время.",
            TelegramClient.removeKeyboard());
        sessionManager.updateState(chatId, SessionState.AWAITING_PAYMENT);
    }
}
