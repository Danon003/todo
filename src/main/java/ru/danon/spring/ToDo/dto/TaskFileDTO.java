package ru.danon.spring.ToDo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskFileDTO {
    private Long id;
    private String originalFileName;
    private Long fileSize;
    private String fileType;
    private LocalDateTime uploadedAt;
    private Long uploadedById;
    private String uploadedByName;
    private String downloadUrl;
}