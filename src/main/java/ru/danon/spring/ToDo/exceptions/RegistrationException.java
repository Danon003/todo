package ru.danon.spring.ToDo.exceptions;

import lombok.Getter;

@Getter
public class RegistrationException extends RuntimeException {

    private final String errorCode;

    public RegistrationException(String message) {
        super(message);
        this.errorCode = null;
    }

    public RegistrationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}