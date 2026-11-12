package com.practice.aulaequilibrio.util;

public final class Validations {

    private Validations() {
    }

    public static String required(String value, String field, int maxLength) {
        String clean = value == null ? "" : value.trim();
        if (clean.isEmpty()) {
            throw new ValidationException("El campo " + field + " es obligatorio.");
        }
        if (clean.length() > maxLength) {
            throw new ValidationException(
                    "El campo " + field + " admite como máximo " + maxLength + " caracteres.");
        }
        return clean;
    }

    public static int positiveInteger(String value, String field) {
        String clean = value == null ? "" : value.trim();
        if (clean.isEmpty()) {
            throw new ValidationException("El campo " + field + " es obligatorio.");
        }
        int number;
        try {
            number = Integer.parseInt(clean);
        } catch (NumberFormatException exception) {
            throw new ValidationException("El campo " + field + " debe ser un número entero.");
        }
        if (number <= 0) {
            throw new ValidationException("El campo " + field + " debe ser mayor que cero.");
        }
        return number;
    }
}
