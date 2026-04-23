package ru.danon.spring.ToDo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskFileDTO {
    private Integer id;
    private String originalFileName;
    private Long fileSize;
    private String fileType;
    private LocalDateTime uploadedAt;
    private Integer uploadedById;
    private String uploadedByName;
    private String downloadUrl;
}