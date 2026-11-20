package com.gestourant.user;

import jakarta.validation.constraints.NotNull;

public record ManagedUserUpdateRequest(@NotNull Role role, @NotNull Boolean enabled) { }
