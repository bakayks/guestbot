package com.guestbot.core.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "failed_notifications")
@Getter @Setter @NoArgsConstructor
public class FailedNotification extends BaseEntity {

    @Column(nullable = false)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(columnDefinition = "TEXT")
    private String error;

    @Column(nullable = false)
    private Integer attempts = 0;

    @Column(nullable = false)
    private LocalDateTime nextRetry = LocalDateTime.now();

    public void incrementAttempts() {
        this.attempts++;
        // Exponential backoff: 5min, 10min, 20min, 40min, 80min
        this.nextRetry = LocalDateTime.now().plusMinutes(5L * attempts);
    }
}
