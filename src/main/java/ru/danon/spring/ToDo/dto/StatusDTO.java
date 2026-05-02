package ru.danon.spring.ToDo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StatusDTO {
    @NotBlank(message = "Status cannot be blank")
    String userStatus;

    public StatusDTO(String userStatus) {
        this.userStatus = userStatus;
    }
}
