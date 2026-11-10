package com.practice.aulaequilibrio.service;

/**
 * Error con un mensaje legible que se puede mostrar directamente al usuario.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
