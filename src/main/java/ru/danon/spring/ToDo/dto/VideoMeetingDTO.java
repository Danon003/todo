package ru.danon.spring.ToDo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VideoMeetingDTO {
    private Long id;
    private String title;
    private String description;
    private String meetingUrl;
    private String meetingId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long createdById;
    private String createdByUsername;
    private Long groupId;
    private String groupName;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


