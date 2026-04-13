package ru.danon.spring.ToDo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SolutionDTO {
    private Integer studentId;
    private String studentName;
    private String fileName;
    private Long fileSize;
    private LocalDateTime uploadedAt;
    private String downloadUrl;
    private Integer grade;
    private String teacherComment;
    private boolean canUpload;
}