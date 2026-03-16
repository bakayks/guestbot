package com.guestbot.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.guestbot.core.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Booking extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String bookingNumber; // BK-20260315-001

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    // Данные гостя
    @Column(nullable = false)
    private String guestName;

    @Column(nullable = false)
    private String guestPhone;

    private String guestEmail;

    @Column(nullable = false)
    private LocalDate checkIn;

    @Column(nullable = false)
    private LocalDate checkOut;

    @Column(nullable = false)
    private Integer nights;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING_PAYMENT;

    private LocalDateTime paymentDeadline; // +24ч от создания

    private Long telegramChatId; // для уведомлений гостю

    private String source = "TELEGRAM";
}
