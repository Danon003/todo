package ru.danon.spring.ToDo.services;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.transaction.annotation.Transactional;
import ru.danon.spring.ToDo.models.postgre.Person;

public interface RegistrationService {
    @Transactional
    void register(Person person);

    @Transactional
    void updatePasswordsToBCrypt();

    void initiatePasswordReset(@NotBlank(message = "Email не может быть пустым")
                               @Email(message = "Некорректный формат email") String email);

    @Transactional
    void resetPassword(@NotBlank(message = "Email не может быть пустым")
                       @Email(message = "Некорректный формат email") String email,
                       @NotBlank(message = "Код подтверждения не может быть пустым") String code,
                       @NotBlank(message = "Пароль не может быть пустым")
                       @Size(min = 4, message = "Пароль должен содержать минимум 4 символов") String newPassword);

    boolean validateResetToken(String token, String email);
}
