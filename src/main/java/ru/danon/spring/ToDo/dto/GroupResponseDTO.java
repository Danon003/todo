package ru.danon.spring.ToDo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GroupResponseDTO {
    private Long id;
    private String name;
    private String description;
    private Long teacherId;
    private LocalDateTime createdAt;
}
