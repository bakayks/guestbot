package com.guestbot.repository;

import com.guestbot.core.entity.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByHotelIdAndActiveTrue(Long hotelId);

    @Query("SELECT r FROM Room r LEFT JOIN FETCH r.photos WHERE r.hotel.id = :hotelId AND r.active = true")
    List<Room> findByHotelIdAndActiveTrueWithPhotos(@Param("hotelId") Long hotelId);

    // Pessimistic write lock — используется в CalendarService при создании брони
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.id = :id")
    Optional<Room> findByIdWithLock(@Param("id") Long id);
}
