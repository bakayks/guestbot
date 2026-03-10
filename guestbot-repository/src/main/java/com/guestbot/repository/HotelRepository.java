package com.guestbot.repository;

import com.guestbot.core.entity.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HotelRepository extends JpaRepository<Hotel, Long> {
    List<Hotel> findByOwnerIdAndActiveTrue(Long ownerId);
    Optional<Hotel> findByTelegramBotToken(String token);
}
