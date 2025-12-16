package ru.danon.spring.ToDo.exceptions;

public class NotSolutionException extends RuntimeException {
    public NotSolutionException(String message) {
        super(message);
    }
}
