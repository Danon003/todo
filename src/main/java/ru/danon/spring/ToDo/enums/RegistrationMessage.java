package ru.danon.spring.ToDo.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RegistrationMessage {

    USERNAME_TAKEN("Логин уже занят"),
    EMAIL_TAKEN("Email уже используется"),
    USERNAME_EMAIL_TAKEN("Логин и Email уже заняты"),
    INVALID_USERNAME("Некорректное имя пользователя"),
    INVALID_EMAIL("Некорректный email"),
    INVALID_PASSWORD("Пароль должен содержать минимум 4 символа"),
    PASSWORDS_DO_NOT_MATCH("Пароли не совпадают"),
    REGISTRATION_SUCCESS("Регистрация успешно завершена"),
    USER_NOT_FOUND("Пользователь не найден"),
    INVALID_RESET_CODE("Неверный код подтверждения"),
    RESET_CODE_EXPIRED("Срок действия кода истёк"),
    PASSWORD_RESET_SUCCESS("Пароль успешно изменён");

    private final String message;
}