package ru.danon.spring.ToDo.dto;

import lombok.Data;

@Data
public class StatusDTO {
    String userStatus;

    public StatusDTO(String userStatus) {
        this.userStatus = userStatus;
    }
}
