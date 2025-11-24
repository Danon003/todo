package ru.danon.spring.ToDo.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class CreateVideoMeetingDTO {
    @NotEmpty(message = "Название встречи не должно быть пустым")
    @Size(min = 1, max = 255, message = "Название должно быть от 1 до 255 символов")
    private String title;

    private String description;

    @NotNull(message = "Время начала встречи обязательно")
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer groupId;

    public CreateVideoMeetingDTO() {}

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }
}


