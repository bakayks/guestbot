package com.guestbot.telegram.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
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

    private String key(Long chatId) {
        return "session:" + chatId;
    }

    /** Загружает сессию, возвращает null если не существует. */
    public ConversationSession get(Long chatId) {
        String json = redisTemplate.opsForValue().get(key(chatId));
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, ConversationSession.class);
        } catch (Exception e) {
            log.warn("Failed to deserialize session for chatId={}", chatId);
            return null;
        }
    }

    /** Загружает сессию или создаёт новую. hotelId может быть null (маркетплейс-флоу). */
    public ConversationSession getOrCreate(Long chatId) {
        ConversationSession session = get(chatId);
        if (session != null) return session;

        session = new ConversationSession(null, chatId);
        save(session);
        return session;
    }

    /** Привязывает сессию к конкретному отелю после выбора гостем. */
    public void setHotel(Long chatId, Long hotelId) {
        ConversationSession session = get(chatId);
        if (session == null) return;
        session.setHotelId(hotelId);
        session.setState(SessionState.IDLE);
        save(session);
    }

    public void addMessage(Long chatId, String role, String content) {
        ConversationSession session = get(chatId);
        if (session == null) return;

        session.getHistory().add(Map.of("role", role, "content", content));

        if (session.getHistory().size() > MAX_HISTORY_SIZE) {
            List<Map<String, String>> history = session.getHistory();
            while (history.size() > MAX_HISTORY_SIZE) {
                history.remove(1); // сохраняем первое сообщение (системный контекст)
            }
        }

        save(session);
    }

    public void updateState(Long chatId, SessionState state) {
        ConversationSession session = get(chatId);
        if (session == null) return;
        session.setState(state);
        save(session);
    }

    public void clearSession(Long chatId) {
        redisTemplate.delete(key(chatId));
    }

    public void save(ConversationSession session) {
        try {
            String json = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(key(session.getChatId()), json, SESSION_TTL);
        } catch (Exception e) {
            log.error("Failed to save session for chatId={}", session.getChatId(), e);
        }
    }
}
