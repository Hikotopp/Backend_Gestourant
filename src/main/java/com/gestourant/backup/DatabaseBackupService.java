package com.gestourant.backup;

import com.gestourant.audit.AuditService;
import com.gestourant.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import com.gestourant.time.ColombiaTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class DatabaseBackupService {
    private static final Logger log = LoggerFactory.getLogger(DatabaseBackupService.class);
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")
        .withZone(ColombiaTime.ZONE);
    private final ObjectProvider<S3Client> s3Clients;
    private final String bucket;
    private final String prefix;
    private final String localDirectory;
    private final String datasourceUrl;
    private final String datasourceUsername;
    private final String datasourcePassword;
    private final String dumpCommand;
    private final String mysqlCommand;
    private final AuditService audit;

    public DatabaseBackupService(ObjectProvider<S3Client> s3Clients,
                                 @Value("${app.backup.s3.bucket:}") String bucket,
                                 @Value("${app.backup.s3.prefix:gestourant/backups}") String prefix,
                                 @Value("${app.backup.local.directory:}") String localDirectory,
                                 @Value("${spring.datasource.url}") String datasourceUrl,
                                 @Value("${spring.datasource.username}") String datasourceUsername,
                                 @Value("${spring.datasource.password:}") String datasourcePassword,
                                 @Value("${app.backup.mysqldump-command:mysqldump}") String dumpCommand,
                                 @Value("${app.backup.mysql-command:mysql}") String mysqlCommand,
                                 AuditService audit) {
        this.s3Clients = s3Clients;
        this.bucket = bucket;
        this.prefix = prefix.replaceAll("^/+|/+$", "");
        this.localDirectory = localDirectory;
        this.datasourceUrl = datasourceUrl;
        this.datasourceUsername = datasourceUsername;
        this.datasourcePassword = datasourcePassword;
        this.dumpCommand = dumpCommand;
        this.mysqlCommand = mysqlCommand;
        this.audit = audit;
    }

    public List<DatabaseBackup> list() {
        List<DatabaseBackup> backups = new ArrayList<>();
        S3Client s3 = configuredS3Client();
        if (s3 != null) {
            s3.listObjectsV2Paginator(ListObjectsV2Request.builder().bucket(bucket).prefix(prefix + "/").build())
                .contents().forEach(object -> {
                    String id = idFromKey(object.key());
                    if (id != null) {
                        backups.add(new DatabaseBackup(id,
                            "gestourant-backup-" + FILE_TIMESTAMP.format(object.lastModified()) + ".sql",
                            object.lastModified(), object.size()));
                    }
                });
        } else {
            Path directory = requireLocalDirectory();
            try (var files = Files.list(directory)) {
                files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().matches("[0-9a-fA-F-]{36}\\.sql"))
                    .forEach(path -> {
                        try {
                            String id = path.getFileName().toString().replaceFirst("\\.sql$", "");
                            Instant createdAt = Files.getLastModifiedTime(path).toInstant();
                            backups.add(new DatabaseBackup(id,
                                "gestourant-backup-" + FILE_TIMESTAMP.format(createdAt) + ".sql",
                                createdAt, Files.size(path)));
                        } catch (IOException ex) {
                            throw new UncheckedIOException(ex);
                        }
                    });
            } catch (UncheckedIOException ex) {
                throw backupStorageFailure(ex.getCause());
            } catch (IOException ex) {
                throw backupStorageFailure(ex);
            }
        }
        backups.sort(Comparator.comparing(DatabaseBackup::createdAt).reversed());
        return backups;
    }

    public DatabaseBackup create(User user) {
        S3Client s3 = configuredS3Client();
        Path directory = s3 == null ? requireLocalDirectory() : null;
        Path dump = null;
        Path errors = null;
        try {
            dump = Files.createTempFile("gestourant-backup-", ".sql");
            errors = Files.createTempFile("gestourant-backup-", ".err");
            ProcessBuilder process = mysqlProcess(dumpCommand, true);
            process.redirectOutput(dump.toFile());
            process.redirectError(errors.toFile());
            requireSuccessfulProcess(process, "No se pudo generar el respaldo de MySQL.");

            Instant createdAt = Instant.now();
            String id = UUID.randomUUID().toString();
            if (s3 != null) {
                s3.putObject(PutObjectRequest.builder().bucket(bucket).key(objectKey(id)).contentType("application/sql")
                    .metadata(java.util.Map.of("created-by", user.getUsername(), "created-at", createdAt.toString()))
                    .build(), RequestBody.fromFile(dump));
            } else {
                storeLocalBackup(directory, id, dump);
            }
            audit.log(user, "CREATE", "database-backup", "Respaldo creado: " + id, null);
            return new DatabaseBackup(id,
                "gestourant-backup-" + FILE_TIMESTAMP.format(createdAt) + ".sql", createdAt, Files.size(dump));
        } catch (IOException ex) {
            log.error("Database backup operation failed.", ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "No fue posible crear el respaldo. Revisa la configuración de MySQL y del almacenamiento.", ex);
        } finally {
            deleteTemporaryFile(dump);
            deleteTemporaryFile(errors);
        }
    }

    public InputStream download(String id) {
        String backupId = validateId(id);
        S3Client s3 = configuredS3Client();
        if (s3 != null) {
            return s3.getObject(GetObjectRequest.builder().bucket(bucket).key(objectKey(backupId)).build());
        }
        try {
            Path backup = localBackupPath(requireLocalDirectory(), backupId);
            if (!Files.isRegularFile(backup)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el respaldo solicitado.");
            }
            return Files.newInputStream(backup);
        } catch (IOException ex) {
            throw backupStorageFailure(ex);
        }
    }

    public RestoreResult restore(String id, User user) {
        String backupId = validateId(id);
        S3Client s3 = configuredS3Client();
        Path directory = s3 == null ? requireLocalDirectory() : null;
        Path dump = null;
        Path errors = null;
        try {
            dump = Files.createTempFile("gestourant-restore-", ".sql");
            errors = Files.createTempFile("gestourant-restore-", ".err");
            if (s3 != null) {
                s3.getObject(GetObjectRequest.builder().bucket(bucket).key(objectKey(backupId)).build(),
                    ResponseTransformer.toFile(dump));
            } else {
                Path source = localBackupPath(directory, backupId);
                if (!Files.isRegularFile(source)) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el respaldo solicitado.");
                }
                Files.copy(source, dump, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            DatabaseBackup safetyBackup = create(user);
            audit.log(user, "SAFETY_BACKUP", "database-backup",
                "Respaldo previo obligatorio antes de restaurar " + backupId + ": " + safetyBackup.id(), null);
            ProcessBuilder process = mysqlProcess(mysqlCommand, false);
            process.redirectInput(dump.toFile());
            process.redirectError(errors.toFile());
            requireSuccessfulProcess(process, "No se pudo restaurar el respaldo de MySQL.");
            audit.log(user, "RESTORE", "database-backup",
                "Base de datos restaurada desde: " + backupId + "; respaldo previo: " + safetyBackup.id(), null);
            return new RestoreResult(backupId, safetyBackup.id(), safetyBackup.fileName(), safetyBackup.createdAt());
        } catch (IOException ex) {
            log.error("Database restore operation failed.", ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "No fue posible restaurar el respaldo. Revisa la configuración de MySQL y del almacenamiento.", ex);
        } finally {
            deleteTemporaryFile(dump);
            deleteTemporaryFile(errors);
        }
    }

    private ProcessBuilder mysqlProcess(String command, boolean dump) {
        DatabaseTarget target = databaseTarget();
        List<String> args = new ArrayList<>(List.of(command, "--host=" + target.host(),
            "--port=" + target.port(), "--user=" + datasourceUsername, "--default-character-set=utf8mb4"));
        if (dump) {
            args.addAll(List.of("--single-transaction", "--routines", "--triggers", "--events", "--databases", target.database()));
        } else {
            args.add("--database=" + target.database());
            args.add("--binary-mode=1");
        }
        ProcessBuilder builder = new ProcessBuilder(args);
        builder.environment().put("MYSQL_PWD", datasourcePassword);
        return builder;
    }

    private DatabaseTarget databaseTarget() {
        try {
            String url = datasourceUrl.startsWith("jdbc:") ? datasourceUrl.substring(5) : datasourceUrl;
            URI uri = URI.create(url);
            String database = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "").split("/", 2)[0];
            if (!"mysql".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || database.isBlank()) {
                throw new IllegalArgumentException("Expected a jdbc:mysql URL with a database name.");
            }
            return new DatabaseTarget(uri.getHost(), uri.getPort() < 0 ? 3306 : uri.getPort(), database);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "La configuración de conexión a MySQL no es válida para respaldos.", ex);
        }
    }

    private void requireSuccessfulProcess(ProcessBuilder builder, String failureMessage) throws IOException {
        Process process;
        try {
            process = builder.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("MySQL backup command failed with exit code {}.", exitCode);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, failureMessage);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "La operación de respaldo fue interrumpida.", ex);
        }
    }

    private S3Client configuredS3Client() {
        if (bucket.isBlank()) return null;
        S3Client client = s3Clients.getIfAvailable();
        if (client == null) throw new BackupStorageUnavailableException();
        return client;
    }

    private Path requireLocalDirectory() {
        if (localDirectory.isBlank()) throw new BackupStorageUnavailableException();
        Path directory = Path.of(localDirectory).toAbsolutePath().normalize();
        try {
            Files.createDirectories(directory);
            return directory;
        } catch (IOException ex) {
            log.error("Unable to create local database backup directory.", ex);
            throw backupStorageFailure(ex);
        }
    }

    private void storeLocalBackup(Path directory, String id, Path dump) throws IOException {
        Path target = localBackupPath(directory, id);
        Path temporary = Files.createTempFile(directory, ".backup-", ".tmp");
        try {
            Files.copy(dump, temporary, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            try {
                Files.move(temporary, target, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                Files.move(temporary, target);
            }
        } finally {
            deleteTemporaryFile(temporary);
        }
    }

    private Path localBackupPath(Path directory, String id) {
        Path backup = directory.resolve(validateId(id) + ".sql").normalize();
        if (!backup.getParent().equals(directory)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el respaldo solicitado.");
        }
        return backup;
    }

    private ResponseStatusException backupStorageFailure(IOException cause) {
        log.error("Local database backup storage operation failed.", cause);
        return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
            "No fue posible acceder al almacenamiento local de respaldos.", cause);
    }

    private String objectKey(String id) {
        return prefix + "/" + id + ".sql";
    }

    private String idFromKey(String key) {
        String fileName = key.substring(key.lastIndexOf('/') + 1);
        if (!fileName.matches("[0-9a-fA-F-]{36}\\.sql")) return null;
        return fileName.substring(0, fileName.length() - 4);
    }

    private String validateId(String id) {
        try {
            return UUID.fromString(id).toString();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el respaldo solicitado.");
        }
    }

    private void deleteTemporaryFile(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            log.warn("Unable to remove temporary database backup file.");
        }
    }

    private record DatabaseTarget(String host, int port, String database) { }

    public record RestoreResult(String restoredBackupId, String safetyBackupId,
                                String safetyBackupFileName, Instant safetyBackupCreatedAt) { }
}
