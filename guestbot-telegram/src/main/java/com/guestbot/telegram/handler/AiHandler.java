package com.guestbot.telegram.handler;

import com.guestbot.core.entity.Hotel;
import com.guestbot.service.claude.ClaudeService;
import com.guestbot.service.conversation.ConversationService;
import com.guestbot.service.hotel.HotelService;
import com.guestbot.telegram.session.ConversationSession;
import com.guestbot.telegram.session.SessionManager;
import com.guestbot.telegram.session.SessionState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiHandler {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final Pattern DATE_PATTERN = Pattern.compile("\\b(\\d{1,2}\\.\\d{1,2}\\.\\d{4})\\b");

    private final TelegramClient telegramClient;
    private final SessionManager sessionManager;
    private final HotelService hotelService;
    private final ClaudeService claudeService;
    private final ConversationService conversationService;

    // ── Discovery: гость ещё не выбрал отель ───────────────────────────────

    public void sendPlatformWelcome(Long chatId) {
        telegramClient.sendMessage(chatId,
            "👋 *Добро пожаловать в Guestbot!*\n\n" +
            "Я помогу вам найти и забронировать гостиницу.\n\n" +
            "Расскажите, что вы ищете — напишите в свободной форме или укажите:\n" +
            "📍 Город или регион\n" +
            "📅 Даты заезда и выезда\n" +
            "👥 Количество гостей\n" +
            "💰 Примерный бюджет\n\n" +
            "📌 *Доступные команды:*\n" +
            "/start — главное меню\n" +
            "/help — помощь\n" +
            "/cancel — отменить действие",
            TelegramClient.removeKeyboard());
    }

    public void handleDiscovery(Long chatId, String text, ConversationSession session) {
        telegramClient.sendTyping(chatId);

        List<Hotel> hotels = hotelService.getActiveBotHotels();
        if (hotels.isEmpty()) {
            telegramClient.sendMessage(chatId,
                "К сожалению, сейчас нет доступных гостиниц. Попробуйте позже.");
            return;
        }

        String reply = claudeService.discover(hotels, session.getHistory(), text);

        // Показываем только те отели, что упоминались в контексте разговора
        List<Hotel> relevant = filterByContext(hotels, session.getHistory(), text);

        List<List<Map<String, String>>> rows = relevant.stream()
            .map(h -> {
                StringBuilder label = new StringBuilder("🏨 ").append(h.getName());
                if (h.getCity() != null && !h.getCity().isBlank()) {
                    label.append(" · ").append(h.getCity());
                    if (h.getAddress() != null && !h.getAddress().isBlank())
                        label.append(", ").append(h.getAddress());
                } else if (h.getAddress() != null && !h.getAddress().isBlank()) {
                    label.append(" · ").append(h.getAddress());
                }
                return List.of(TelegramClient.btn(label.toString(), "hotel:" + h.getId()));
            })
            .collect(java.util.stream.Collectors.toList());

        tryExtractDates(chatId, text, session);

        telegramClient.sendMessage(chatId, reply, TelegramClient.inlineKeyboard(rows));
        sessionManager.addMessage(chatId, "user", text);
        sessionManager.addMessage(chatId, "assistant", reply);

        sessionManager.updateState(chatId, SessionState.SELECTING_HOTEL);
        sessionManager.addMessage(chatId, "system", "hotels:" +
            hotels.stream().map(h -> h.getId() + "=" + h.getName())
                           .reduce((a, b) -> a + "," + b).orElse(""));
    }

    // ── Выбор отеля из списка ────────────────────────────────────────────────

    public void handleHotelSelection(Long chatId, String text, ConversationSession session) {
        List<Hotel> hotels = hotelService.getActiveBotHotels();

        try {
            int idx = Integer.parseInt(text.trim()) - 1;
            if (idx < 0 || idx >= hotels.size()) throw new NumberFormatException();

            Hotel chosen = hotels.get(idx);
            sessionManager.setHotel(chatId, chosen.getId());
            sendWelcome(chosen, chatId);
        } catch (NumberFormatException e) {
            telegramClient.sendMessage(chatId,
                "Пожалуйста, введите номер гостиницы из списка (например: *1*)");
        }
    }

    // ── Чат по конкретному отелю ─────────────────────────────────────────────

    public void handle(Hotel hotel, Long chatId, String text, ConversationSession session) {
        telegramClient.sendTyping(chatId);

        String reply = claudeService.chat(hotel, session.getHistory(), text);
        telegramClient.sendMessage(chatId, reply);
        sessionManager.addMessage(chatId, "user", text);
        sessionManager.addMessage(chatId, "assistant", reply);

        conversationService.saveGuestMessage(hotel.getId(), chatId, text);
        conversationService.saveBotMessage(hotel.getId(), chatId, reply);
    }

    public void sendWelcome(Hotel hotel, Long chatId) {
        String welcome = hotel.getWelcomeMessage() != null
            ? hotel.getWelcomeMessage()
            : "✅ Вы выбрали *" + hotel.getName() + "*!\n\n" +
              "Могу помочь с:\n" +
              "• Информацией о номерах и ценах\n" +
              "• Проверкой доступности\n" +
              "• Оформлением бронирования\n\n" +
              "Задайте ваш вопрос!";

        telegramClient.sendMessage(chatId, welcome,
            TelegramClient.replyKeyboard(List.of(
                List.of("📅 Забронировать", "🛏 Номера и цены"),
                List.of("❓ Помощь", "🔄 Сменить отель")
            )));
    }

    public void sendHelp(Hotel hotel, Long chatId) {
        var keyboard = TelegramClient.inlineKeyboard(List.of(
            List.of(TelegramClient.btn("📅 Забронировать", "book")),
            List.of(TelegramClient.btn("🔄 Сменить отель", "change_hotel"))
        ));

        telegramClient.sendMessage(chatId,
            "Я могу помочь вам:\n\n" +
            "🏨 Рассказать о гостинице и номерах\n" +
            "📅 Проверить доступность на ваши даты\n" +
            "📋 Оформить бронирование\n" +
            "💳 Принять оплату",
            keyboard);
    }

    public void sendMessage(Long chatId, String text) {
        telegramClient.sendMessage(chatId, text);
    }

    /**
     * Фильтрует отели по упоминаниям города или названия в тексте и истории.
     * Если ни один не совпал — возвращает все (чтобы не оставить пользователя без вариантов).
     */
    private List<Hotel> filterByContext(List<Hotel> hotels, List<Map<String, String>> history, String text) {
        // Собираем весь контекст: текущее сообщение + предыдущие сообщения пользователя
        StringBuilder ctx = new StringBuilder(text.toLowerCase());
        for (Map<String, String> msg : history) {
            if ("user".equals(msg.get("role"))) {
                ctx.append(" ").append(msg.get("content").toLowerCase());
            }
        }
        String context = ctx.toString();

        List<Hotel> matched = hotels.stream()
            .filter(h -> {
                if (h.getCity() != null && context.contains(h.getCity().toLowerCase())) return true;
                if (h.getName() != null && context.contains(h.getName().toLowerCase())) return true;
                return false;
            })
            .collect(java.util.stream.Collectors.toList());

        return matched.isEmpty() ? hotels : matched;
    }

    private void tryExtractDates(Long chatId, String text, ConversationSession session) {
        Matcher matcher = DATE_PATTERN.matcher(text);
        List<LocalDate> dates = new ArrayList<>();
        while (matcher.find()) {
            try {
                LocalDate date = LocalDate.parse(matcher.group(), DATE_FORMAT);
                if (!date.isBefore(LocalDate.now())) {
                    dates.add(date);
                }
            } catch (DateTimeParseException ignored) {}
        }
        if (dates.isEmpty()) return;

        dates.sort(Comparator.naturalOrder());
        ConversationSession current = sessionManager.get(chatId);
        if (current == null) return;

        current.setCheckIn(dates.get(0));
        if (dates.size() >= 2) current.setCheckOut(dates.get(1));
        sessionManager.save(current);
    }
}
