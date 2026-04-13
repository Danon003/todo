package ru.danon.spring.ToDo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityDTO {
    private Integer id;
    private String description;
    private LocalDateTime timestamp;
}
