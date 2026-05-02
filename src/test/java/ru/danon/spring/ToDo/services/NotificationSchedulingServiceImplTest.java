package ru.danon.spring.ToDo.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.danon.spring.ToDo.models.postgre.ScheduledNotification;
import ru.danon.spring.ToDo.models.postgre.Task;
import ru.danon.spring.ToDo.models.postgre.TaskAssignment;
import ru.danon.spring.ToDo.repositories.jpa.ScheduledNotificationRepository;
import ru.danon.spring.ToDo.services.impl.NotificationSchedulingServiceImpl;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationSchedulingServiceImplTest {

    @Mock
    private ScheduledNotificationRepository scheduledNotificationRepository;

    @InjectMocks
    private NotificationSchedulingServiceImpl service;

    @Test
    void scheduleTaskNotificationsShouldSkipWhenDeadlineMissing() {
        Task task = new Task();
        task.setId(1L);
        TaskAssignment assignment = new TaskAssignment();
        assignment.setTask(task);
        assignment.setUserId(2L);

        service.scheduleTaskNotifications(assignment);

        verify(scheduledNotificationRepository, never()).save(any());
    }

    @Test
    void scheduleTaskNotificationsShouldCreateThreeNotifications() {
        Task task = new Task();
        task.setId(1L);
        task.setDeadline(LocalDateTime.now().plusDays(3));
        TaskAssignment assignment = new TaskAssignment();
        assignment.setTask(task);
        assignment.setUserId(2L);
        when(scheduledNotificationRepository.existsByTaskIdAndUserIdAndEventTypeAndStatus(anyLong(), anyLong(), anyString(), eq("PENDING")))
                .thenReturn(false);

        service.scheduleTaskNotifications(assignment);

        verify(scheduledNotificationRepository, times(3)).save(any(ScheduledNotification.class));
    }

    @Test
    void cancelTaskNotificationsShouldMarkPendingAsCancelled() {
        ScheduledNotification n1 = new ScheduledNotification();
        n1.setStatus("PENDING");
        ScheduledNotification n2 = new ScheduledNotification();
        n2.setStatus("PENDING");
        when(scheduledNotificationRepository.findByTaskIdAndUserIdAndStatus(3L, 4L, "PENDING"))
                .thenReturn(List.of(n1, n2));

        service.cancelTaskNotifications(3L, 4L);

        assertEquals("CANCELLED", n1.getStatus());
        assertEquals("CANCELLED", n2.getStatus());
        verify(scheduledNotificationRepository).saveAll(List.of(n1, n2));
    }

    @Test
    void cancelAllTaskNotificationsShouldCancelOnlyPending() {
        ScheduledNotification pending = new ScheduledNotification();
        pending.setStatus("PENDING");
        ScheduledNotification sent = new ScheduledNotification();
        sent.setStatus("SENT");
        when(scheduledNotificationRepository.findByTaskId(8L)).thenReturn(List.of(pending, sent));

        service.cancelAllTaskNotifications(8L);

        assertEquals("CANCELLED", pending.getStatus());
        assertEquals("SENT", sent.getStatus());
        ArgumentCaptor<List<ScheduledNotification>> captor = ArgumentCaptor.forClass(List.class);
        verify(scheduledNotificationRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
    }
}
