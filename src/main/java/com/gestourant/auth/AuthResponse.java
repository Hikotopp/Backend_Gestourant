package com.gestourant.auth;
import com.gestourant.user.Role;
public record AuthResponse(String token, String tokenType, Long userId, String username, String email, Role role) { }
