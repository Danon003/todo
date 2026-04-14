package ru.danon.spring.ToDo.services;

import ru.danon.spring.ToDo.dto.NotificationEvent;

import java.time.LocalDateTime;

public interface NotificationProducerService {
    void sendNotification(NotificationEvent event);

    // Вспомогательные методы для создания событий
    void sendTaskAssignedNotification(Integer userId, String userRole, String taskTitle, Integer taskId);

    void sendGroupAddedNotification(Integer userId, String userRole, String groupName, Integer groupId);

    void sendGroupRemovedNotification(Integer userId, String userRole, String groupName, Integer groupId);

    void sendTaskOverdueNotification(Integer userId, String userRole, String taskTitle, Integer taskId);

    void sendTaskDeadlineApproachingNotification(
            Integer userId,
            String userRole,
            String taskTitle,
            Integer taskId,
            String timeLabel,
            String eventType
    );

    void sendChangeRoleNotification(Integer userId, String newRole);

    void sendTeacherRemovedNotification(Integer id, String groupName);

    void sendTeacherAssignNotification(Integer id, String name);

    void sendSolutionUploadedNotification(Integer teacherUserId, String teacherRole,
                                          String studentName, String taskTitle, Integer taskId);

    void sendSolutionGradedNotification(Integer studentUserId, String studentRole,
                                        String teacherName, String taskTitle, Integer grade,
                                        String comment, Integer taskId);

    void sendCommentNotification(Integer studentUserId, String username,
                                 String taskTitle, Integer taskId);

    void sendVideoMeetingCreatedNotification(Integer userId,
                                             String userRole,
                                             String meetingTitle,
                                             LocalDateTime startTime,
                                             Integer meetingId,
                                             String groupName);

    void sendVideoMeetingReminderNotification(Integer userId,
                                              String userRole,
                                              String meetingTitle,
                                              LocalDateTime startTime,
                                              Integer meetingId,
                                              String meetingUrl);
}
