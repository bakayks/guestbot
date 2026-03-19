package com.guestbot.telegram.webhook;

import com.guestbot.telegram.handler.TelegramClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BotInitializer {

    private final TelegramClient telegramClient;

    @EventListener(ApplicationReadyEvent.class)
    public void registerCommands() {
        telegramClient.setMyCommands(List.of(
            Map.of("command", "start",  "description", "Начать / Главное меню"),
            Map.of("command", "help",   "description", "Помощь и список возможностей"),
            Map.of("command", "cancel", "description", "Отменить текущее действие")
        ));
    }
}
