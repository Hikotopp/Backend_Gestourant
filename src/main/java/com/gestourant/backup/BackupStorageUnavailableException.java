package com.gestourant.backup;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class BackupStorageUnavailableException extends ResponseStatusException {
    public BackupStorageUnavailableException() {
        super(HttpStatus.SERVICE_UNAVAILABLE,
            "El almacenamiento de respaldos no está configurado. Configura una carpeta local o un bucket S3.");
    }
}
