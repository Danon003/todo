package ru.danon.spring.ToDo.exceptions;

public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(String entity, Object id) {
        super(String.format("%s с id '%s' не найден(а)", entity, id));
    }
}