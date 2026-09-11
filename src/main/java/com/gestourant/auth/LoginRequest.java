package com.gestourant.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(@NotBlank(message = "El correo o usuario es obligatorio") String identifier, @NotBlank(message = "La contraseña es obligatoria") @Size(max = 72) String password) { }
