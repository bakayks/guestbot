package com.guestbot.repository;

import com.guestbot.core.entity.Booking;
import com.guestbot.core.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long>,
        JpaSpecificationExecutor<Booking> {

    Optional<Booking> findByBookingNumber(String bookingNumber);

    // Количество активных броней на номер в диапазоне дат
    // Используется для проверки доступности
    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.room.id = :roomId
        AND b.status IN ('PENDING_PAYMENT', 'CONFIRMED', 'CHECKED_IN')
        AND b.checkIn < :checkOut
        AND b.checkOut > :checkIn
    """)
    long countActiveBookingsForRoom(
        @Param("roomId") Long roomId,
        @Param("checkIn") LocalDate checkIn,
        @Param("checkOut") LocalDate checkOut
    );

    // Для автоотмены просроченных броней
    List<Booking> findByStatusAndPaymentDeadlineBefore(
        BookingStatus status,
        LocalDateTime deadline
    );

    // Для напоминаний о заезде завтра
    @Query("""
        SELECT b FROM Booking b
        WHERE b.status = 'CONFIRMED'
        AND b.checkIn = :tomorrow
    """)
    List<Booking> findConfirmedCheckInsForDate(@Param("tomorrow") LocalDate tomorrow);
}
