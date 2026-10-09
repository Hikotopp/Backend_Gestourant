package com.gestourant.backup;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class BackupStorageUnavailableException extends ResponseStatusException {
    public BackupStorageUnavailableException() {
        super(HttpStatus.SERVICE_UNAVAILABLE,
            "El almacenamiento de backups no está configurado. Configura el bucket S3 antes de usar esta función.");
    }
}
