package com.practice.aulaequilibrio.util;

import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.JDBCConnectionException;

/**
 * Traduce excepciones de Hibernate a mensajes comprensibles.
 */
public final class ErrorMessages {

    private ErrorMessages() {
    }

    public static ValidationException onSave(String entity, RuntimeException exception) {
        if (has(exception, ConstraintViolationException.class)) {
            return new ValidationException(
                    "No se pudo guardar " + entity + ": ya existe un registro con esos datos.", exception);
        }
        return new ValidationException("No se pudo guardar " + entity + ". " + detail(exception), exception);
    }

    public static ValidationException onDelete(String entity, RuntimeException exception) {
        if (has(exception, ConstraintViolationException.class)) {
            return new ValidationException(
                    "No se puede eliminar " + entity + " porque está siendo usado en otros registros.", exception);
        }
        return new ValidationException("No se pudo eliminar " + entity + ". " + detail(exception), exception);
    }

    public static String forUser(Throwable exception) {
        if (exception instanceof ValidationException) {
            return exception.getMessage();
        }
        return "Ocurrió un error inesperado. " + detail(exception);
    }

    private static String detail(Throwable exception) {
        if (has(exception, JDBCConnectionException.class)) {
            return "No hay conexión con PostgreSQL; revise que el servicio esté iniciado y el archivo .env.";
        }
        return "Revise la conexión con la base de datos e inténtelo de nuevo.";
    }

    private static boolean has(Throwable exception, Class<? extends Throwable> type) {
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return true;
            }
        }
        return false;
    }
}
