package com.guestbot.telegram.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.guestbot.core.entity.Hotel;
import com.guestbot.core.exception.ResourceNotFoundException;
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

        if (data.startsWith("resume:")) {
            Long hotelId = Long.parseLong(data.substring(7));
            Hotel hotel = findHotelOrReset(chatId, hotelId);
            if (hotel != null) {
                sessionManager.setHotel(chatId, hotelId);
                aiHandler.sendWelcome(hotel, chatId);
            }

        } else if (data.equals("restart_session")) {
            sessionManager.clearSession(chatId);
            aiHandler.sendPlatformWelcome(chatId);

        } else if (data.startsWith("region:")) {
            handleRegionSelect(chatId, data.substring(7), session);

        } else if (data.startsWith("hotel:")) {
            handleHotelSelect(chatId, data);

        } else if (data.startsWith("room:")) {
            Long roomId = Long.parseLong(data.substring(5));
            bookingFlowHandler.handleRoomSelection(chatId, roomId, session);

        } else if (data.equals("book")) {
            if (session.getHotelId() == null) return;
            Hotel hotel = findHotelOrReset(chatId, session.getHotelId());
            if (hotel == null) return;
            bookingFlowHandler.startBookingFlow(hotel, chatId);

        } else if (data.equals("help")) {
            Hotel hotel = session.getHotelId() != null
                ? findHotelOrReset(chatId, session.getHotelId()) : null;
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
                Hotel hotel = findHotelOrReset(chatId, session.getHotelId());
                if (hotel != null) aiHandler.sendWelcome(hotel, chatId);
            }

        } else if (data.equals("confirm_booking")) {
            bookingFlowHandler.confirmBooking(chatId, session);
            if (session.getHotelId() != null) {
                Hotel hotel = findHotelOrReset(chatId, session.getHotelId());
                if (hotel != null) aiHandler.sendWelcome(hotel, chatId);
            }

        } else {
            log.warn("Unknown callback data: {}", data);
        }
    }

    private void handleRegionSelect(Long chatId, String region, ConversationSession session) {
        if ("другой".equals(region)) {
            telegramClient.sendMessage(chatId,
                "Напишите куда хотите поехать — город, регион или название места:");
            return;
        }
        // Имитируем текстовый ввод региона — запускаем discovery с этим текстом
        aiHandler.handleDiscovery(chatId, region, session);
    }

    private void handleHotelSelect(Long chatId, String data) {
        try {
            Long hotelId = Long.parseLong(data.substring(6));
            Hotel hotel = hotelService.getById(hotelId);
            sessionManager.setHotel(chatId, hotelId);
            aiHandler.sendWelcome(hotel, chatId);
        } catch (Exception e) {
            log.error("Failed to select hotel from callback: {}", data, e);
            telegramClient.sendMessage(chatId, "Не удалось выбрать гостиницу. Попробуйте снова.");
        }
    }

    /** Загружает отель; если не найден — сбрасывает сессию и возвращает null. */
    private Hotel findHotelOrReset(Long chatId, Long hotelId) {
        try {
            return hotelService.getById(hotelId);
        } catch (ResourceNotFoundException e) {
            log.warn("Hotel {} not found, resetting session for chatId={}", hotelId, chatId);
            sessionManager.clearSession(chatId);
            aiHandler.sendPlatformWelcome(chatId);
            return null;
        }
    }
}
