package ru.danon.spring.ToDo.dto;

import java.time.LocalDateTime;

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

    // Конструкторы
    public CommentDTO() {}

    public CommentDTO(String id, Integer taskId, Integer authorId, String authorName,
                      String authorRole, String content, LocalDateTime createdAt,
                      LocalDateTime updatedAt, String parentId, Integer repliesCount) {
        this.id = id;
        this.taskId = taskId;
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorRole = authorRole;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.parentId = parentId;
        this.repliesCount = repliesCount;
    }

    // Геттеры и сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Integer getTaskId() { return taskId; }
    public void setTaskId(Integer taskId) { this.taskId = taskId; }

    public Integer getAuthorId() { return authorId; }
    public void setAuthorId(Integer authorId) { this.authorId = authorId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorRole() { return authorRole; }
    public void setAuthorRole(String authorRole) { this.authorRole = authorRole; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public Integer getRepliesCount() { return repliesCount; }
    public void setRepliesCount(Integer repliesCount) { this.repliesCount = repliesCount; }

    public boolean isCanEdit() { return canEdit; }
    public void setCanEdit(boolean canEdit) { this.canEdit = canEdit; }

    public boolean isCanDelete() { return canDelete; }
    public void setCanDelete(boolean canDelete) { this.canDelete = canDelete; }
}