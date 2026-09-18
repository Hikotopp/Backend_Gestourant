package com.gestourant.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.gestourant.audit.port.AuditQueryPort;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditController {
    private final AuditQueryPort logs;
    public AuditController(AuditQueryPort logs) { this.logs = logs; }
    @GetMapping
    public Page<AuditLog> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return logs.findLatest(pageable);
    }
}
