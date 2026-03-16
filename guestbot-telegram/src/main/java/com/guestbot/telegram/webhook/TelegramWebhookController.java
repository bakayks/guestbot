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

    @PostMapping
    public ResponseEntity<Void> handleUpdate(
        @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String secretToken,
        @RequestBody JsonNode update
    ) {
        if (webhookSecret != null && !webhookSecret.equals(secretToken)) {
            log.warn("Invalid webhook secret token");
            return ResponseEntity.ok().build(); // 200 всегда, чтобы Telegram не ретраил
        }

        log.info("Telegram update received: {}", update.has("update_id") ? update.get("update_id").asLong() : "unknown");

        try {
            updateDispatcher.dispatch(update);
        } catch (Exception e) {
            log.error("Error processing Telegram update", e);
        }

        return ResponseEntity.ok().build();
    }
}
