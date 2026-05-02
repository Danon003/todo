package ru.danon.spring.ToDo.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.Map;
@Data
public class NotificationEvent {
    private String id;
    private String type;
    private String title;
    private String message;
    private Long userId;
    private String userRole;
    private Timestamp createdAt;
    private Map<String, Object> metadata;

    public NotificationEvent() {}

    public NotificationEvent(String id, String type, String title, String message, Long userId, String userRole, Timestamp createdAt, Map<String, Object> metadata) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.message = message;
        this.userId = userId;
        this.userRole = userRole;
        this.createdAt = createdAt;
        this.metadata = metadata;
    }
}