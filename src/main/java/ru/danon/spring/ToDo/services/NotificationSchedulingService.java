package ru.danon.spring.ToDo.services;

import org.springframework.transaction.annotation.Transactional;
import ru.danon.spring.ToDo.models.postgre.TaskAssignment;

public interface NotificationSchedulingService {
    @Transactional
    void scheduleTaskNotifications(TaskAssignment assignment);

    @Transactional
    void rescheduleTaskNotifications(TaskAssignment assignment);

    @Transactional
    void cancelTaskNotifications(Integer taskId, Integer userId);

    @Transactional
    void cancelAllTaskNotifications(Integer taskId);
}
