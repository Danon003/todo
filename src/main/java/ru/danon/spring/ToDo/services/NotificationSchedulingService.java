package ru.danon.spring.ToDo.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.danon.spring.ToDo.models.ScheduledNotification;
import ru.danon.spring.ToDo.models.TaskAssignment;
import ru.danon.spring.ToDo.repositories.jpa.ScheduledNotificationRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSchedulingService {

    private final ScheduledNotificationRepository scheduledNotificationRepository;

    // Конфигурируемые интервалы уведомлений (часы до дедлайна)
    private final List<Integer> NOTIFICATION_INTERVALS = Arrays.asList(48, 24, 12);

    @Transactional
    public void scheduleTaskNotifications(TaskAssignment assignment) {
        LocalDateTime deadline = assignment.getTask().getDeadline();
        if (deadline == null) {
            log.warn("Task {} has no deadline, skipping notification scheduling",
                    assignment.getTask().getId());
            return;
        }

        log.info("Scheduling notifications for task {}, user {}, deadline {}",
                assignment.getTask().getId(), assignment.getUserId(), deadline);

        // Планируем уведомления для каждого интервала
        for (Integer hours : NOTIFICATION_INTERVALS) {
            scheduleNotification(assignment, deadline.minusHours(hours),
                    getEventType(hours), getLabel(hours));
        }
    }

    @Transactional
    public void rescheduleTaskNotifications(TaskAssignment assignment) {
        // Сначала отменяем старые уведомления
        cancelTaskNotifications(assignment.getTask().getId(), assignment.getUserId());

        // Затем создаем новые
        scheduleTaskNotifications(assignment);
    }

    @Transactional
    public void cancelTaskNotifications(Integer taskId, Integer userId) {
        List<ScheduledNotification> pendingNotifications =
                scheduledNotificationRepository.findByTaskIdAndUserIdAndStatus(taskId, userId, "PENDING");

        if (!pendingNotifications.isEmpty()) {
            for (ScheduledNotification notification : pendingNotifications) {
                notification.setStatus("CANCELLED");
            }
            scheduledNotificationRepository.saveAll(pendingNotifications);
            log.info("Cancelled {} pending notifications for task {}, user {}",
                    pendingNotifications.size(), taskId, userId);
        }
    }

    @Transactional
    public void cancelAllTaskNotifications(Integer taskId) {
        List<ScheduledNotification> pendingNotifications =
                scheduledNotificationRepository.findByTaskId(taskId);

        List<ScheduledNotification> toCancel = pendingNotifications.stream()
                .filter(notification -> "PENDING".equals(notification.getStatus()))
                .toList();

        if (!toCancel.isEmpty()) {
            for (ScheduledNotification notification : toCancel) {
                notification.setStatus("CANCELLED");
            }
            scheduledNotificationRepository.saveAll(toCancel);
            log.info("Cancelled {} pending notifications for task {}",
                    toCancel.size(), taskId);
        }
    }

    private void scheduleNotification(TaskAssignment assignment,
                                      LocalDateTime scheduledTime,
                                      String eventType, String label) {

        // Проверяем, не было ли уже запланировано такое уведомление
        if (scheduledNotificationRepository.existsByTaskIdAndUserIdAndEventTypeAndStatus(
                assignment.getTask().getId(), assignment.getUserId(), eventType, "PENDING")) {
            log.debug("Notification already scheduled: task {}, user {}, type {}",
                    assignment.getTask().getId(), assignment.getUserId(), eventType);
            return;
        }

        // Проверяем, что время уведомления еще не прошло
        if (scheduledTime.isBefore(LocalDateTime.now())) {
            log.debug("Scheduled time already passed for task {}, type {}",
                    assignment.getTask().getId(), eventType);
            return;
        }

        ScheduledNotification notification = new ScheduledNotification();
        notification.setUserId(assignment.getUserId());
        notification.setTaskId(assignment.getTask().getId());
        notification.setEventType(eventType);
        notification.setScheduledTime(scheduledTime);
        notification.setStatus("PENDING");

        scheduledNotificationRepository.save(notification);

        log.debug("Scheduled notification: task {}, user {}, type {}, time {}",
                assignment.getTask().getId(), assignment.getUserId(), eventType, scheduledTime);
    }

    private String getEventType(int hours) {
        switch (hours) {
            case 48: return "TASK_DEADLINE_2D";
            case 24: return "TASK_DEADLINE_1D";
            case 12: return "TASK_DEADLINE_12H";
            default: return "TASK_DEADLINE_" + hours + "H";
        }
    }

    private String getLabel(int hours) {
        switch (hours) {
            case 48: return "через 2 дня";
            case 24: return "через 1 день";
            case 12: return "через 12 часов";
            default: return "через " + hours + " часов";
        }
    }
}