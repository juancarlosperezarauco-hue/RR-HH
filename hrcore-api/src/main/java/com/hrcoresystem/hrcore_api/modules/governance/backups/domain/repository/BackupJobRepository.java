package com.hrcoresystem.hrcore_api.modules.governance.backups.domain.repository;

import com.hrcoresystem.hrcore_api.modules.governance.backups.domain.model.BackupJob;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BackupJobRepository {

    BackupJob save(BackupJob backupJob);

    Optional<BackupJob> findById(UUID id);

    List<BackupJob> findAll();

    List<BackupJob> findByTenantSlug(String tenantSlug);

    boolean existsActiveJobForTenant(String tenantSlug);

    boolean existsActiveFullDatabaseJob();
}
