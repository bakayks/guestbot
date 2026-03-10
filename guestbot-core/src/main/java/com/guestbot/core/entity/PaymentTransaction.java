package com.guestbot.core.entity;

import com.guestbot.core.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "payment_transactions")
@Getter @Setter @NoArgsConstructor
public class PaymentTransaction extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false, unique = true)
    private String externalPaymentId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal commissionAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal hotelAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    private String cardType;
    private String paymentUrl;
    private String idempotencyKey;
}
