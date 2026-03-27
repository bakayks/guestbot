package com.guestbot.telegram.handler;

import com.guestbot.core.entity.Booking;
import com.guestbot.core.entity.Hotel;
import com.guestbot.core.entity.Room;
import com.guestbot.core.entity.RoomPhoto;
import com.guestbot.core.exception.RoomNotAvailableException;
import com.guestbot.service.booking.BookingService;
import com.guestbot.service.calendar.CalendarService;
import com.guestbot.service.conversation.ConversationService;
import com.guestbot.telegram.session.ConversationSession;
import com.guestbot.telegram.session.SessionManager;
import com.guestbot.telegram.session.SessionState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingFlowHandler {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    // Русские названия месяцев (именительный + родительный падеж)
    private static final Map<String, Integer> RU_MONTHS = Map.ofEntries(
        Map.entry("январь", 1),  Map.entry("января", 1),
        Map.entry("февраль", 2), Map.entry("февраля", 2),
        Map.entry("март", 3),    Map.entry("марта", 3),
        Map.entry("апрель", 4),  Map.entry("апреля", 4),
        Map.entry("май", 5),     Map.entry("мая", 5),
        Map.entry("июнь", 6),    Map.entry("июня", 6),
        Map.entry("июль", 7),    Map.entry("июля", 7),
        Map.entry("август", 8),  Map.entry("августа", 8),
        Map.entry("сентябрь", 9),Map.entry("сентября", 9),
        Map.entry("октябрь", 10),Map.entry("октября", 10),
        Map.entry("ноябрь", 11), Map.entry("ноября", 11),
        Map.entry("декабрь", 12),Map.entry("декабря", 12)
    );

    private static final Object CANCEL_KEYBOARD = TelegramClient.replyKeyboard(
        List.of(List.of("❌ Отмена"))
    );

    private final TelegramClient telegramClient;
    private final SessionManager sessionManager;
    private final CalendarService calendarService;
    private final BookingService bookingService;
    private final ConversationService conversationService;

    // ── Точка входа: показываем все комнаты ───────────────────────────────────

    public void startBookingFlow(Hotel hotel, Long chatId) {
        ConversationSession session = sessionManager.getOrCreate(chatId);
        showAllRooms(hotel, chatId, session);
    }

    /** Просмотр номеров без начала бронирования. Состояние сессии не меняется. */
    public void browseRooms(Hotel hotel, Long chatId) {
        List<Room> rooms = calendarService.getAllActiveRooms(hotel.getId());
        if (rooms.isEmpty()) {
            telegramClient.sendMessage(chatId, "😔 В этом отеле пока нет доступных номеров.");
            return;
        }

        ConversationSession session = sessionManager.getOrCreate(chatId);
        LocalDate checkIn  = session.getCheckIn();
        LocalDate checkOut = session.getCheckOut();
        boolean hasDates   = checkIn != null && checkOut != null && checkOut.isAfter(checkIn)
                             && !checkIn.isBefore(LocalDate.now());

        String header = "🛏 *Номера " + hotel.getName() + ":*";
        if (hasDates) {
            header += "\n📅 " + checkIn.format(DATE_FORMAT) + " — " + checkOut.format(DATE_FORMAT);
        }
        telegramClient.sendMessage(chatId, header);

        for (Room room : rooms) {
            String caption = "*" + room.getType() + "*";
            if (room.getDescription() != null) caption += "\n" + room.getDescription();
            caption += "\n\n💰 " + room.getPricePerNight() + " сом/ночь"
                + " · 👥 " + room.getCapacity() + " чел.";

            if (hasDates) {
                boolean available = calendarService.isRoomAvailable(room.getId(), checkIn, checkOut);
                caption += available
                    ? "\n\n✅ Свободен на ваши даты"
                    : "\n\n❌ Занят на выбранные даты";
            }

            var keyboard = TelegramClient.inlineKeyboard(List.of(List.of(
                TelegramClient.btn("📅 Забронировать этот номер", "room:" + room.getId())
            )));

            String photoUrl = room.getPhotos().stream()
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

    // ── Обработка входящих сообщений в процессе бронирования ─────────────────

    public void handle(Hotel hotel, Long chatId, String text, ConversationSession session) {
        switch (session.getState()) {
            case COLLECTING_CHECK_IN  -> handleCheckIn(chatId, text, session);
            case COLLECTING_CHECK_OUT -> handleCheckOut(hotel, chatId, text, session);
            case COLLECTING_GUEST_NAME  -> handleName(chatId, text, session);
            case COLLECTING_GUEST_PHONE -> handlePhone(chatId, text, session);
        }
    }

    // ── Callback: пользователь выбрал комнату ─────────────────────────────────

    public void handleRoomSelection(Long chatId, Long roomId, ConversationSession session) {
        Room room = calendarService.getRoom(roomId);
        session.setRoomId(roomId);
        session.setRoomName(room.getType());
        sessionManager.save(session);

        LocalDate today = LocalDate.now();
        boolean checkInValid  = session.getCheckIn() != null && !session.getCheckIn().isBefore(today);
        boolean checkOutValid = checkInValid && session.getCheckOut() != null
                                && session.getCheckOut().isAfter(session.getCheckIn());

        if (checkOutValid) {
            // Даты уже есть — сразу к имени
            session.setState(SessionState.COLLECTING_GUEST_NAME);
            sessionManager.save(session);
            askName(chatId, session);
        } else if (checkInValid) {
            session.setState(SessionState.COLLECTING_CHECK_OUT);
            sessionManager.save(session);
            telegramClient.sendMessage(chatId,
                "Дата заезда: *" + session.getCheckIn().format(DATE_FORMAT) + "*\n\nВведите дату выезда:",
                CANCEL_KEYBOARD);
        } else {
            session.setState(SessionState.COLLECTING_CHECK_IN);
            sessionManager.save(session);
            telegramClient.sendMessage(chatId,
                "Номер выбран: *" + room.getType() + "*\n\n" +
                "Введите дату заезда (например: *15.06.2026* или *15 июня 2026*):",
                CANCEL_KEYBOARD);
        }
    }

    // ── Callback: пользователь поделился контактом ────────────────────────────

    public void handleContactShared(Long chatId, String phone, ConversationSession session) {
        phone = phone.replaceAll("[^+\\d]", "");
        session.setGuestPhone(phone);
        sessionManager.save(session);
        if (session.getHotelId() != null)
            conversationService.saveGuestMessageWithContact(
                session.getHotelId(), chatId, phone, session.getGuestName(), phone);
        showConfirmation(chatId, session);
    }

    // ── Callback: подтвердить бронирование ────────────────────────────────────

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
                "Попробуйте выбрать другой номер.",
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

    // ── Показ всех комнат ─────────────────────────────────────────────────────

    private void showAllRooms(Hotel hotel, Long chatId, ConversationSession session) {
        List<Room> rooms = calendarService.getAllActiveRooms(hotel.getId());

        if (rooms.isEmpty()) {
            telegramClient.sendMessage(chatId,
                "😔 К сожалению, в этом отеле нет доступных номеров.",
                TelegramClient.replyKeyboard(List.of(List.of("🔄 Сменить отель"))));
            return;
        }

        session.setState(SessionState.SELECTING_ROOM);
        sessionManager.save(session);

        StringBuilder header = new StringBuilder("🏨 *Выберите номер для бронирования:*");
        if (session.getCheckIn() != null) {
            header.append("\n\n✅ Дата заезда: *").append(session.getCheckIn().format(DATE_FORMAT)).append("*");
            if (session.getCheckOut() != null)
                header.append("\n✅ Дата выезда: *").append(session.getCheckOut().format(DATE_FORMAT)).append("*");
        }
        telegramClient.sendMessage(chatId, header.toString(), TelegramClient.removeKeyboard());

        boolean hasDates = session.getCheckIn() != null && session.getCheckOut() != null
                           && session.getCheckOut().isAfter(session.getCheckIn());

        for (Room room : rooms) {
            String caption = "*" + room.getType() + "*";
            if (room.getDescription() != null) caption += "\n" + room.getDescription();
            caption += "\n\n💰 " + room.getPricePerNight() + " сом/ночь"
                + " · 👥 " + room.getCapacity() + " чел.";

            if (hasDates) {
                boolean available = calendarService.isRoomAvailable(
                    room.getId(), session.getCheckIn(), session.getCheckOut());
                caption += available
                    ? "\n\n✅ Свободен на ваши даты"
                    : "\n\n❌ Занят на выбранные даты";
            }

            var keyboard = TelegramClient.inlineKeyboard(List.of(List.of(
                TelegramClient.btn("✅ Выбрать этот номер", "room:" + room.getId())
            )));

            String photoUrl = room.getPhotos().stream()
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

    // ── Шаги сбора данных ─────────────────────────────────────────────────────

    private void handleCheckIn(Long chatId, String text, ConversationSession session) {
        LocalDate checkIn = parseDate(text);
        if (checkIn == null) {
            telegramClient.sendMessage(chatId,
                "Не могу распознать дату. Попробуйте: *15.06.2026* или *15 июня 2026*",
                CANCEL_KEYBOARD);
            return;
        }
        if (checkIn.isBefore(LocalDate.now())) {
            telegramClient.sendMessage(chatId,
                "Дата заезда не может быть в прошлом. Введите корректную дату:",
                CANCEL_KEYBOARD);
            return;
        }
        if (checkIn.isAfter(LocalDate.now().plusYears(1))) {
            telegramClient.sendMessage(chatId,
                "Бронирование доступно только на ближайший год (до *"
                + LocalDate.now().plusYears(1).format(DATE_FORMAT) + "*).\n\nВведите другую дату:",
                CANCEL_KEYBOARD);
            return;
        }
        session.setCheckIn(checkIn);
        session.setState(SessionState.COLLECTING_CHECK_OUT);
        sessionManager.save(session);
        telegramClient.sendMessage(chatId,
            "Дата заезда: *" + checkIn.format(DATE_FORMAT) + "*\n\nВведите дату выезда:",
            CANCEL_KEYBOARD);
    }

    private void handleCheckOut(Hotel hotel, Long chatId, String text, ConversationSession session) {
        LocalDate checkOut = parseDate(text);
        if (checkOut == null) {
            telegramClient.sendMessage(chatId,
                "Не могу распознать дату. Попробуйте: *20.06.2026* или *20 июня 2026*",
                CANCEL_KEYBOARD);
            return;
        }
        if (!checkOut.isAfter(session.getCheckIn())) {
            telegramClient.sendMessage(chatId,
                "Дата выезда должна быть позже даты заезда. Введите корректную дату:",
                CANCEL_KEYBOARD);
            return;
        }

        // Проверяем доступность выбранного номера
        if (session.getRoomId() != null
                && !calendarService.isRoomAvailable(session.getRoomId(), session.getCheckIn(), checkOut)) {
            telegramClient.sendMessage(chatId,
                "😔 Номер *" + session.getRoomName() + "* занят на эти даты.\n\n" +
                "Попробуйте другие даты — введите новую дату заезда:",
                CANCEL_KEYBOARD);
            session.setCheckIn(null);
            session.setState(SessionState.COLLECTING_CHECK_IN);
            sessionManager.save(session);
            return;
        }

        session.setCheckOut(checkOut);
        session.setState(SessionState.COLLECTING_GUEST_NAME);
        sessionManager.save(session);
        askName(chatId, session);
    }

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
            "Спасибо, *" + text.trim() + "*!\n\n" + PHONE_PROMPT,
            TelegramClient.requestContactKeyboard());
    }

    private static final String PHONE_PROMPT =
        "Введите номер телефона или нажмите кнопку ниже:\n\n" +
        "🇰🇬 *+996 550 123 456*\n" +
        "🇷🇺 *+7 999 123 45 67*\n" +
        "🇰🇿 *+7 707 123 45 67*\n" +
        "🇺🇿 *+998 90 123 45 67*";

    private void handlePhone(Long chatId, String text, ConversationSession session) {
        String phone = text.replaceAll("[^+\\d]", "");
        if (!isValidPhone(phone)) {
            telegramClient.sendMessage(chatId,
                "Номер не распознан. Попробуйте ещё раз:\n\n" +
                "🇰🇬 *+996 550 123 456*\n" +
                "🇷🇺 *+7 999 123 45 67*\n" +
                "🇰🇿 *+7 707 123 45 67*\n" +
                "🇺🇿 *+998 90 123 45 67*",
                TelegramClient.requestContactKeyboard());
            return;
        }
        session.setGuestPhone(phone);
        sessionManager.save(session);

        if (session.getHotelId() != null)
            conversationService.saveGuestMessageWithContact(
                session.getHotelId(), chatId, phone, session.getGuestName(), phone);

        showConfirmation(chatId, session);
    }

    private void askName(Long chatId, ConversationSession session) {
        int nights = session.getCheckIn().until(session.getCheckOut()).getDays();
        String msg = "📅 *" + session.getCheckIn().format(DATE_FORMAT)
            + " — " + session.getCheckOut().format(DATE_FORMAT)
            + "* (" + nights + " " + nightsWord(nights) + ")\n\n"
            + "Введите ваше *имя*:";
        telegramClient.sendMessage(chatId, msg, CANCEL_KEYBOARD);
    }

    private void showConfirmation(Long chatId, ConversationSession session) {
        int nights = session.getCheckIn().until(session.getCheckOut()).getDays();

        // Считаем сумму если знаем цену
        String totalLine = "";
        try {
            Room room = calendarService.getRoom(session.getRoomId());
            BigDecimal total = room.getPricePerNight().multiply(BigDecimal.valueOf(nights));
            totalLine = "💰 Сумма: *" + total + " сом* (" + room.getPricePerNight() + " × " + nights + " ночей)\n";
        } catch (Exception ignored) {}

        var keyboard = TelegramClient.inlineKeyboard(List.of(List.of(
            TelegramClient.btn("✅ Подтвердить", "confirm_booking"),
            TelegramClient.btn("❌ Отмена", "cancel_booking")
        )));

        telegramClient.sendMessage(chatId,
            "📋 *Проверьте данные бронирования:*\n\n" +
            "🏨 Номер: *" + (session.getRoomName() != null ? session.getRoomName() : "") + "*\n" +
            "📅 Заезд: *" + session.getCheckIn().format(DATE_FORMAT) + "*\n" +
            "📅 Выезд: *" + session.getCheckOut().format(DATE_FORMAT) + "*\n" +
            "🌙 Ночей: *" + nights + "*\n" +
            totalLine +
            "👤 Имя: *" + session.getGuestName() + "*\n" +
            "📞 Телефон: *" + session.getGuestPhone() + "*\n\n" +
            "Всё верно?",
            keyboard);
    }

    // ── Парсинг дат в разных форматах ────────────────────────────────────────

    static LocalDate parseDate(String raw) {
        if (raw == null) return null;
        String text = raw.trim().toLowerCase()
            .replaceAll("\\s+", " ")
            .replaceAll("[,.]$", "");

        // Числовые форматы: d.M.yyyy / d/M/yyyy / yyyy-MM-dd / d-M-yyyy
        List<DateTimeFormatter> numericFmts = List.of(
            DateTimeFormatter.ofPattern("d.M.yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-M-d"),
            DateTimeFormatter.ofPattern("d-M-yyyy")
        );
        for (DateTimeFormatter fmt : numericFmts) {
            try { return LocalDate.parse(text, fmt); }
            catch (DateTimeParseException ignored) {}
        }

        // Без года: d.M / d/M — подставляем текущий или следующий год
        List<DateTimeFormatter> shortFmts = List.of(
            DateTimeFormatter.ofPattern("d.M"),
            DateTimeFormatter.ofPattern("d/M"),
            DateTimeFormatter.ofPattern("d-M")
        );
        for (DateTimeFormatter fmt : shortFmts) {
            try {
                java.time.MonthDay md = java.time.MonthDay.parse(text, fmt);
                LocalDate candidate = md.atYear(LocalDate.now().getYear());
                // если дата уже прошла в этом году — берём следующий
                if (candidate.isBefore(LocalDate.now())) candidate = candidate.plusYears(1);
                return candidate;
            } catch (DateTimeParseException ignored) {}
        }

        // Русский текстовый формат: "15 июня 2026" или "15 июня"
        String[] parts = text.split(" ");
        if (parts.length >= 2) {
            try {
                int day = Integer.parseInt(parts[0]);
                Integer month = RU_MONTHS.get(parts[1]);
                if (month != null) {
                    int year = (parts.length >= 3) ? Integer.parseInt(parts[2]) : LocalDate.now().getYear();
                    return LocalDate.of(year, month, day);
                }
            } catch (NumberFormatException | DateTimeException ignored) {}
        }

        return null;
    }

    /**
     * Валидация номера телефона по форматам KG / RU / KZ / UZ + общий международный.
     *
     * KG: +996 [5|7]\d{8}           → +996 5XX XXX XXX
     * RU: +7 [489]\d{9}  или 8...   → +7 9XX XXX XX XX
     * KZ: +7 [67]\d{9}              → +7 7XX XXX XX XX
     * UZ: +998 [3569789]\d{8}       → +998 9X XXX XX XX
     * Локальный KG без кода: 0[5|7]\d{8} (10 цифр)
     * Общий международный: +XX...   (7–15 цифр)
     */
    private boolean isValidPhone(String phone) {
        if (phone == null || phone.isBlank()) return false;

        // Кыргызстан локальный: 05XXXXXXXX или 07XXXXXXXX
        if (phone.matches("0[57]\\d{8}")) return true;

        // Кыргызстан: +996 5/7 + 8 цифр
        if (phone.matches("\\+996[57]\\d{8}")) return true;

        // Россия: +7 (4xx / 8xx / 9xx) + 9 цифр
        if (phone.matches("\\+7[3489]\\d{9}")) return true;
        // Россия локальный: 8 (4xx / 8xx / 9xx)
        if (phone.matches("8[3489]\\d{9}")) return true;

        // Казахстан: +7 (6xx / 7xx) + 9 цифр
        if (phone.matches("\\+7[67]\\d{9}")) return true;

        // Узбекистан: +998 + 9 цифр
        if (phone.matches("\\+998\\d{9}")) return true;

        // Общий международный: +X...X (7–15 цифр после +)
        if (phone.startsWith("+") && phone.substring(1).matches("\\d{7,14}")) return true;

        return false;
    }

    private String nightsWord(int n) {
        if (n % 10 == 1 && n % 100 != 11) return "ночь";
        if (n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20)) return "ночи";
        return "ночей";
    }
}