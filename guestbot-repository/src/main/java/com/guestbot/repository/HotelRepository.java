package com.guestbot.repository;

import com.guestbot.core.entity.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HotelRepository extends JpaRepository<Hotel, Long> {
    List<Hotel> findByOwnerIdAndActiveTrue(Long ownerId);
    List<Hotel> findByBotActiveTrueAndActiveTrue();
}
