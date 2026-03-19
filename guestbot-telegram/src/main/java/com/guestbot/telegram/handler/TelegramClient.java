package com.guestbot.telegram.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TelegramClient {

    private final WebClient webClient;

    @Value("${telegram.bot-token}")
    private String botToken;

    public TelegramClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    // ── Send message ─────────────────────────────────────────────────────────

    public void sendMessage(Long chatId, String text) {
        sendMessage(chatId, text, null);
    }

    public void sendMessage(Long chatId, String text, Object replyMarkup) {
        if (botToken == null || botToken.isBlank()) {
            log.warn("Bot token is not configured, cannot send message");
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);
        body.put("parse_mode", "Markdown");
        if (replyMarkup != null) body.put("reply_markup", replyMarkup);

        webClient.post()
            .uri("https://api.telegram.org/bot" + botToken + "/sendMessage")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(String.class)
            .subscribe(
                r -> log.debug("Message sent to chatId={}", chatId),
                e -> log.error("Failed to send message to chatId={}: {}", chatId, e.getMessage())
            );
    }

    public void sendTyping(Long chatId) {
        if (botToken == null || botToken.isBlank()) return;

        webClient.post()
            .uri("https://api.telegram.org/bot" + botToken + "/sendChatAction")
            .bodyValue(Map.of("chat_id", chatId, "action", "typing"))
            .retrieve()
            .bodyToMono(String.class)
            .subscribe();
    }

    public void answerCallbackQuery(String callbackQueryId) {
        if (botToken == null || botToken.isBlank()) return;

        webClient.post()
            .uri("https://api.telegram.org/bot" + botToken + "/answerCallbackQuery")
            .bodyValue(Map.of("callback_query_id", callbackQueryId))
            .retrieve()
            .bodyToMono(String.class)
            .subscribe(r -> {}, e -> log.warn("Failed to answer callback query: {}", e.getMessage()));
    }

    /** Регистрирует команды бота — они появляются в меню "/" у пользователя. */
    public void setMyCommands(List<Map<String, String>> commands) {
        if (botToken == null || botToken.isBlank()) return;

        webClient.post()
            .uri("https://api.telegram.org/bot" + botToken + "/setMyCommands")
            .bodyValue(Map.of("commands", commands))
            .retrieve()
            .bodyToMono(String.class)
            .subscribe(
                r -> log.info("Bot commands registered: {}", commands.stream()
                    .map(c -> "/" + c.get("command")).toList()),
                e -> log.error("Failed to register bot commands: {}", e.getMessage())
            );
    }

    // ── Keyboard builders ─────────────────────────────────────────────────────

    /** Inline-кнопки под сообщением. rows — список строк, каждая строка — список кнопок. */
    public static Map<String, Object> inlineKeyboard(List<List<Map<String, String>>> rows) {
        return Map.of("inline_keyboard", rows);
    }

    /** Одна inline-кнопка с callback_data. */
    public static Map<String, String> btn(String text, String callbackData) {
        return Map.of("text", text, "callback_data", callbackData);
    }

    /** Reply-кнопки внизу экрана (обычная клавиатура). */
    public static Map<String, Object> replyKeyboard(List<List<String>> rows) {
        List<List<Map<String, String>>> keyboard = rows.stream()
            .map(row -> row.stream()
                .map(text -> Map.of("text", text))
                .collect(Collectors.toList()))
            .collect(Collectors.toList());
        return Map.of("keyboard", keyboard, "resize_keyboard", true, "one_time_keyboard", false);
    }

    /** Убирает reply-клавиатуру. */
    public static Map<String, Object> removeKeyboard() {
        return Map.of("remove_keyboard", true);
    }
}
