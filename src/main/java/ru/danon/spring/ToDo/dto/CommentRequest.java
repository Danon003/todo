package ru.danon.spring.ToDo.dto;

public class CommentRequest {
    private String content;
    private String parentId; // для будущих ответов

    // Конструкторы
    public CommentRequest() {}

    public CommentRequest(String content) {
        this.content = content;
    }

    // Геттеры и сеттеры
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
}