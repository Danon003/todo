package ru.danon.spring.ToDo.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.danon.spring.ToDo.models.postgre.ScheduledNotification;
import ru.danon.spring.ToDo.repositories.jpa.ScheduledNotificationRepository;
import ru.danon.spring.ToDo.repositories.jpa.TaskAssignmentRepository;
import ru.danon.spring.ToDo.repositories.jpa.TaskRepository;
import ru.danon.spring.ToDo.services.NotificationProcessingService;
import ru.danon.spring.ToDo.services.NotificationProducerService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProcessingServiceImpl implements NotificationProcessingService {

    private final ScheduledNotificationRepository scheduledNotificationRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final NotificationProducerService notificationProducerServiceImpl;
    private final TaskRepository taskRepository;

    private static final int MAX_ATTEMPTS = 3;
    private static final int PROCESSING_WINDOW_MINUTES = 10;
    private static final int FUTURE_BUFFER_MINUTES = 2;

    @Transactional
    @Override
    public void processScheduledNotifications() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minusMinutes(PROCESSING_WINDOW_MINUTES);
        LocalDateTime windowEnd = now.plusMinutes(FUTURE_BUFFER_MINUTES);

        List<ScheduledNotification> pendingNotifications =
                scheduledNotificationRepository.findByStatusAndScheduledTimeBetween(
                        "PENDING", windowStart, windowEnd);

        log.info("Найдено {} ожидающих уведомлений для обработки", pendingNotifications.size());

        for (ScheduledNotification notification : pendingNotifications) {
            processSingleNotification(notification, now);
        }

        // Обрабатываем failed уведомления для повторных попыток
        processFailedNotifications();
    }

    private void processSingleNotification(ScheduledNotification notification, LocalDateTime now) {
        try {
            notification.setAttemptCount(notification.getAttemptCount() + 1);
            log.debug("Обработка уведомления id={}, попытка {}/{}",
                    notification.getId(), notification.getAttemptCount(), MAX_ATTEMPTS);

            // Проверяем, что задача еще актуальна
            if (!isTaskValidForNotification(notification.getTaskId(), notification.getUserId())) {
                notification.setStatus("CANCELLED");
                scheduledNotificationRepository.save(notification);
                log.info("Уведомление отменено - задача не актуальна: id={}, taskId={}, userId={}",
                        notification.getId(), notification.getTaskId(), notification.getUserId());
                return;
            }

            // Отправляем уведомление
            sendActualNotification(notification);

            // Помечаем как отправленное
            notification.setStatus("SENT");
            notification.setNotificationTime(now);
            scheduledNotificationRepository.save(notification);

            log.info("Уведомление успешно отправлено: id={}, taskId={}, userId={}, type={}",
                    notification.getId(), notification.getTaskId(), notification.getUserId(), notification.getEventType());

        } catch (Exception e) {
            handleNotificationError(notification, e);
        }
    }

    private void processFailedNotifications() {
        List<ScheduledNotification> failedNotifications =
                scheduledNotificationRepository.findByStatusAndAttemptCountLessThan("FAILED", MAX_ATTEMPTS);

        if (!failedNotifications.isEmpty()) {
            log.info("Найдено {} неудачных уведомлений для повторной обработки", failedNotifications.size());

            for (ScheduledNotification notification : failedNotifications) {
                // Повторяем обработку для уведомлений, которые еще не превысили лимит попыток
                if (notification.getScheduledTime().isAfter(LocalDateTime.now().minusHours(24))) {
                    notification.setStatus("PENDING");
                    scheduledNotificationRepository.save(notification);
                    log.debug("Уведомление id={} возвращено в статус PENDING для повторной обработки", notification.getId());
                }
            }
        }
    }

    private void handleNotificationError(ScheduledNotification notification, Exception e) {
        if (notification.getAttemptCount() >= MAX_ATTEMPTS) {
            notification.setStatus("FAILED");
            log.error("Уведомление не удалось отправить после {} попыток: id={}, taskId={}, userId={}",
                    notification.getAttemptCount(), notification.getId(), notification.getTaskId(), notification.getUserId(), e);
        } else {
            notification.setStatus("FAILED"); // Временно FAILED, будет повторно обработано
            log.warn("Ошибка обработки уведомления (попытка {}): id={}, ошибка: {}",
                    notification.getAttemptCount(), notification.getId(), e.getMessage());
        }
        scheduledNotificationRepository.save(notification);
    }

    private boolean isTaskValidForNotification(Long taskId, Long userId) {
        // Проверяем, что задача существует, не завершена и не просрочена
        boolean isValid = taskAssignmentRepository.existsValidTaskForNotification(taskId, userId, LocalDateTime.now());
        log.debug("Проверка актуальности задачи для уведомления: taskId={}, userId={}, isValid={}", taskId, userId, isValid);
        return isValid;
    }

    private void sendActualNotification(ScheduledNotification notification) {
        String label = getLabelByEventType(notification.getEventType());

        log.debug("Отправка уведомления о приближении дедлайна: userId={}, taskId={}, label={}",
                notification.getUserId(), notification.getTaskId(), label);

        notificationProducerServiceImpl.sendTaskDeadlineApproachingNotification(
                notification.getUserId(),
                "ROLE_STUDENT",
                getTaskTitle(notification.getTaskId()),
                notification.getTaskId(),
                label,
                notification.getEventType()
        );
    }

    private String getLabelByEventType(String eventType) {
        return switch (eventType) {
            case "TASK_DEADLINE_2D" -> "через 2 дня";
            case "TASK_DEADLINE_1D" -> "через 1 день";
            case "TASK_DEADLINE_12H" -> "через 12 часов";
            default -> "скоро";
        };
    }

    private String getTaskTitle(Long taskId) {
        return taskRepository.findById(taskId)
                .map(task -> task.getTitle())
                .orElse("Неизвестная задача");
    }
}
