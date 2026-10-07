package com.hrcoresystem.hrcore_api.modules.governance.backups.infrastructure.storage;

import com.hrcoresystem.hrcore_api.modules.governance.backups.application.config.BackupProperties;
import com.hrcoresystem.hrcore_api.modules.governance.backups.domain.exception.BackupNotFoundException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class LocalBackupStorageAdapter implements BackupStorage {

    private final Path rootPath;

    public LocalBackupStorageAdapter(BackupProperties properties) {
        this.rootPath = Path.of(properties.getLocalPath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootPath);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create backup directory", e);
        }
    }

    public Path resolveTargetPath(String fileName) {
        validateFileName(fileName);
        return rootPath.resolve(fileName).normalize();
    }

    public Path resolvePath(String storagePath) {
        Path p = rootPath.resolve(storagePath).normalize();
        if (!p.startsWith(rootPath)) {
            throw new BackupNotFoundException("Invalid backup storage path");
        }
        return p;
    }

    public Resource loadAsResource(String storagePath) {
        Path p = resolvePath(storagePath);
        if (!Files.exists(p)) {
            throw new BackupNotFoundException("Backup artifact not found");
        }
        return new FileSystemResource(p);
    }

    public String toStoragePath(Path path) {
        Path n = path.toAbsolutePath().normalize();
        if (!n.startsWith(rootPath)) {
            throw new IllegalArgumentException("Path is outside backup root");
        }
        return rootPath.relativize(n).toString().replace("\\", "/");
    }

    private void validateFileName(String fileName) {
        if (fileName == null || fileName.isBlank() || !fileName.matches("^[a-zA-Z0-9_.\\-]+$")) {
            throw new IllegalArgumentException("Invalid backup file name: " + fileName);
        }
    }
}
