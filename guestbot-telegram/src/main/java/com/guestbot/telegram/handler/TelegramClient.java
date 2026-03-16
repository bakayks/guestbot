package com.guestbot.telegram.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Component
public class TelegramClient {

    private final WebClient webClient;

    @Value("${telegram.bot-token}")
    private String botToken;

    public TelegramClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public void sendMessage(Long chatId, String text) {
        sendMessage(chatId, text, null);
    }

    public void sendMessage(Long chatId, String text, Object replyMarkup) {
        if (botToken == null || botToken.isBlank()) {
            log.warn("Bot token is not configured, cannot send message");
            return;
        }

        Map<String, Object> body = replyMarkup != null
            ? Map.of("chat_id", chatId, "text", text,
                     "parse_mode", "Markdown", "reply_markup", replyMarkup)
            : Map.of("chat_id", chatId, "text", text, "parse_mode", "Markdown");

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
}
