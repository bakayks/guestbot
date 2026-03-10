package com.guestbot.telegram.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.guestbot.telegram.dispatcher.UpdateDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhook")
@RequiredArgsConstructor
public class TelegramWebhookController {

    private final UpdateDispatcher updateDispatcher;

    @Value("${telegram.webhook-secret}")
    private String webhookSecret;

    @PostMapping("/{botToken}")
    public ResponseEntity<Void> handleUpdate(
        @PathVariable String botToken,
        @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String secretToken,
        @RequestBody JsonNode update
    ) {
        // Проверяем secret token от Telegram
        if (webhookSecret != null && !webhookSecret.equals(secretToken)) {
            log.warn("Invalid webhook secret token for bot {}", botToken);
            return ResponseEntity.ok().build(); // 200 всегда, чтобы Telegram не ретраил
        }

        try {
            updateDispatcher.dispatch(botToken, update);
        } catch (Exception e) {
            // Логируем но возвращаем 200 — иначе Telegram будет ретраить
            log.error("Error processing Telegram update", e);
        }

        return ResponseEntity.ok().build();
    }
}
