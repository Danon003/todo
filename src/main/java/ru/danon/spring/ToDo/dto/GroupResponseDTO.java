package ru.danon.spring.ToDo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GroupResponseDTO {
    private Integer id;
    private String name;
    private String description;
    private Integer teacherId;
    private LocalDateTime createdAt;
}
