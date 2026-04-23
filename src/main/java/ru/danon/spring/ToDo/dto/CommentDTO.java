package ru.danon.spring.ToDo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentDTO {
    private String id;
    private Integer taskId;
    private Integer authorId;
    private String authorName;
    private String authorRole;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String parentId;
    private Integer repliesCount;
    private boolean canEdit;
    private boolean canDelete;
}