package com.guestbot.telegram.handler;

import com.guestbot.core.entity.Booking;
import com.guestbot.core.entity.Hotel;
import com.guestbot.core.exception.RoomNotAvailableException;
import com.guestbot.service.booking.BookingService;
import com.guestbot.service.calendar.CalendarService;
import com.guestbot.service.calendar.CalendarService.RoomAvailability;
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

    private static final Object CANCEL_KEYBOARD = TelegramClient.replyKeyboard(
        List.of(List.of("❌ Отмена"))
    );

    private final TelegramClient telegramClient;
    private final SessionManager sessionManager;
    private final CalendarService calendarService;
    private final BookingService bookingService;

    public void startBookingFlow(Hotel hotel, Long chatId) {
        sessionManager.updateState(chatId, SessionState.COLLECTING_GUEST_NAME);
        telegramClient.sendMessage(chatId,
            "Отлично! Давайте оформим бронирование.\n\nВведите ваше *имя*:",
            CANCEL_KEYBOARD);
    }

    public void handle(Hotel hotel, Long chatId, String text, ConversationSession session) {
        switch (session.getState()) {
            case COLLECTING_GUEST_NAME -> handleName(chatId, text, session);
            case COLLECTING_GUEST_PHONE -> handlePhone(chatId, text, session);
            case COLLECTING_CHECK_IN -> handleCheckIn(chatId, text, session);
            case COLLECTING_CHECK_OUT -> handleCheckOut(hotel, chatId, text, session);
        }
    }

    /** Вызывается из CallbackHandler при выборе номера через inline-кнопку. */
    public void handleRoomSelection(Long chatId, Long roomId, ConversationSession session) {
        session.setRoomId(roomId);
        sessionManager.save(session);
        showConfirmation(chatId, session);
    }

    /** Вызывается из CallbackHandler при нажатии "✅ Подтвердить". */
    public void confirmBooking(Long chatId, ConversationSession session) {
        if (session.getRoomId() == null || session.getHotelId() == null
                || session.getGuestName() == null || session.getCheckIn() == null
                || session.getCheckOut() == null) {
            telegramClient.sendMessage(chatId,
                "Ошибка: не все данные заполнены. Начните бронирование заново.",
                TelegramClient.removeKeyboard());
            sessionManager.updateState(chatId, SessionState.IDLE);
            return;
        }

        try {
            Booking booking = bookingService.create(
                session.getHotelId(),
                session.getRoomId(),
                session.getGuestName(),
                session.getGuestPhone(),
                null,
                session.getCheckIn(),
                session.getCheckOut(),
                chatId
            );

            sessionManager.updateState(chatId, SessionState.AWAITING_PAYMENT);

            telegramClient.sendMessage(chatId,
                "✅ *Бронирование создано!*\n\n" +
                "📋 Номер брони: *" + booking.getBookingNumber() + "*\n" +
                "💰 Сумма к оплате: *" + booking.getTotalAmount() + " сом*\n" +
                "⏰ Оплатите в течение 24 часов\n\n" +
                "Ссылка на оплату придёт отдельным сообщением.",
                TelegramClient.removeKeyboard());

        } catch (RoomNotAvailableException e) {
            telegramClient.sendMessage(chatId,
                "😔 К сожалению, этот номер уже занят на выбранные даты.\n\n" +
                "Попробуйте выбрать другие даты или номер.",
                TelegramClient.removeKeyboard());
            sessionManager.updateState(chatId, SessionState.IDLE);
        } catch (Exception e) {
            log.error("Failed to create booking for chatId={}", chatId, e);
            telegramClient.sendMessage(chatId,
                "Произошла ошибка при создании бронирования. Попробуйте позже.",
                TelegramClient.removeKeyboard());
            sessionManager.updateState(chatId, SessionState.IDLE);
        }
    }

    // ── Private steps ─────────────────────────────────────────────────────────

    private void handleName(Long chatId, String text, ConversationSession session) {
        if (text.trim().length() < 2) {
            telegramClient.sendMessage(chatId, "Пожалуйста, введите корректное имя:", CANCEL_KEYBOARD);
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
                "Введите корректный номер телефона (например: +996 550 123456):",
                CANCEL_KEYBOARD);
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
                    "Дата заезда не может быть в прошлом. Введите корректную дату:",
                    CANCEL_KEYBOARD);
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
                "Неверный формат даты. Введите в формате *дд.мм.гггг*:", CANCEL_KEYBOARD);
        }
    }

    private void handleCheckOut(Hotel hotel, Long chatId, String text, ConversationSession session) {
        try {
            LocalDate checkOut = LocalDate.parse(text.trim(), DATE_FORMAT);
            if (session.getCheckIn() == null || !checkOut.isAfter(session.getCheckIn())) {
                telegramClient.sendMessage(chatId,
                    "Дата выезда должна быть позже даты заезда. Введите корректную дату:",
                    CANCEL_KEYBOARD);
                return;
            }

            session.setCheckOut(checkOut);
            sessionManager.save(session);

            showAvailableRooms(hotel, chatId, session);

        } catch (DateTimeParseException e) {
            telegramClient.sendMessage(chatId,
                "Неверный формат даты. Введите в формате *дд.мм.гггг*:", CANCEL_KEYBOARD);
        }
    }

    private void showAvailableRooms(Hotel hotel, Long chatId, ConversationSession session) {
        List<RoomAvailability> available = calendarService.checkAvailability(
            hotel.getId(), session.getCheckIn(), session.getCheckOut()
        );

        if (available.isEmpty()) {
            telegramClient.sendMessage(chatId,
                "😔 К сожалению, на выбранные даты свободных номеров нет.\n\n" +
                "Попробуйте другие даты:",
                CANCEL_KEYBOARD);
            session.setState(SessionState.COLLECTING_CHECK_IN);
            session.setCheckIn(null);
            session.setCheckOut(null);
            sessionManager.save(session);
            return;
        }

        session.setState(SessionState.SELECTING_ROOM);
        sessionManager.save(session);

        int nights = session.getCheckIn().until(session.getCheckOut()).getDays();

        StringBuilder sb = new StringBuilder("🏨 *Доступные номера* на ")
            .append(nights).append(" ").append(nightsWord(nights)).append(":\n\n");

        var rows = new java.util.ArrayList<List<java.util.Map<String, String>>>();
        for (RoomAvailability ra : available) {
            sb.append("• *").append(ra.room().getType()).append("*");
            if (ra.room().getDescription() != null) {
                sb.append(" — ").append(ra.room().getDescription(), 0,
                    Math.min(60, ra.room().getDescription().length()));
            }
            sb.append("\n  💰 ").append(ra.room().getPricePerNight()).append(" сом/ночь");
            sb.append(" · Итого: *").append(
                ra.room().getPricePerNight().multiply(java.math.BigDecimal.valueOf(nights))
            ).append(" сом*\n\n");

            rows.add(List.of(TelegramClient.btn(
                ra.room().getType() + " — " + ra.room().getPricePerNight() + " сом/ночь",
                "room:" + ra.room().getId()
            )));
        }

        sb.append("Выберите номер:");
        telegramClient.sendMessage(chatId, sb.toString(), TelegramClient.inlineKeyboard(rows));
    }

    private void showConfirmation(Long chatId, ConversationSession session) {
        int nights = session.getCheckIn().until(session.getCheckOut()).getDays();

        var keyboard = TelegramClient.inlineKeyboard(List.of(
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
            "📅 Выезд: *" + session.getCheckOut().format(DATE_FORMAT) + "*\n" +
            "🌙 Ночей: *" + nights + "*\n\n" +
            "Всё верно?",
            keyboard);
    }

    private String nightsWord(int n) {
        if (n % 10 == 1 && n % 100 != 11) return "ночь";
        if (n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20)) return "ночи";
        return "ночей";
    }
}