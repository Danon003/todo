package ru.danon.spring.ToDo.dto;

import java.util.Map;

public class TaskStatDTO {
    private Integer id;
    private String status; // Для студента
    private Map<String, Integer> statusStatistics; // Для группы
    private Integer userId;
    private Integer groupId;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Integer> getStatusStatistics() {
        return statusStatistics;
    }

    public void setStatusStatistics(Map<String, Integer> statusStatistics) {
        this.statusStatistics = statusStatistics;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }
}
