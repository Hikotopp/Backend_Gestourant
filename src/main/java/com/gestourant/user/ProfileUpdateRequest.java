package com.gestourant.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
    @NotBlank @Size(min = 3, max = 50)
    @Pattern(regexp = "^[A-Za-z0-9_.-]+$", message = "El usuario solo puede incluir letras, números, punto, guion y guion bajo")
    String username,
    @NotBlank @Email @Size(max = 150) String email,
    @NotBlank @Size(max = 72) String currentPassword,
    @Size(max = 72) String newPassword
) { }
