package com.gestourant.backup;

import com.gestourant.audit.AuditService;
import com.gestourant.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatabaseBackupServiceTest {
    @Test
    void abortsRestoreWhenTheRequiredSafetyBackupCannotBeCreated() {
        ObjectProvider<S3Client> clients = mock(ObjectProvider.class);
        when(clients.getIfAvailable()).thenReturn(mock(S3Client.class));
        User user = mock(User.class);
        DatabaseBackupService service = spy(new DatabaseBackupService(
            clients,
            "backups",
            "gestourant/backups",
            "jdbc:mysql://localhost:3306/gestourant",
            "test-user",
            "test-password",
            "mysqldump-test",
            "mysql-test",
            mock(AuditService.class)
        ));
        doThrow(new ResponseStatusException(
            org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
            "No se pudo crear el respaldo previo."
        )).when(service).create(user);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> service.restore(UUID.randomUUID().toString(), user));

        assertEquals("No se pudo crear el respaldo previo.", exception.getReason());
        verify(service).create(user);
    }
}
