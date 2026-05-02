package ru.danon.spring.ToDo.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateVideoMeetingDTO {
    @NotEmpty(message = "Название встречи не должно быть пустым")
    @Size(min = 1, max = 255, message = "Название должно быть от 1 до 255 символов")
    private String title;

    private String description;

    @NotNull(message = "Время начала встречи обязательно")
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long groupId;
}


