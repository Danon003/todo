package ru.danon.spring.ToDo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LogResponseDTO {

        private Integer id;
        private PersonDTO user;
        private String oldRole;
        private String newRole;
        private LocalDateTime changedAt;
}
