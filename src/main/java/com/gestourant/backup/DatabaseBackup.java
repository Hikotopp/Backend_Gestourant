package com.gestourant.backup;

import java.time.Instant;

public record DatabaseBackup(String id, String fileName, Instant createdAt, long sizeBytes) { }
