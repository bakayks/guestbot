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
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingFlowHandler {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final TelegramClient telegramClient;
    private final SessionManager sessionManager;

    private static final Object CANCEL_KEYBOARD = TelegramClient.replyKeyboard(
        List.of(List.of("❌ Отмена"))
    );

    public void startBookingFlow(Hotel hotel, Long chatId) {
        sessionManager.updateState(chatId, SessionState.COLLECTING_GUEST_NAME);
        telegramClient.sendMessage(chatId,
            "Отлично! Давайте оформим бронирование.\n\nВведите ваше имя:",
            CANCEL_KEYBOARD);
    }

    public void handle(Hotel hotel, Long chatId, String text, ConversationSession session) {
        switch (session.getState()) {
            case COLLECTING_GUEST_NAME -> handleName(chatId, text, session);
            case COLLECTING_GUEST_PHONE -> handlePhone(chatId, text, session);
            case COLLECTING_CHECK_IN -> handleCheckIn(chatId, text, session);
            case COLLECTING_CHECK_OUT -> handleCheckOut(chatId, text, session);
        }
    }

    private void handleName(Long chatId, String text, ConversationSession session) {
        if (text.trim().length() < 2) {
            telegramClient.sendMessage(chatId, "Пожалуйста, введите корректное имя:");
            return;
        }

        session.setGuestName(text.trim());
        session.setState(SessionState.COLLECTING_GUEST_PHONE);
        sessionManager.save(session);

        telegramClient.sendMessage(chatId,
            "Спасибо, *" + text.trim() + "*!\n\nВведите ваш номер телефона:",
            CANCEL_KEYBOARD);
    }

    private void handlePhone(Long chatId, String text, ConversationSession session) {
        String phone = text.replaceAll("[^+\\d]", "");
        if (phone.length() < 9) {
            telegramClient.sendMessage(chatId,
                "Введите корректный номер телефона (например: +996 550 123456):");
            return;
        }

        session.setGuestPhone(phone);
        session.setState(SessionState.COLLECTING_CHECK_IN);
        sessionManager.save(session);

        telegramClient.sendMessage(chatId,
            "Введите дату заезда в формате *дд.мм.гггг*\n(например: 15.06.2026):",
            CANCEL_KEYBOARD);
    }

    private void handleCheckIn(Long chatId, String text, ConversationSession session) {
        try {
            LocalDate checkIn = LocalDate.parse(text.trim(), DATE_FORMAT);
            if (checkIn.isBefore(LocalDate.now())) {
                telegramClient.sendMessage(chatId,
                    "Дата заезда не может быть в прошлом. Введите корректную дату:");
                return;
            }

            session.setCheckIn(checkIn);
            session.setState(SessionState.COLLECTING_CHECK_OUT);
            sessionManager.save(session);

            telegramClient.sendMessage(chatId,
                "Дата заезда: *" + checkIn.format(DATE_FORMAT) + "*\n\nВведите дату выезда:",
                CANCEL_KEYBOARD);
        } catch (DateTimeParseException e) {
            telegramClient.sendMessage(chatId,
                "Неверный формат даты. Введите в формате *дд.мм.гггг*:");
        }
    }

    private void handleCheckOut(Long chatId, String text, ConversationSession session) {
        try {
            LocalDate checkOut = LocalDate.parse(text.trim(), DATE_FORMAT);

            if (session.getCheckIn() == null || !checkOut.isAfter(session.getCheckIn())) {
                telegramClient.sendMessage(chatId,
                    "Дата выезда должна быть позже даты заезда. Введите корректную дату:");
                return;
            }

            session.setCheckOut(checkOut);
            sessionManager.save(session);

            var confirmKeyboard = TelegramClient.inlineKeyboard(List.of(
                List.of(
                    TelegramClient.btn("✅ Подтвердить", "confirm_booking"),
                    TelegramClient.btn("❌ Отмена", "cancel_booking")
                )
            ));

            telegramClient.sendMessage(chatId,
                "📋 *Проверьте данные бронирования:*\n\n" +
                "👤 Имя: *" + session.getGuestName() + "*\n" +
                "📞 Телефон: *" + session.getGuestPhone() + "*\n" +
                "📅 Заезд: *" + session.getCheckIn().format(DATE_FORMAT) + "*\n" +
                "📅 Выезд: *" + checkOut.format(DATE_FORMAT) + "*\n\n" +
                "Всё верно?",
                confirmKeyboard);
        } catch (DateTimeParseException e) {
            telegramClient.sendMessage(chatId,
                "Неверный формат даты. Введите в формате *дд.мм.гггг*:");
        }
    }
}
