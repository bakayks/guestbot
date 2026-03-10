package com.guestbot.core.event;

import com.guestbot.core.enums.CancellationReason;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class DomainEvents {

    private DomainEvents() {}

    public record BookingCreatedEvent(
        Long bookingId,
        Long hotelId,
        Long roomId,
        String guestName,
        String guestPhone,
        String guestEmail,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        BigDecimal totalAmount
    ) {}

    public record PaymentSuccessEvent(
        Long bookingId,
        Long hotelId,
        String transactionId,
        BigDecimal amount,
        String cardType
    ) {}

    public record PaymentFailedEvent(
        Long bookingId,
        String reason,
        String guestPhone
    ) {}

    public record BookingCancelledEvent(
        Long bookingId,
        Long hotelId,
        String guestPhone,
        String guestName,
        CancellationReason reason
    ) {}

    public record EscalationRequestedEvent(
        Long conversationId,
        Long hotelId,
        Long telegramChatId,
        String guestQuestion,
        String guestPhone
    ) {}

    public record GuestCheckInTomorrowEvent(
        Long bookingId,
        Long hotelId,
        String guestPhone,
        String guestName,
        LocalDate checkIn
    ) {}

    public record OwnerRepliedEvent(
        Long conversationId,
        Long telegramChatId,
        String replyText
    ) {}
}
