package com.gestourant.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 50) private String username;
    @Column(nullable = false, unique = true, length = 150) private String email;
    @Column(name = "password_hash", nullable = false) private String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) private Role role;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    protected User() { }
    public User(String username, String email, String passwordHash, Role role) { this.username = username; this.email = email; this.passwordHash = passwordHash; this.role = role; this.createdAt = LocalDateTime.now(); }
    public Long getId() { return id; } public String getUsername() { return username; } public String getEmail() { return email; } public String getPasswordHash() { return passwordHash; } public Role getRole() { return role; }
}
