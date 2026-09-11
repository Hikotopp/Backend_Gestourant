package com.gestourant.audit;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id") private Long userId;
    @Column(length = 50) private String username;
    @Column(nullable = false, length = 20) private String action;
    @Column(nullable = false, length = 120) private String resource;
    @Column(length = 500) private String details;
    @Column(name = "ip_address", length = 45) private String ipAddress;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    protected AuditLog() { }
    public AuditLog(Long userId, String username, String action, String resource, String details, String ipAddress) { this.userId = userId; this.username = username; this.action = action; this.resource = resource; this.details = details; this.ipAddress = ipAddress; this.createdAt = LocalDateTime.now(); }
    public Long getId() { return id; } public Long getUserId() { return userId; } public String getUsername() { return username; } public String getAction() { return action; } public String getResource() { return resource; } public String getDetails() { return details; } public String getIpAddress() { return ipAddress; } public LocalDateTime getCreatedAt() { return createdAt; }
}
