package ru.danon.spring.ToDo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "scheduled_notifications")
@Data
public class ScheduledNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "task_id", nullable = false)
    private Integer taskId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "scheduled_time", nullable = false)
    private LocalDateTime scheduledTime;

    @Column(name = "notification_time")
    private LocalDateTime notificationTime;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // PENDING, SENT, CANCELLED, FAILED

    @Column(name = "attempt_count")
    private Integer attemptCount = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (attemptCount == null) attemptCount = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}