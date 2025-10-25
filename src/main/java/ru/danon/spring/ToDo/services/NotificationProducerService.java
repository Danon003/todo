package ru.danon.spring.ToDo.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.danon.spring.ToDo.events.NotificationEvent;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationProducerService {

    private static final Logger log = LoggerFactory.getLogger(NotificationProducerService.class);
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    public NotificationProducerService(KafkaTemplate<String, NotificationEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendNotification(NotificationEvent event) {
        try {
            kafkaTemplate.send("notifications-topic", event.getUserId().toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Notification sent successfully: {}", event);
                        } else {
                            log.error("Failed to send notification: {}", event, ex);
                        }
                    });
        } catch (Exception e) {
            log.error("Error sending notification to Kafka: {}", e.getMessage());
        }
    }

    // Вспомогательные методы для создания событий
    public void sendTaskAssignedNotification(Integer userId, String userRole, String taskTitle, Integer taskId) {
        NotificationEvent event = new NotificationEvent();
        event.setId(UUID.randomUUID().toString());
        event.setType("TASK");
        event.setTitle("Новая задача");
        event.setMessage("Вам назначена задача: " + taskTitle);
        event.setUserId(userId);
        event.setUserRole(userRole);
        event.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        event.setMetadata(Map.of("taskId", taskId));

        sendNotification(event);
    }

    public void sendGroupAddedNotification(Integer userId, String userRole, String groupName, Integer groupId) {
        NotificationEvent event = new NotificationEvent();
        event.setId(UUID.randomUUID().toString());
        event.setType("GROUP");
        event.setTitle("Добавление в группу");
        event.setMessage("Вас добавили в группу: " + groupName);
        event.setUserId(userId);
        event.setUserRole(userRole);
        event.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        event.setMetadata(Map.of("groupId", groupId));

        sendNotification(event);
    }

    public void sendGroupRemovedNotification(Integer userId, String userRole, String groupName, Integer groupId) {
        NotificationEvent event = new NotificationEvent();
        event.setId(UUID.randomUUID().toString());
        event.setType("GROUP");
        event.setTitle("Удаление из группы");
        event.setMessage("Вас удалили из группы: " + groupName);
        event.setUserId(userId);
        event.setUserRole(userRole);
        event.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        event.setMetadata(Map.of("groupId", groupId));

        sendNotification(event);
    }

    public void sendTaskOverdueNotification(Integer userId, String userRole, String taskTitle, Integer taskId) {
        NotificationEvent event = new NotificationEvent();
        event.setId(UUID.randomUUID().toString());
        event.setType("TASK_OVERDUE");
        event.setTitle("Задача просрочена");
        event.setMessage("Задача \"" + taskTitle + "\" просрочена!");
        event.setUserId(userId);
        event.setUserRole(userRole);
        event.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        event.setMetadata(Map.of("taskId", taskId));

        sendNotification(event);
    }

    public void sendTaskDeadlineApproachingNotification(
            Integer userId,
            String userRole,
            String taskTitle,
            Integer taskId,
            String timeLabel,
            String eventType
    ) {
        NotificationEvent event = new NotificationEvent();
        event.setId(UUID.randomUUID().toString());
        event.setType(eventType);
        event.setTitle("Скоро дедлайн!");
        event.setMessage("По задаче \"" + taskTitle + "\" дедлайн истекает " + timeLabel + "!");
        event.setUserId(userId);
        event.setUserRole(userRole);
        event.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        event.setMetadata(Map.of(
                "taskId", taskId,
                "deadlineIn", timeLabel
        ));

        sendNotification(event);
    }

    public void sendChangeRoleNotification(Integer userId, String newRole) {
        NotificationEvent event = new NotificationEvent();

        event.setId(UUID.randomUUID().toString());
        event.setType("CHANGE_ROLE");
        event.setTitle("У вас новая роль!");
        event.setMessage("Теперь у вас доступ с правами: " + newRole.replace("ROLE_", ""));
        event.setUserId(userId);
        event.setUserRole(newRole);
        event.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        event.setMetadata(Map.of("role", newRole));

        sendNotification(event);
    }

    public void sendTeacherRemovedNotification(Integer id, String groupName) {
        NotificationEvent event = new NotificationEvent();

        event.setId(UUID.randomUUID().toString());
        event.setType("TEACHER_REMOVED");
        event.setTitle("Вас сняли с должности преподавателя");
        event.setMessage("Вы больше не отвечаете за " + groupName);
        event.setUserId(id);
        event.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));

        sendNotification(event);
    }

    public void sendTeacherAssignNotification(Integer id, String name) {
        NotificationEvent event = new NotificationEvent();

        event.setId(UUID.randomUUID().toString());
        event.setType("TEACHER_ASSIGN");
        event.setTitle("Назначен преподаватель");
        event.setMessage("Вы ответственны за " + name);
        event.setUserId(id);
        event.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));

        sendNotification(event);
    }

    public void sendSolutionUploadedNotification(Integer teacherUserId, String teacherRole,
                                                 String studentName, String taskTitle, Integer taskId) {
        NotificationEvent event = new NotificationEvent();
        event.setId(UUID.randomUUID().toString());
        event.setType("SOLUTION_UPLOADED");
        event.setTitle("Новое решение задачи");
        event.setMessage("Студент " + studentName + " загрузил решение по задаче: " + taskTitle);
        event.setUserId(teacherUserId);
        event.setUserRole(teacherRole);
        event.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        event.setMetadata(Map.of(
                "taskId", taskId,
                "studentName", studentName,
                "taskTitle", taskTitle
        ));

        sendNotification(event);
    }

    /**
     * Уведомление об оценке решения преподавателем
     */
    public void sendSolutionGradedNotification(Integer studentUserId, String studentRole,
                                               String teacherName, String taskTitle, Integer grade,
                                               String comment, Integer taskId) {
        NotificationEvent event = new NotificationEvent();
        event.setId(UUID.randomUUID().toString());
        event.setType("SOLUTION_GRADED");
        event.setTitle("Оценка вашего решения");
        event.setMessage("Преподаватель " + teacherName + " оценил ваше решение по задаче: " + taskTitle);
        event.setUserId(studentUserId);
        event.setUserRole(studentRole);
        event.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        event.setMetadata(Map.of(
                "taskId", taskId,
                "teacherName", teacherName,
                "taskTitle", taskTitle,
                "grade", grade,
                "comment", comment
        ));

        sendNotification(event);
    }

    public void sendCommentNotification(Integer studentUserId, String username,
                                        String taskTitle, Integer taskId) {
        NotificationEvent event = new NotificationEvent();
        event.setId(UUID.randomUUID().toString());
        event.setType("New_Comment");
        event.setTitle("Новый комментарий");
        event.setMessage("Пользователь " + username + " оставил комментарий к задаче: " + taskTitle);
        event.setUserId(studentUserId);
        event.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        event.setMetadata(Map.of(
                "taskId", taskId,
                "taskTitle", taskTitle
        ));

        sendNotification(event);
    }
}