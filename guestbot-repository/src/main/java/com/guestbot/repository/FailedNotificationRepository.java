package com.guestbot.repository;

import com.guestbot.core.entity.FailedNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface FailedNotificationRepository extends JpaRepository<FailedNotification, Long> {
    List<FailedNotification> findByNextRetryBeforeAndAttemptsLessThan(
        LocalDateTime now,
        Integer maxAttempts
    );
}
