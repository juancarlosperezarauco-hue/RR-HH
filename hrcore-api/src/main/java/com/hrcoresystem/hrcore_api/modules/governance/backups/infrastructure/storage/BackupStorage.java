package com.hrcoresystem.hrcore_api.modules.governance.backups.infrastructure.storage;

import org.springframework.core.io.Resource;
import java.nio.file.Path;

public interface BackupStorage {

    Path resolveTargetPath(String fileName);

    Path resolvePath(String storagePath);

    Resource loadAsResource(String storagePath);

    String toStoragePath(Path path);
}
