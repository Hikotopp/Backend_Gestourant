package com.gestourant.auth;

public final class PasswordPolicy {
    public static final int MIN_LENGTH = 12;
    public static final int MAX_LENGTH = 72;
    public static final String PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s]).+$";
    public static final String MESSAGE =
        "La contraseña debe tener entre 12 y 72 caracteres e incluir mayúscula, minúscula, número y carácter especial.";
    public static final String SIZE_MESSAGE = "La contraseña debe tener entre 12 y 72 caracteres.";

    private PasswordPolicy() { }
}
