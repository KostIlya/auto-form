package ru.bell.auto_form.exception;

public class CriticalException extends RuntimeException {
    public CriticalException(String message) {
        super(message);
    }

    public CriticalException(Throwable cause) {
        super(cause);
    }
}
