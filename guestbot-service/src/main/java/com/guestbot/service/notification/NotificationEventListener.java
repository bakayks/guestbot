package com.guestbot.service.notification;

import com.guestbot.core.entity.FailedNotification;
import com.guestbot.core.event.DomainEvents.*;
import com.guestbot.repository.FailedNotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventListener {

    private final TelegramNotificationService telegramNotificationService;
    private final FailedNotificationRepository failedNotificationRepository;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void onBookingCreated(BookingCreatedEvent event) {
        MDC.put("bookingId", event.bookingId().toString());
        try {
            // Уведомляем владельца гостиницы
            telegramNotificationService.notifyOwnerNewBooking(event);
        } catch (Exception e) {
            log.error("Failed to notify owner for booking {}", event.bookingId(), e);
            saveFailedNotification("BookingCreatedEvent", event);
        } finally {
            MDC.clear();
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void onPaymentSuccess(PaymentSuccessEvent event) {
        MDC.put("bookingId", event.bookingId().toString());
        try {
            telegramNotificationService.notifyOwnerPaymentReceived(event);
            telegramNotificationService.notifyGuestBookingConfirmed(event);
        } catch (Exception e) {
            log.error("Failed to send payment success notifications for booking {}",
                event.bookingId(), e);
            saveFailedNotification("PaymentSuccessEvent", event);
        } finally {
            MDC.clear();
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void onBookingCancelled(BookingCancelledEvent event) {
        MDC.put("bookingId", event.bookingId().toString());
        try {
            telegramNotificationService.notifyOwnerBookingCancelled(event);
            telegramNotificationService.notifyGuestBookingCancelled(event);
        } catch (Exception e) {
            log.error("Failed to send cancellation notifications for booking {}",
                event.bookingId(), e);
            saveFailedNotification("BookingCancelledEvent", event);
        } finally {
            MDC.clear();
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void onGuestCheckInTomorrow(GuestCheckInTomorrowEvent event) {
        MDC.put("bookingId", event.bookingId().toString());
        try {
            telegramNotificationService.notifyGuestCheckInReminder(event);
        } catch (Exception e) {
            log.error("Failed to send check-in reminder for booking {}", event.bookingId(), e);
            saveFailedNotification("GuestCheckInTomorrowEvent", event);
        } finally {
            MDC.clear();
        }
    }

    // Retry каждые 5 минут
    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void retryFailedNotifications() {
        List<FailedNotification> failed = failedNotificationRepository
            .findByNextRetryBeforeAndAttemptsLessThan(LocalDateTime.now(), 5);

        if (!failed.isEmpty()) {
            log.info("Retrying {} failed notifications", failed.size());
        }

        failed.forEach(n -> {
            try {
                // Десериализуем и переотправляем
                reprocess(n);
                failedNotificationRepository.delete(n);
            } catch (Exception e) {
                log.warn("Retry failed for notification {}, attempts: {}", n.getId(), n.getAttempts());
                n.incrementAttempts();
                failedNotificationRepository.save(n);
            }
        });
    }

    private void reprocess(FailedNotification notification) throws Exception {
        // Простая переотправка в зависимости от типа
        switch (notification.getEventType()) {
            case "BookingCreatedEvent" -> {
                BookingCreatedEvent event = objectMapper.readValue(
                    notification.getPayload(), BookingCreatedEvent.class
                );
                telegramNotificationService.notifyOwnerNewBooking(event);
            }
            default -> log.warn("Unknown event type for retry: {}", notification.getEventType());
        }
    }

    private void saveFailedNotification(String eventType, Object event) {
        try {
            FailedNotification failed = new FailedNotification();
            failed.setEventType(eventType);
            failed.setPayload(objectMapper.writeValueAsString(event));
            failedNotificationRepository.save(failed);
        } catch (Exception e) {
            log.error("Failed to save failed notification to DB!", e);
        }
    }
}
