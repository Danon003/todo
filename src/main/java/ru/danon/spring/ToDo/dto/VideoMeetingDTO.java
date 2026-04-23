package ru.danon.spring.ToDo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VideoMeetingDTO {
    private Integer id;
    private String title;
    private String description;
    private String meetingUrl;
    private String meetingId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer createdById;
    private String createdByUsername;
    private Integer groupId;
    private String groupName;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


