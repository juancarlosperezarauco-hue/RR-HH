package com.hrcoresystem.hrcore_api.modules.governance.backups.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.governance.backups.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.governance.backups.domain.repository.BackupJobRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class BackupJobRepositoryAdapter implements BackupJobRepository {

    private static final List<BackupStatus> ACTIVE = List.of(BackupStatus.PENDING, BackupStatus.RUNNING, BackupStatus.RESTORING);
    private final BackupJobJpaRepository jpa;

    public BackupJobRepositoryAdapter(BackupJobJpaRepository jpa) {
        this.jpa = jpa;
    }

    public BackupJob save(BackupJob b) {
        return toDomain(jpa.save(toEntity(b)));
    }

    public Optional<BackupJob> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    public List<BackupJob> findAll() {
        return jpa.findAllByOrderByCreatedAtDesc().stream().map(this::toDomain).toList();
    }

    public List<BackupJob> findByTenantSlug(String tenantSlug) {
        return jpa.findAllByTenantSlugOrderByCreatedAtDesc(tenantSlug).stream().map(this::toDomain).toList();
    }

    public boolean existsActiveJobForTenant(String tenantSlug) {
        return jpa.existsByTenantSlugAndStatusIn(tenantSlug, ACTIVE);
    }

    public boolean existsActiveFullDatabaseJob() {
        return jpa.existsByScopeAndStatusIn(BackupScope.FULL_DATABASE, ACTIVE);
    }

    private BackupJobEntity toEntity(BackupJob b) {
        BackupJobEntity e = new BackupJobEntity();
        e.setId(b.id());
        e.setOperationType(b.operationType());
        e.setScope(b.scope());
        e.setStatus(b.status());
        e.setTenantId(b.tenantId());
        e.setTenantSlug(b.tenantSlug());
        e.setSchemaName(b.schemaName());
        e.setSourceBackupId(b.sourceBackupId());
        e.setPreRestoreBackupId(b.preRestoreBackupId());
        e.setFileName(b.fileName());
        e.setStoragePath(b.storagePath());
        e.setFormat(b.format());
        e.setSizeBytes(b.sizeBytes());
        e.setChecksumSha256(b.checksumSha256());
        e.setRequestedBy(b.requestedBy());
        e.setReason(b.reason());
        e.setFailureReason(b.failureReason());
        e.setStartedAt(b.startedAt());
        e.setFinishedAt(b.finishedAt());
        e.setCreatedAt(b.createdAt());
        e.setUpdatedAt(b.updatedAt());
        return e;
    }

    private BackupJob toDomain(BackupJobEntity e) {
        return new BackupJob(e.getId(), e.getOperationType(), e.getScope(), e.getStatus(), e.getTenantId(), e.getTenantSlug(), e.getSchemaName(), e.getSourceBackupId(), e.getPreRestoreBackupId(), e.getFileName(), e.getStoragePath(), e.getFormat(), e.getSizeBytes(), e.getChecksumSha256(), e.getRequestedBy(), e.getReason(), e.getFailureReason(), e.getStartedAt(), e.getFinishedAt(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
