package ru.danon.spring.ToDo.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.danon.spring.ToDo.models.ScheduledNotification;
import ru.danon.spring.ToDo.repositories.jpa.ScheduledNotificationRepository;
import ru.danon.spring.ToDo.repositories.jpa.TaskAssignmentRepository;
import ru.danon.spring.ToDo.repositories.jpa.TaskRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProcessingService {

    private final ScheduledNotificationRepository scheduledNotificationRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final NotificationProducerService notificationProducerService;
    private final TaskRepository taskRepository;

    private static final int MAX_ATTEMPTS = 3;
    private static final int PROCESSING_WINDOW_MINUTES = 10;
    private static final int FUTURE_BUFFER_MINUTES = 2;

    @Scheduled(fixedRate = 300000) // Каждые 5 минут
    @Transactional
    public void processScheduledNotifications() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minusMinutes(PROCESSING_WINDOW_MINUTES);
        LocalDateTime windowEnd = now.plusMinutes(FUTURE_BUFFER_MINUTES);

        List<ScheduledNotification> pendingNotifications =
                scheduledNotificationRepository.findByStatusAndScheduledTimeBetween(
                        "PENDING", windowStart, windowEnd);

        log.info("Found {} pending notifications to process", pendingNotifications.size());

        for (ScheduledNotification notification : pendingNotifications) {
            processSingleNotification(notification, now);
        }

        // Обрабатываем failed уведомления для повторных попыток
        processFailedNotifications();
    }

    private void processSingleNotification(ScheduledNotification notification, LocalDateTime now) {
        try {
            notification.setAttemptCount(notification.getAttemptCount() + 1);

            // Проверяем, что задача еще актуальна
            if (!isTaskValidForNotification(notification.getTaskId(), notification.getUserId())) {
                notification.setStatus("CANCELLED");
                scheduledNotificationRepository.save(notification);
                log.info("Notification cancelled - task not valid: {}", notification.getId());
                return;
            }

            // Отправляем уведомление
            sendActualNotification(notification);

            // Помечаем как отправленное
            notification.setStatus("SENT");
            notification.setNotificationTime(now);
            scheduledNotificationRepository.save(notification);

            log.info("Notification sent successfully: {}", notification.getId());

        } catch (Exception e) {
            handleNotificationError(notification, e);
        }
    }

    private void processFailedNotifications() {
        List<ScheduledNotification> failedNotifications =
                scheduledNotificationRepository.findByStatusAndAttemptCountLessThan("FAILED", MAX_ATTEMPTS);

        for (ScheduledNotification notification : failedNotifications) {
            // Повторяем обработку для уведомлений, которые еще не превысили лимит попыток
            if (notification.getScheduledTime().isAfter(LocalDateTime.now().minusHours(24))) {
                notification.setStatus("PENDING");
                scheduledNotificationRepository.save(notification);
            }
        }
    }

    private void handleNotificationError(ScheduledNotification notification, Exception e) {
        if (notification.getAttemptCount() >= MAX_ATTEMPTS) {
            notification.setStatus("FAILED");
            log.error("Notification failed after {} attempts: {}",
                    notification.getAttemptCount(), notification.getId(), e);
        } else {
            notification.setStatus("FAILED"); // Временно FAILED, будет повторно обработано
            log.warn("Notification processing failed (attempt {}): {}",
                    notification.getAttemptCount(), notification.getId(), e.getMessage());
        }
        scheduledNotificationRepository.save(notification);
    }

    private boolean isTaskValidForNotification(Integer taskId, Integer userId) {
        // Проверяем, что задача существует, не завершена и не просрочена
        return taskAssignmentRepository.existsValidTaskForNotification(taskId, userId, LocalDateTime.now());
    }

    private void sendActualNotification(ScheduledNotification notification) {
        String label = getLabelByEventType(notification.getEventType());

        notificationProducerService.sendTaskDeadlineApproachingNotification(
                notification.getUserId(),
                "ROLE_STUDENT",
                getTaskTitle(notification.getTaskId()),
                notification.getTaskId(),
                label,
                notification.getEventType()
        );
    }

    private String getLabelByEventType(String eventType) {
        switch (eventType) {
            case "TASK_DEADLINE_2D": return "через 2 дня";
            case "TASK_DEADLINE_1D": return "через 1 день";
            case "TASK_DEADLINE_12H": return "через 12 часов";
            default: return "скоро";
        }
    }

    private String getTaskTitle(Integer taskId) {
        return taskRepository.findById(taskId).get().getTitle();
    }
}