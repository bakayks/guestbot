package com.guestbot.repository;

import com.guestbot.core.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByBookingId(Long bookingId);
    Optional<PaymentTransaction> findByExternalPaymentId(String externalPaymentId);
    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);
}
