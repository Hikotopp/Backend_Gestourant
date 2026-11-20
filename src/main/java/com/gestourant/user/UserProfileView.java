package com.gestourant.user;

public record UserProfileView(Long id, String username, String email, Role role, boolean photoAvailable) {
    public static UserProfileView from(User user, boolean photoAvailable) {
        return new UserProfileView(user.getId(), user.getUsername(), user.getEmail(), user.getRole(), photoAvailable);
    }
}
