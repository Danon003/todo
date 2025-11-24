package ru.danon.spring.ToDo.repositories.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.danon.spring.ToDo.models.ScheduledNotification;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduledNotificationRepository extends JpaRepository<ScheduledNotification, Integer> {
    // Для обработки: находим уведомления, готовые к отправке
    List<ScheduledNotification> findByStatusAndScheduledTimeBetween(
            String status, LocalDateTime start, LocalDateTime end);

    // Для отмены: находим все pending уведомления для задачи
    List<ScheduledNotification> findByTaskIdAndUserIdAndStatus(
            Integer taskId, Integer userId, String status);

    // Проверяем, не было ли уже запланировано такое уведомление
    boolean existsByTaskIdAndUserIdAndEventTypeAndStatus(
            Integer taskId, Integer userId, String eventType, String status);

    // Для повторной обработки failed уведомлений
    List<ScheduledNotification> findByStatusAndAttemptCountLessThan(
            String status, Integer maxAttempts);

    // Находим уведомления по задаче и пользователю
    @Query("SELECT sn FROM ScheduledNotification sn WHERE sn.taskId = :taskId AND sn.userId = :userId")
    List<ScheduledNotification> findByTaskAndUser(@Param("taskId") Integer taskId,
                                                  @Param("userId") Integer userId);

    List<ScheduledNotification> findByTaskId(Integer taskId);
}
