package com.hrcoresystem.hrcore_api.modules.governance.backups.infrastructure.engine;

import com.hrcoresystem.hrcore_api.modules.governance.backups.domain.model.BackupArtifact;
import java.nio.file.Path;
import com.hrcoresystem.hrcore_api.modules.governance.backups.domain.model.BackupScope;

public interface BackupEngine {

    BackupArtifact dumpSchema(String schemaName, Path target);

    BackupArtifact dumpFullDatabase(Path target);

    void validateArchive(Path source, BackupScope scope, String schemaName);

    String restoreSchema(String schemaName, Path source);

    String restoreFullDatabase(Path source);
}
