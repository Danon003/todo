package ru.danon.spring.ToDo.dto;

public class StatusDTO {
    String userStatus;

    public StatusDTO(String userStatus) {
        this.userStatus = userStatus;
    }

    public String getStatus() {
        return userStatus;
    }

    public void setStatus(String userStatus) {
        this.userStatus = userStatus;
    }
}
