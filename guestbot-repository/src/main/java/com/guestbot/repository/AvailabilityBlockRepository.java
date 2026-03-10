package com.guestbot.repository;

import com.guestbot.core.entity.AvailabilityBlock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AvailabilityBlockRepository extends JpaRepository<AvailabilityBlock, Long> {

    @Query("""
        SELECT COUNT(b) FROM AvailabilityBlock b
        WHERE b.room.id = :roomId
        AND b.dateFrom < :dateTo
        AND b.dateTo > :dateFrom
    """)
    long countBlocksForRoom(
        @Param("roomId") Long roomId,
        @Param("dateFrom") LocalDate dateFrom,
        @Param("dateTo") LocalDate dateTo
    );

    List<AvailabilityBlock> findByHotelIdAndDateFromBetween(
        Long hotelId, LocalDate from, LocalDate to
    );
}
