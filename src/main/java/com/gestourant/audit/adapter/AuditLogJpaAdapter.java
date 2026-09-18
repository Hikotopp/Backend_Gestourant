package com.gestourant.audit.adapter;

import com.gestourant.audit.AuditLog;
import com.gestourant.audit.AuditLogRepository;
import com.gestourant.audit.port.AuditPort;
import com.gestourant.user.User;
import org.springframework.stereotype.Component;

@Component
public class AuditLogJpaAdapter implements AuditPort {
    private final AuditLogRepository logs;

    public AuditLogJpaAdapter(AuditLogRepository logs) {
        this.logs = logs;
    }

    @Override
    public void record(User user, String action, String resource, String details, String ipAddress) {
        logs.save(new AuditLog(user.getId(), user.getUsername(), action, resource, details, ipAddress));
    }
}
