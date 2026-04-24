package ru.danon.spring.ToDo.services;

import ru.danon.spring.ToDo.dto.NotificationEvent;

import java.time.LocalDateTime;

public interface NotificationProducerService {
    void sendNotification(NotificationEvent event);

    // Вспомогательные методы для создания событий
    void sendTaskAssignedNotification(Long userId, String userRole, String taskTitle, Long taskId);

    void sendGroupAddedNotification(Long userId, String userRole, String groupName, Long groupId);

    void sendGroupRemovedNotification(Long userId, String userRole, String groupName, Long groupId);

    void sendTaskOverdueNotification(Long userId, String userRole, String taskTitle, Long taskId);

    void sendTaskDeadlineApproachingNotification(
            Long userId,
            String userRole,
            String taskTitle,
            Long taskId,
            String timeLabel,
            String eventType
    );

    void sendChangeRoleNotification(Long userId, String newRole);

    void sendTeacherRemovedNotification(Long id, String groupName);

    void sendTeacherAssignNotification(Long id, String name);

    void sendSolutionUploadedNotification(Long teacherUserId, String teacherRole,
                                          String studentName, String taskTitle, Long taskId);

    void sendSolutionGradedNotification(Long studentUserId, String studentRole,
                                        String teacherName, String taskTitle, Integer grade,
                                        String comment, Long taskId);

    void sendCommentNotification(Long studentUserId, String username,
                                 String taskTitle, Long taskId);

    void sendVideoMeetingCreatedNotification(Long userId,
                                             String userRole,
                                             String meetingTitle,
                                             LocalDateTime startTime,
                                             Long meetingId,
                                             String groupName);

    void sendVideoMeetingReminderNotification(Long userId,
                                              String userRole,
                                              String meetingTitle,
                                              LocalDateTime startTime,
                                              Long meetingId,
                                              String meetingUrl);
}
