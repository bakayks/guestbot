package com.guestbot.telegram.handler;

import com.guestbot.core.entity.Hotel;
import com.guestbot.telegram.session.ConversationSession;
import com.guestbot.telegram.session.SessionManager;
import com.guestbot.telegram.session.SessionState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingFlowHandler {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final TelegramClient telegramClient;
    private final SessionManager sessionManager;

    public void startBookingFlow(Hotel hotel, Long chatId, Long roomId) {
        ConversationSession session = sessionManager.getOrCreate(hotel.getId(), chatId);
        session.setState(SessionState.COLLECTING_GUEST_NAME);
        sessionManager.updateState(hotel.getId(), chatId, SessionState.COLLECTING_GUEST_NAME);

        telegramClient.sendMessage(hotel.getTelegramBotToken(), chatId,
            "Отлично! Давайте оформим бронирование.\n\nВведите ваше имя:");
    }

    public void handle(Hotel hotel, Long chatId, String text, ConversationSession session) {
        switch (session.getState()) {
            case COLLECTING_GUEST_NAME -> handleName(hotel, chatId, text, session);
            case COLLECTING_GUEST_PHONE -> handlePhone(hotel, chatId, text, session);
            case COLLECTING_CHECK_IN -> handleCheckIn(hotel, chatId, text, session);
            case COLLECTING_CHECK_OUT -> handleCheckOut(hotel, chatId, text, session);
        }
    }

    private void handleName(Hotel hotel, Long chatId, String text, ConversationSession session) {
        if (text.trim().length() < 2) {
            telegramClient.sendMessage(hotel.getTelegramBotToken(), chatId,
                "Пожалуйста, введите корректное имя:");
            return;
        }

        session.setGuestName(text.trim());
        sessionManager.updateState(hotel.getId(), chatId, SessionState.COLLECTING_GUEST_PHONE);

        telegramClient.sendMessage(hotel.getTelegramBotToken(), chatId,
            "Спасибо, *" + text.trim() + "*!\n\nВведите ваш номер телефона:");
    }

    private void handlePhone(Hotel hotel, Long chatId, String text, ConversationSession session) {
        String phone = text.replaceAll("[^+\\d]", "");
        if (phone.length() < 9) {
            telegramClient.sendMessage(hotel.getTelegramBotToken(), chatId,
                "Введите корректный номер телефона (например: +996 550 123456):");
            return;
        }

        session.setGuestPhone(phone);
        sessionManager.updateState(hotel.getId(), chatId, SessionState.COLLECTING_CHECK_IN);

        telegramClient.sendMessage(hotel.getTelegramBotToken(), chatId,
            "Введите дату заезда в формате *дд.мм.гггг*\n(например: 15.06.2026):");
    }

    private void handleCheckIn(Hotel hotel, Long chatId, String text, ConversationSession session) {
        try {
            LocalDate checkIn = LocalDate.parse(text.trim(), DATE_FORMAT);
            if (checkIn.isBefore(LocalDate.now())) {
                telegramClient.sendMessage(hotel.getTelegramBotToken(), chatId,
                    "Дата заезда не может быть в прошлом. Введите корректную дату:");
                return;
            }

            // Сохраняем через MDC или в session напрямую через другой механизм
            // Упрощенно — сохраняем в виде строки в истории
            sessionManager.addMessage(hotel.getId(), chatId, "system",
                "CHECK_IN:" + checkIn);
            sessionManager.updateState(hotel.getId(), chatId, SessionState.COLLECTING_CHECK_OUT);

            telegramClient.sendMessage(hotel.getTelegramBotToken(), chatId,
                "Дата заезда: *" + checkIn.format(DATE_FORMAT) + "*\n\n" +
                "Введите дату выезда:");
        } catch (DateTimeParseException e) {
            telegramClient.sendMessage(hotel.getTelegramBotToken(), chatId,
                "Неверный формат даты. Введите в формате *дд.мм.гггг*:");
        }
    }

    private void handleCheckOut(Hotel hotel, Long chatId, String text, ConversationSession session) {
        try {
            LocalDate checkOut = LocalDate.parse(text.trim(), DATE_FORMAT);

            // TODO: достать checkIn из истории, создать бронирование через BookingService
            telegramClient.sendMessage(hotel.getTelegramBotToken(), chatId,
                "Дата выезда: *" + checkOut.format(DATE_FORMAT) + "*\n\n" +
                "Подтверждаю бронирование...");

            sessionManager.updateState(hotel.getId(), chatId, SessionState.AWAITING_PAYMENT);
        } catch (DateTimeParseException e) {
            telegramClient.sendMessage(hotel.getTelegramBotToken(), chatId,
                "Неверный формат даты. Введите в формате *дд.мм.гггг*:");
        }
    }
}
