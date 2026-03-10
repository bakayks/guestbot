package com.guestbot.telegram.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionManager {

    private static final Duration SESSION_TTL = Duration.ofHours(2);
    private static final int MAX_HISTORY_SIZE = 20;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private String key(Long hotelId, Long chatId) {
        return "session:" + hotelId + ":" + chatId;
    }

    public ConversationSession getOrCreate(Long hotelId, Long chatId) {
        String key = key(hotelId, chatId);
        String json = redisTemplate.opsForValue().get(key);

        if (json == null) {
            ConversationSession session = new ConversationSession(hotelId, chatId);
            save(session);
            return session;
        }

        try {
            return objectMapper.readValue(json, ConversationSession.class);
        } catch (Exception e) {
            log.warn("Failed to deserialize session for {}:{}, creating new one", hotelId, chatId);
            ConversationSession session = new ConversationSession(hotelId, chatId);
            save(session);
            return session;
        }
    }

    public void addMessage(Long hotelId, Long chatId, String role, String content) {
        ConversationSession session = getOrCreate(hotelId, chatId);

        session.getHistory().add(Map.of("role", role, "content", content));

        // Ограничиваем историю — Claude контекстное окно не бесконечное
        if (session.getHistory().size() > MAX_HISTORY_SIZE) {
            // Удаляем самые старые, но сохраняем первое сообщение (контекст бронирования)
            List<Map<String, String>> history = session.getHistory();
            while (history.size() > MAX_HISTORY_SIZE) {
                history.remove(1); // удаляем второе (первое оставляем)
            }
        }

        save(session);
    }

    public void updateState(Long hotelId, Long chatId, SessionState state) {
        ConversationSession session = getOrCreate(hotelId, chatId);
        session.setState(state);
        save(session);
    }

    public void clearSession(Long hotelId, Long chatId) {
        redisTemplate.delete(key(hotelId, chatId));
    }

    private void save(ConversationSession session) {
        try {
            String json = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(key(session.getHotelId(), session.getChatId()),
                json, SESSION_TTL);
        } catch (Exception e) {
            log.error("Failed to save session", e);
        }
    }
}
