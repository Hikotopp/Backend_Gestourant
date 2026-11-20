package com.gestourant.user;

import java.time.LocalDateTime;

public record ManagedUserView(Long id, String username, String email, Role role, boolean enabled, LocalDateTime createdAt) {
    public static ManagedUserView from(User user) {
        return new ManagedUserView(user.getId(), user.getUsername(), user.getEmail(), user.getRole(), user.isEnabled(), user.getCreatedAt());
    }
}
