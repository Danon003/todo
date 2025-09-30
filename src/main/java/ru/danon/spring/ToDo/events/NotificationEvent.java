package ru.danon.spring.ToDo.events;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public class NotificationEvent {
    private String id;
    private String type;
    private String title;
    private String message;
    private Integer userId;
    private String userRole;
    private Timestamp createdAt;
    private Map<String, Object> metadata;

    public NotificationEvent() {}

    public NotificationEvent(String id, String type, String title, String message, Integer userId, String userRole, Timestamp createdAt, Map<String, Object> metadata) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.message = message;
        this.userId = userId;
        this.userRole = userRole;
        this.createdAt = createdAt;
        this.metadata = metadata;
    }

    // Геттеры и сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}