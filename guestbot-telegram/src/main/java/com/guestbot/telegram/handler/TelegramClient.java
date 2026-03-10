package com.guestbot.telegram.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramClient {

    private final WebClient.Builder webClientBuilder;

    public void sendMessage(String botToken, Long chatId, String text) {
        sendMessage(botToken, chatId, text, null);
    }

    public void sendMessage(String botToken, Long chatId, String text, Object replyMarkup) {
        if (botToken == null) {
            log.warn("Bot token is null, cannot send message");
            return;
        }

        Map<String, Object> body = replyMarkup != null
            ? Map.of("chat_id", chatId, "text", text,
                     "parse_mode", "Markdown", "reply_markup", replyMarkup)
            : Map.of("chat_id", chatId, "text", text, "parse_mode", "Markdown");

        webClientBuilder.build()
            .post()
            .uri("https://api.telegram.org/bot" + botToken + "/sendMessage")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(String.class)
            .subscribe(
                r -> log.debug("Message sent to chatId={}", chatId),
                e -> log.error("Failed to send message to chatId={}: {}", chatId, e.getMessage())
            );
    }

    public void sendTyping(String botToken, Long chatId) {
        webClientBuilder.build()
            .post()
            .uri("https://api.telegram.org/bot" + botToken + "/sendChatAction")
            .bodyValue(Map.of("chat_id", chatId, "action", "typing"))
            .retrieve()
            .bodyToMono(String.class)
            .subscribe();
    }
}
