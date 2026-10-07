package com.hrcoresystem.hrcore_api.modules.governance.backups.domain.model;

import java.nio.file.Path;

public record BackupArtifact(
        Path path,
        long sizeBytes,
        String checksumSha256
        ) {

}
