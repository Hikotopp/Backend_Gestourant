package com.gestourant.audit.port;

import com.gestourant.user.User;

public interface AuditPort {
    void record(User user, String action, String resource, String details, String ipAddress);
}
