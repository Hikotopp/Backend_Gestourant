package com.gestourant.audit.port;

import com.gestourant.audit.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditQueryPort {
    Page<AuditLog> findLatest(Pageable pageable);
}
