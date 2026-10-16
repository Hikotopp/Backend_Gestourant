package com.gestourant.audit;

import com.gestourant.audit.port.AuditQueryPort;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditControllerTest {
    private final AuditQueryPort logs = mock(AuditQueryPort.class);
    private final AuditController controller = new AuditController(logs);

    @Test
    void clampsPageAndSizeToSafeBounds() {
        when(logs.findLatest(PageRequest.of(0, 100))).thenReturn(new PageImpl<>(List.of()));

        assertEquals(0, controller.list(-5, 500).getTotalElements());

        verify(logs).findLatest(PageRequest.of(0, 100));
    }

    @Test
    void enforcesMinimumPageSize() {
        when(logs.findLatest(PageRequest.of(2, 1))).thenReturn(new PageImpl<>(List.of()));

        assertEquals(0, controller.list(2, 0).getTotalElements());

        verify(logs).findLatest(PageRequest.of(2, 1));
    }
}
