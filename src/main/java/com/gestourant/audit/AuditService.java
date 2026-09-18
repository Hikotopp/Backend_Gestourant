package com.gestourant.audit;

import com.gestourant.user.User;
import com.gestourant.audit.port.AuditPort;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditPort auditPort;
    public AuditService(AuditPort auditPort) { this.auditPort = auditPort; }
    public void log(User user, String action, String resource, String details, String ip) { auditPort.record(user, action, resource, details, ip); }
}
