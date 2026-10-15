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
    @Column(name = "privacy_consent_at") private LocalDateTime privacyConsentAt;
    @Column(name = "active_session_id", length = 36) private String activeSessionId;
    @Column(name = "active_session_expires_at") private LocalDateTime activeSessionExpiresAt;
    @Column(name = "last_activity_at") private LocalDateTime lastActivityAt;
    @Column(name = "active_session_locked", nullable = false) private boolean activeSessionLocked;
    protected User() { }
    public User(String username, String email, String passwordHash, Role role) { this(username, email, passwordHash, role, null); }
    public User(String username, String email, String passwordHash, Role role, LocalDateTime privacyConsentAt) { this.username = username; this.email = email; this.passwordHash = passwordHash; this.role = role; this.createdAt = LocalDateTime.now(); this.privacyConsentAt = privacyConsentAt; }
    public Long getId() { return id; } public String getUsername() { return username; } public String getEmail() { return email; } public String getPasswordHash() { return passwordHash; } public Role getRole() { return role; } public LocalDateTime getPrivacyConsentAt() { return privacyConsentAt; }
    public String getActiveSessionId() { return activeSessionId; }
    public LocalDateTime getActiveSessionExpiresAt() { return activeSessionExpiresAt; }
    public LocalDateTime getLastActivityAt() { return lastActivityAt; }
    public boolean isActiveSessionLocked() { return activeSessionLocked; }
    public void setActiveSession(String sessionId, LocalDateTime expiresAt, LocalDateTime lastActivityAt) {
        this.activeSessionId = sessionId;
        this.activeSessionExpiresAt = expiresAt;
        this.lastActivityAt = lastActivityAt;
        this.activeSessionLocked = false;
    }
    public void setLastActivityAt(LocalDateTime lastActivityAt) { this.lastActivityAt = lastActivityAt; }
    public void setActiveSessionLocked(boolean activeSessionLocked) { this.activeSessionLocked = activeSessionLocked; }
    public void clearActiveSession() {
        this.activeSessionId = null;
        this.activeSessionExpiresAt = null;
        this.lastActivityAt = null;
        this.activeSessionLocked = false;
    }
}
