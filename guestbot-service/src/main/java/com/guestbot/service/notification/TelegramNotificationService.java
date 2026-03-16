package com.guestbot.service.notification;

import com.guestbot.core.entity.Hotel;
import com.guestbot.core.event.DomainEvents.*;
import com.guestbot.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramNotificationService {

    private final WebClient.Builder webClientBuilder;
    private final HotelRepository hotelRepository;

    @Value("${telegram.bot-token}")
    private String botToken;

    public void notifyOwnerNewBooking(BookingCreatedEvent event) {
        Hotel hotel = hotelRepository.findById(event.hotelId()).orElse(null);
        if (hotel == null || hotel.getOwner().getTelegramChatId() == null) return;

        String text = String.format("""
            🆕 *Новое бронирование!*
            
            📋 Гость: %s (%s)
            🏨 Заезд: %s → %s (%d ночей)
            💰 Сумма: %.0f сом
            ⏰ Оплата до: 24 часа
            """,
            event.guestName(), event.guestPhone(),
            event.checkIn(), event.checkOut(), event.nights(),
            event.totalAmount()
        );

        sendMessage(botToken,
            hotel.getOwner().getTelegramChatId(), text);
    }

    public void notifyOwnerPaymentReceived(PaymentSuccessEvent event) {
        Hotel hotel = hotelRepository.findById(event.hotelId()).orElse(null);
        if (hotel == null || hotel.getOwner().getTelegramChatId() == null) return;

        String text = String.format("""
            ✅ *Оплата получена!*
            
            💳 Карта: %s
            💰 Сумма: %.0f сом
            🏦 Вам будет перечислено: %.0f сом
            """,
            event.cardType(),
            event.amount(),
            event.amount().multiply(java.math.BigDecimal.valueOf(0.90))
        );

        sendMessage(botToken,
            hotel.getOwner().getTelegramChatId(), text);
    }

    public void notifyGuestBookingConfirmed(PaymentSuccessEvent event) {
        // Гостю отправляем через chatId из booking
        // TODO: достать telegramChatId из booking
        log.info("Guest booking confirmed notification sent for booking {}", event.bookingId());
    }

    public void notifyOwnerBookingCancelled(BookingCancelledEvent event) {
        Hotel hotel = hotelRepository.findById(event.hotelId()).orElse(null);
        if (hotel == null || hotel.getOwner().getTelegramChatId() == null) return;

        String text = String.format("""
            ❌ *Бронирование отменено*
            
            👤 Гость: %s
            📌 Причина: %s
            """,
            event.guestName(), event.reason()
        );

        sendMessage(botToken,
            hotel.getOwner().getTelegramChatId(), text);
    }

    public void notifyGuestBookingCancelled(BookingCancelledEvent event) {
        log.info("Guest cancellation notification for booking {}", event.bookingId());
    }

    public void notifyGuestCheckInReminder(GuestCheckInTomorrowEvent event) {
        log.info("Check-in reminder for booking {}", event.bookingId());
    }

    private void sendMessage(String botToken, String chatId, String text) {
        if (botToken == null || chatId == null) return;

        try {
            webClientBuilder.build()
                .post()
                .uri("https://api.telegram.org/bot" + botToken + "/sendMessage")
                .bodyValue(Map.of(
                    "chat_id", chatId,
                    "text", text,
                    "parse_mode", "Markdown"
                ))
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(
                    response -> log.debug("Telegram message sent to {}", chatId),
                    error -> log.error("Failed to send Telegram message: {}", error.getMessage())
                );
        } catch (Exception e) {
            log.error("Error sending Telegram notification", e);
            throw e;
        }
    }
}
