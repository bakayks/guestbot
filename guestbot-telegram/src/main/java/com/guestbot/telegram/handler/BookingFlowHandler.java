package com.guestbot.telegram.handler;

import com.guestbot.core.entity.Booking;
import com.guestbot.core.entity.Hotel;
import com.guestbot.core.entity.RoomPhoto;
import com.guestbot.core.exception.RoomNotAvailableException;
import com.guestbot.service.booking.BookingService;
import com.guestbot.service.calendar.CalendarService;
import com.guestbot.service.calendar.CalendarService.RoomAvailability;
import com.guestbot.service.conversation.ConversationService;
import com.guestbot.telegram.session.ConversationSession;
import com.guestbot.telegram.session.SessionManager;
import com.guestbot.telegram.session.SessionState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
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
    private final ConversationService conversationService;

    public void startBookingFlow(Hotel hotel, Long chatId) {
        ConversationSession session = sessionManager.get(chatId);

        StringBuilder msg = new StringBuilder("Отлично! Давайте оформим бронирование.");
        if (session != null && session.getCheckIn() != null) {
            msg.append("\n\nЯ уже вижу даты из вашего запроса:");
            msg.append("\n✅ Заезд: *").append(session.getCheckIn().format(DATE_FORMAT)).append("*");
            if (session.getCheckOut() != null)
                msg.append("\n✅ Выезд: *").append(session.getCheckOut().format(DATE_FORMAT)).append("*");
        }
        msg.append("\n\nВведите ваше *имя*:");

        sessionManager.updateState(chatId, SessionState.COLLECTING_GUEST_NAME);
        telegramClient.sendMessage(chatId, msg.toString(), CANCEL_KEYBOARD);
    }

    public void handle(Hotel hotel, Long chatId, String text, ConversationSession session) {
        switch (session.getState()) {
            case COLLECTING_GUEST_NAME -> handleName(chatId, text, session);
            case COLLECTING_GUEST_PHONE -> handlePhone(hotel, chatId, text, session);
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

            String confirmation = "✅ *Бронирование создано!*\n\n" +
                "📋 Номер брони: *" + booking.getBookingNumber() + "*\n" +
                "💰 Сумма к оплате: *" + booking.getTotalAmount() + " сом*\n" +
                "⏰ Оплатите в течение 24 часов\n\n" +
                "Ссылка на оплату придёт отдельным сообщением.";

            telegramClient.sendMessage(chatId, confirmation, TelegramClient.removeKeyboard());
            conversationService.saveBotMessage(session.getHotelId(), chatId, confirmation);

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

        if (session.getHotelId() != null)
            conversationService.saveGuestMessage(session.getHotelId(), chatId, text.trim());

        telegramClient.sendMessage(chatId,
            "Спасибо, *" + text.trim() + "*!\n\nВведите ваш номер телефона:",
            CANCEL_KEYBOARD);
    }

    private void handlePhone(Hotel hotel, Long chatId, String text, ConversationSession session) {
        String phone = text.replaceAll("[^+\\d]", "");
        if (phone.length() < 9) {
            telegramClient.sendMessage(chatId,
                "Введите корректный номер телефона (например: +996 550 123456):",
                CANCEL_KEYBOARD);
            return;
        }
        session.setGuestPhone(phone);

        if (session.getHotelId() != null)
            conversationService.saveGuestMessageWithContact(
                session.getHotelId(), chatId, phone, session.getGuestName(), phone);

        LocalDate today = LocalDate.now();
        boolean checkInValid = session.getCheckIn() != null && !session.getCheckIn().isBefore(today);
        boolean checkOutValid = checkInValid && session.getCheckOut() != null
            && session.getCheckOut().isAfter(session.getCheckIn());

        if (checkOutValid) {
            // Обе даты уже известны — сразу показываем номера
            sessionManager.save(session);
            showAvailableRooms(hotel, chatId, session);
        } else if (checkInValid) {
            // Дата заезда известна — спрашиваем только выезд
            session.setState(SessionState.COLLECTING_CHECK_OUT);
            sessionManager.save(session);
            telegramClient.sendMessage(chatId,
                "Дата заезда: *" + session.getCheckIn().format(DATE_FORMAT) + "*\n\nВведите дату выезда:",
                CANCEL_KEYBOARD);
        } else {
            session.setState(SessionState.COLLECTING_CHECK_IN);
            sessionManager.save(session);
            telegramClient.sendMessage(chatId,
                "Введите дату заезда в формате *дд.мм.гггг*\n(например: 15.06.2026):",
                CANCEL_KEYBOARD);
        }
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

        telegramClient.sendMessage(chatId,
            "🏨 *Доступные номера* на " + nights + " " + nightsWord(nights) + ":\n\nВыберите номер:");

        for (RoomAvailability ra : available) {
            String caption = "*" + ra.room().getType() + "*";
            if (ra.room().getDescription() != null) {
                caption += "\n" + ra.room().getDescription();
            }
            caption += "\n\n💰 " + ra.room().getPricePerNight() + " сом/ночь"
                + " · Итого: *"
                + ra.room().getPricePerNight().multiply(BigDecimal.valueOf(nights))
                + " сом*";

            var keyboard = TelegramClient.inlineKeyboard(List.of(List.of(
                TelegramClient.btn("✅ Выбрать этот номер", "room:" + ra.room().getId())
            )));

            List<RoomPhoto> photos = ra.room().getPhotos();
            String photoUrl = photos.stream()
                .min(Comparator.comparingInt(p -> p.getSortOrder() != null ? p.getSortOrder() : 0))
                .map(RoomPhoto::getUrl)
                .orElse(null);

            if (photoUrl != null) {
                telegramClient.sendPhoto(chatId, photoUrl, caption, keyboard);
            } else {
                telegramClient.sendMessage(chatId, caption, keyboard);
            }
        }
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