package com.guestbot.telegram.handler;

import com.guestbot.core.entity.Hotel;
import com.guestbot.telegram.session.ConversationSession;
import com.guestbot.telegram.session.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiHandler {

    private final TelegramClient telegramClient;
    private final SessionManager sessionManager;

    public void handle(Hotel hotel, Long chatId, String text, ConversationSession session) {
        // Показываем "typing..." пока Claude думает
        telegramClient.sendTyping(hotel.getTelegramBotToken(), chatId);

        // Сохраняем сообщение пользователя в историю
        sessionManager.addMessage(hotel.getId(), chatId, "user", text);

        // TODO (Неделя 3): вызов ClaudeService
        // ClaudeResponse response = claudeService.chat(hotel, session);
        // telegramClient.sendMessage(hotel.getTelegramBotToken(), chatId, response.text());

        // Stub пока Claude не подключен
        String stub = "Спасибо за ваш вопрос! Я скоро буду готов отвечать через AI. " +
                      "Пока вы можете написать напрямую администратору.";
        telegramClient.sendMessage(hotel.getTelegramBotToken(), chatId, stub);
        sessionManager.addMessage(hotel.getId(), chatId, "assistant", stub);
    }

    public void sendWelcome(Hotel hotel, Long chatId) {
        String welcome = hotel.getWelcomeMessage() != null
            ? hotel.getWelcomeMessage()
            : "👋 Добро пожаловать! Я бот гостиницы *" + hotel.getName() + "*.\n\n" +
              "Могу помочь с:\n" +
              "• Информацией о номерах и ценах\n" +
              "• Проверкой доступности\n" +
              "• Оформлением бронирования\n\n" +
              "Задайте ваш вопрос!";

        telegramClient.sendMessage(hotel.getTelegramBotToken(), chatId, welcome);
    }

    public void sendHelp(Hotel hotel, Long chatId) {
        String help = "Я могу помочь вам:\n\n" +
                      "🏨 Рассказать о гостинице и номерах\n" +
                      "📅 Проверить доступность на ваши даты\n" +
                      "📋 Оформить бронирование\n" +
                      "💳 Принять оплату\n\n" +
                      "/start - Начать сначала\n" +
                      "/cancel - Отменить текущее действие";

        telegramClient.sendMessage(hotel.getTelegramBotToken(), chatId, help);
    }

    public void sendMessage(String botToken, Long chatId, String text) {
        telegramClient.sendMessage(botToken, chatId, text);
    }
}
