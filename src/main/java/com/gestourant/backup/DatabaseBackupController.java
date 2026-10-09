package com.gestourant.backup;

import com.gestourant.user.User;
import com.gestourant.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/admin/backups")
public class DatabaseBackupController {
    private final DatabaseBackupService backups;
    private final UserRepository users;

    public DatabaseBackupController(DatabaseBackupService backups, UserRepository users) {
        this.backups = backups;
        this.users = users;
    }

    @GetMapping
    public List<DatabaseBackup> list() {
        return backups.list();
    }

    @PostMapping
    public DatabaseBackup create(@AuthenticationPrincipal UserDetails principal) {
        return backups.create(currentUser(principal));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<StreamingResponseBody> download(@PathVariable String id) {
        DatabaseBackup metadata = backups.list().stream().filter(backup -> backup.id().equals(id)).findFirst()
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "No se encontró el respaldo solicitado."));
        StreamingResponseBody body = output -> {
            try (InputStream input = backups.download(id)) {
                input.transferTo(output);
            } catch (IOException ex) {
                throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible descargar el respaldo.", ex);
            }
        };
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/sql"))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(metadata.fileName(), StandardCharsets.UTF_8).build().toString())
            .body(body);
    }

    @PostMapping("/{id}/restore")
    public void restore(@PathVariable String id, @Valid @RequestBody RestoreRequest request,
                        @AuthenticationPrincipal UserDetails principal) {
        if (!"RESTAURAR".equals(request.confirmation())) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "Escribe RESTAURAR para confirmar la operación.");
        }
        backups.restore(id, currentUser(principal));
    }

    private User currentUser(UserDetails principal) {
        return users.findByEmailIgnoreCase(principal.getUsername())
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "La cuenta ya no existe."));
    }

    public record RestoreRequest(@NotBlank String confirmation) { }
}
