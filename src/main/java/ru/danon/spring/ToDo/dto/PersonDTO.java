package ru.danon.spring.ToDo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PersonDTO {
    @NotEmpty(message = "Имя не должно быть пустым")
    @Size(min = 2, max = 255, message = "Имя должно быть от 2 до 255 символов")
    private String username;

    @Email(message = "Email должен быть корректным")
    @NotEmpty(message = "Email не должен быть пустым")
    private String email;

    private String password;

}
