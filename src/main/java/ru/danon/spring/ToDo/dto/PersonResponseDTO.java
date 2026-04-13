package ru.danon.spring.ToDo.dto;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class PersonResponseDTO {
    private Integer id;
    private String username;
    private String email;
    private String role;
    private LocalDateTime createdAt;
}
