package com.gestourant.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.gestourant.user.Role;

public record RegisterRequest(
    @NotBlank(message = "El usuario es obligatorio") @Size(min = 3, max = 50, message = "El usuario debe tener entre 3 y 50 caracteres") @Pattern(regexp = "^[A-Za-z0-9_.-]+$", message = "El usuario solo puede incluir letras, números, punto, guion y guion bajo") String username,
    @NotBlank(message = "El correo es obligatorio") @Email(message = "El correo electrónico no es válido") @Size(max = 150) String email,
    @NotBlank(message = "La contraseña es obligatoria") @Size(min = 10, max = 72, message = "La contraseña debe tener entre 10 y 72 caracteres") @Pattern(regexp = ".*\\d.*", message = "La contraseña debe incluir al menos un número") String password,
    Role role,
    String adminCode,
    @Size(max = 2048) String captchaToken,
    @Size(max = 200) String website,
    @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.AssertTrue(message = "Debes autorizar el tratamiento de datos para crear una cuenta") Boolean privacyConsent
) { }
