package com.gestourant.audit.adapter;

import com.gestourant.audit.AuditLog;
import com.gestourant.audit.AuditLogRepository;
import com.gestourant.audit.port.AuditQueryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class AuditLogQueryJpaAdapter implements AuditQueryPort {
    private final AuditLogRepository logs;

    public AuditLogQueryJpaAdapter(AuditLogRepository logs) {
        this.logs = logs;
    }

    @Override
    public Page<AuditLog> findLatest(Pageable pageable) {
        return logs.findAllByOrderByCreatedAtDesc(pageable);
    }
}
