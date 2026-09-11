package com.gestourant.audit;

import com.gestourant.user.User;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditLogRepository logs;
    public AuditService(AuditLogRepository logs) { this.logs = logs; }
    public void log(User user, String action, String resource, String details, String ip) { logs.save(new AuditLog(user.getId(), user.getUsername(), action, resource, details, ip)); }
}
