package com.guestbot.repository;

import com.guestbot.core.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByHotelIdAndTelegramChatId(Long hotelId, Long telegramChatId);
}
