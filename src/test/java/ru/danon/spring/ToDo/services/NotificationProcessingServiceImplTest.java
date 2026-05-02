package ru.danon.spring.ToDo.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.danon.spring.ToDo.models.postgre.ScheduledNotification;
import ru.danon.spring.ToDo.models.postgre.Task;
import ru.danon.spring.ToDo.repositories.jpa.ScheduledNotificationRepository;
import ru.danon.spring.ToDo.repositories.jpa.TaskAssignmentRepository;
import ru.danon.spring.ToDo.repositories.jpa.TaskRepository;
import ru.danon.spring.ToDo.services.impl.NotificationProcessingServiceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationProcessingServiceImplTest {

    @Mock
    private ScheduledNotificationRepository scheduledNotificationRepository;
    @Mock
    private TaskAssignmentRepository taskAssignmentRepository;
    @Mock
    private NotificationProducerService notificationProducerServiceImpl;
    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private NotificationProcessingServiceImpl service;

    @Test
    void processScheduledNotificationsShouldHandleEmptyQueues() {
        when(scheduledNotificationRepository.findByStatusAndScheduledTimeBetween(eq("PENDING"), any(), any()))
                .thenReturn(List.of());
        when(scheduledNotificationRepository.findByStatusAndAttemptCountLessThan("FAILED", 3))
                .thenReturn(List.of());

        service.processScheduledNotifications();

        verify(scheduledNotificationRepository).findByStatusAndScheduledTimeBetween(eq("PENDING"), any(), any());
        verify(scheduledNotificationRepository).findByStatusAndAttemptCountLessThan("FAILED", 3);
        verifyNoInteractions(notificationProducerServiceImpl);
    }

    @Test
    void processScheduledNotificationsShouldSendAndMarkSent() {
        ScheduledNotification n = new ScheduledNotification();
        n.setId(1L);
        n.setTaskId(10L);
        n.setUserId(20L);
        n.setEventType("TASK_DEADLINE_1D");
        n.setStatus("PENDING");
        n.setAttemptCount(0);
        n.setScheduledTime(LocalDateTime.now().minusMinutes(1));
        Task task = new Task();
        task.setTitle("Task X");

        when(scheduledNotificationRepository.findByStatusAndScheduledTimeBetween(eq("PENDING"), any(), any()))
                .thenReturn(List.of(n));
        when(scheduledNotificationRepository.findByStatusAndAttemptCountLessThan("FAILED", 3))
                .thenReturn(List.of());
        when(taskAssignmentRepository.existsValidTaskForNotification(eq(10L), eq(20L), any()))
                .thenReturn(true);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

        service.processScheduledNotifications();

        verify(notificationProducerServiceImpl).sendTaskDeadlineApproachingNotification(
                eq(20L), eq("ROLE_STUDENT"), eq("Task X"), eq(10L), eq("через 1 день"), eq("TASK_DEADLINE_1D"));
        verify(scheduledNotificationRepository, atLeastOnce()).save(n);
    }
}
