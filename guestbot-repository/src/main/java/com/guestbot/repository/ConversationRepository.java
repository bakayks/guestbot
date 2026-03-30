package com.guestbot.repository;

import com.guestbot.core.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByHotelIdAndTelegramChatId(Long hotelId, Long telegramChatId);

    // Последний диалог гостя за указанный период (для предложения "продолжить")
    @Query("""
        SELECT c FROM Conversation c
        WHERE c.telegramChatId = :chatId
        AND c.lastMessageAt >= :since
        ORDER BY c.lastMessageAt DESC
        LIMIT 1
    """)
    Optional<Conversation> findRecentByTelegramChatId(
        @Param("chatId") Long chatId,
        @Param("since") LocalDateTime since
    );
}
