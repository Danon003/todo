package ru.danon.spring.ToDo.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.danon.spring.ToDo.models.postgre.ScheduledNotification;
import ru.danon.spring.ToDo.models.postgre.TaskAssignment;
import ru.danon.spring.ToDo.repositories.jpa.ScheduledNotificationRepository;
import ru.danon.spring.ToDo.services.NotificationSchedulingService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSchedulingServiceImpl implements NotificationSchedulingService {

    private final ScheduledNotificationRepository scheduledNotificationRepository;

    // Конфигурируемые интервалы уведомлений (часы до дедлайна)
    private final List<Integer> NOTIFICATION_INTERVALS = Arrays.asList(48, 24, 12);

    @Transactional @Override public void scheduleTaskNotifications(TaskAssignment assignment) {
        LocalDateTime deadline = assignment.getTask().getDeadline();
        if (deadline == null) {
            log.warn("Задача id={} не имеет дедлайна, пропускаем планирование уведомлений", assignment.getTask().getId());
            return;
        }

        log.info("Планирование уведомлений для задачи id={}, пользователь id={}, дедлайн: {}",
                assignment.getTask().getId(), assignment.getUserId(), deadline);

        // Планируем уведомления для каждого интервала
        for (Integer hours : NOTIFICATION_INTERVALS) {
            scheduleNotification(assignment, deadline.minusHours(hours),
                    getEventType(hours), getLabel(hours));
        }
    }

    @Transactional @Override public void rescheduleTaskNotifications(TaskAssignment assignment) {
        log.info("Перепланирование уведомлений для задачи id={}, пользователь id={}",
                assignment.getTask().getId(), assignment.getUserId());

        // Сначала отменяем старые уведомления
        cancelTaskNotifications(assignment.getTask().getId(), assignment.getUserId());

        // Затем создаем новые
        scheduleTaskNotifications(assignment);
    }

    @Transactional @Override public void cancelTaskNotifications(Long taskId, Long userId) {
        List<ScheduledNotification> pendingNotifications =
                scheduledNotificationRepository.findByTaskIdAndUserIdAndStatus(taskId, userId, "PENDING");

        if (!pendingNotifications.isEmpty()) {
            for (ScheduledNotification notification : pendingNotifications) {
                notification.setStatus("CANCELLED");
            }
            scheduledNotificationRepository.saveAll(pendingNotifications);
            log.info("Отменено {} ожидающих уведомлений для задачи id={}, пользователь id={}",
                    pendingNotifications.size(), taskId, userId);
        }
    }

    @Transactional @Override public void cancelAllTaskNotifications(Long taskId) {
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
            log.info("Отменено {} ожидающих уведомлений для задачи id={}", toCancel.size(), taskId);
        }
    }

    private void scheduleNotification(TaskAssignment assignment,
                                      LocalDateTime scheduledTime,
                                      String eventType, String label) {

        // Проверяем, не было ли уже запланировано такое уведомление
        if (scheduledNotificationRepository.existsByTaskIdAndUserIdAndEventTypeAndStatus(
                assignment.getTask().getId(), assignment.getUserId(), eventType, "PENDING")) {
            log.debug("Уведомление уже запланировано: taskId={}, userId={}, type={}",
                    assignment.getTask().getId(), assignment.getUserId(), eventType);
            return;
        }

        // Проверяем, что время уведомления еще не прошло
        if (scheduledTime.isBefore(LocalDateTime.now())) {
            log.debug("Время уведомления уже прошло: taskId={}, type={}, time={}",
                    assignment.getTask().getId(), eventType, scheduledTime);
            return;
        }

        ScheduledNotification notification = new ScheduledNotification();
        notification.setUserId(assignment.getUserId());
        notification.setTaskId(assignment.getTask().getId());
        notification.setEventType(eventType);
        notification.setScheduledTime(scheduledTime);
        notification.setStatus("PENDING");

        scheduledNotificationRepository.save(notification);

        log.debug("Уведомление запланировано: taskId={}, userId={}, type={}, time={}",
                assignment.getTask().getId(), assignment.getUserId(), eventType, scheduledTime);
    }

    private String getEventType(int hours) {
        return switch (hours) {
            case 48 -> "TASK_DEADLINE_2D";
            case 24 -> "TASK_DEADLINE_1D";
            case 12 -> "TASK_DEADLINE_12H";
            default -> "TASK_DEADLINE_" + hours + "H";
        };
    }

    private String getLabel(int hours) {
        return switch (hours) {
            case 48 -> "через 2 дня";
            case 24 -> "через 1 день";
            case 12 -> "через 12 часов";
            default -> "через " + hours + " часов";
        };
    }
}
