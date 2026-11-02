package com.gestourant.backup;

import com.gestourant.audit.AuditService;
import com.gestourant.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatabaseBackupServiceTest {
    @TempDir
    Path backupDirectory;

    @Test
    void listsAndDownloadsBackupsFromTheConfiguredLocalDirectory() throws Exception {
        String id = UUID.randomUUID().toString();
        byte[] contents = "CREATE DATABASE gestourant;".getBytes(StandardCharsets.UTF_8);
        Files.write(backupDirectory.resolve(id + ".sql"), contents);
        DatabaseBackupService service = localBackupService();

        List<DatabaseBackup> backups = service.list();
        assertEquals(1, backups.size());
        assertEquals(id, backups.get(0).id());
        assertEquals(contents.length, backups.get(0).sizeBytes());
        try (InputStream download = service.download(id)) {
            assertEquals("CREATE DATABASE gestourant;", new String(download.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void rejectsInvalidLocalBackupIds() {
        DatabaseBackupService service = localBackupService();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> service.download("../../outside"));

        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void abortsRestoreWhenTheRequiredSafetyBackupCannotBeCreated() {
        ObjectProvider<S3Client> clients = mock(ObjectProvider.class);
        when(clients.getIfAvailable()).thenReturn(mock(S3Client.class));
        User user = mock(User.class);
        DatabaseBackupService service = spy(new DatabaseBackupService(
            clients,
            "backups",
            "gestourant/backups",
            "",
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

    private DatabaseBackupService localBackupService() {
        ObjectProvider<S3Client> clients = mock(ObjectProvider.class);
        return new DatabaseBackupService(clients, "", "gestourant/backups", backupDirectory.toString(),
            "jdbc:mysql://localhost:3306/gestourant", "test-user", "test-password",
            "mysqldump-test", "mysql-test", mock(AuditService.class));
    }
}
