package ru.danon.spring.ToDo.dto;

import java.time.LocalDateTime;

// MyTaskDTO.java
public class MyTaskDTO {
    private Integer id;
    private String title;
    private String description;
    private LocalDateTime deadline;
    private String priority;
    private Integer authorId;
    private String userStatus; // ← статус именно этого пользователя

    public MyTaskDTO() {}
    public MyTaskDTO(Integer id, String title, String description,
                     LocalDateTime deadline, String priority,
                      Integer authorId, String userStatus) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.priority = priority;
        this.authorId = authorId;
        this.userStatus = userStatus;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }


    public String getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(String userStatus) {
        this.userStatus = userStatus;
    }

    public Integer getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Integer authorId) {
        this.authorId = authorId;
    }
}