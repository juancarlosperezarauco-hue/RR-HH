package com.hrcoresystem.hrcore_api.modules.governance.backups.application.service;

import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContext;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.backups.domain.exception.*;
import com.hrcoresystem.hrcore_api.modules.governance.backups.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.governance.backups.domain.repository.BackupJobRepository;
import com.hrcoresystem.hrcore_api.modules.governance.backups.infrastructure.storage.BackupStorage;
import com.hrcoresystem.hrcore_api.modules.governance.backups.infrastructure.storage.ChecksumService;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.exception.PlatformTenantNotFoundException;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BackupApplicationService {

    private final BackupJobRepository repo;
    private final PlatformTenantRepository tenants;
    private final SecurityContextFacade security;
    private final BackupJobAsyncExecutor async;
    private final AuditTrailService audit;
    private final BackupStorage storage;
    private final ChecksumService checksumService;

    public BackupApplicationService(
            BackupJobRepository repo,
            PlatformTenantRepository tenants,
            SecurityContextFacade security,
            BackupJobAsyncExecutor async,
            AuditTrailService audit,
            BackupStorage storage,
            ChecksumService checksumService
    ) {
        this.repo = repo;
        this.tenants = tenants;
        this.security = security;
        this.async = async;
        this.audit = audit;
        this.storage = storage;
        this.checksumService = checksumService;
    }

    @Transactional
    public BackupJob createTenantBackup(String reason) {
        TenantContext ctx = TenantContextHolder.getRequired();
        PlatformTenant t = tenants.findBySlug(ctx.tenantSlug()).orElseThrow(() -> new PlatformTenantNotFoundException("Tenant not found: " + ctx.tenantSlug()));
        BackupJob created = save(BackupJobFactory.tenantBackup(t.id(), t.slug(), t.schemaName(), actor(), reason));
        audit.recordTenantEvent(BackupAuditEventTypes.BACKUP_CREATED, "BACKUP", created.id().toString(), Map.of("tenantSlug", t.slug(), "schemaName", t.schemaName()));
        executeAfterCommit(() -> async.executeBackup(created.id()));
        return created;
    }

    @Transactional
    public BackupJob createPlatformFullBackup(String reason) {
        BackupJob created = save(BackupJobFactory.fullBackup(actor(), reason));
        audit.recordPlatformEvent(BackupAuditEventTypes.BACKUP_CREATED, "BACKUP", created.id().toString(), Map.of("scope", BackupScope.FULL_DATABASE.name()));
        executeAfterCommit(() -> async.executeBackup(created.id()));
        return created;
    }

    @Transactional
    public BackupJob createPlatformTenantBackup(UUID tenantId, String reason) {
        PlatformTenant t = tenants.findById(tenantId).orElseThrow(() -> new PlatformTenantNotFoundException("Tenant not found with id: " + tenantId));
        BackupJob created = save(BackupJobFactory.tenantBackup(t.id(), t.slug(), t.schemaName(), actor(), reason));
        audit.recordPlatformEvent(BackupAuditEventTypes.BACKUP_CREATED, "BACKUP", created.id().toString(), Map.of("tenantId", t.id().toString(), "tenantSlug", t.slug(), "schemaName", t.schemaName()));
        executeAfterCommit(() -> async.executeBackup(created.id()));
        return created;
    }

    @Transactional
    public BackupJob createTenantRestoreJob(UUID id, String reason) {
        TenantContext ctx = TenantContextHolder.getRequired();
        BackupJob src = repo.findById(id).orElseThrow(() -> new BackupNotFoundException("Backup not found with id: " + id));
        assertRestorable(src);
        if (src.tenantSlug() == null || !src.tenantSlug().equals(ctx.tenantSlug())) {
            throw new BackupOperationNotAllowedException("You can only restore backups from your own tenant");
        }
        BackupJob created = save(BackupJobFactory.restoreJobFromBackup(src, null, actor(), reason));
        executeAfterCommit(() -> async.executeRestore(created.id()));
        return created;
    }

    @Transactional
    public BackupJob createPlatformRestoreJob(UUID id, String reason) {
        BackupJob src = repo.findById(id).orElseThrow(() -> new BackupNotFoundException("Backup not found with id: " + id));
        assertRestorable(src);
        BackupJob created = save(BackupJobFactory.restoreJobFromBackup(src, null, actor(), reason));
        executeAfterCommit(() -> async.executeRestore(created.id()));
        return created;
    }

    @Transactional
    public BackupJob createTenantRestoreJobFromFile(MultipartFile file, String reason) {
        TenantContext ctx = TenantContextHolder.getRequired();
        PlatformTenant tenant = tenants.findBySlug(ctx.tenantSlug())
                .orElseThrow(() -> new PlatformTenantNotFoundException("Tenant not found: " + ctx.tenantSlug()));
        return createUploadedRestoreJob(
                file,
                BackupScope.TENANT_SCHEMA,
                tenant.id(),
                tenant.slug(),
                tenant.schemaName(),
                reason
        );
    }

    @Transactional
    public BackupJob createPlatformRestoreJobFromFile(BackupScope scope, UUID tenantId, MultipartFile file, String reason) {
        if (scope == BackupScope.TENANT_SCHEMA) {
            if (tenantId == null) {
                throw new BackupOperationNotAllowedException("tenantId is required for tenant schema restore");
            }

            PlatformTenant tenant = tenants.findById(tenantId)
                    .orElseThrow(() -> new PlatformTenantNotFoundException("Tenant not found with id: " + tenantId));

            return createUploadedRestoreJob(file, scope, tenant.id(), tenant.slug(), tenant.schemaName(), reason);
        }

        return createUploadedRestoreJob(file, scope, null, null, null, reason);
    }

    public List<BackupJob> listTenantBackups() {
        return repo.findByTenantSlug(TenantContextHolder.getRequired().tenantSlug());
    }

    public List<BackupJob> listPlatformBackups() {
        return repo.findAll();
    }

    public BackupJob getTenantBackupById(UUID id) {
        TenantContext ctx = TenantContextHolder.getRequired();
        BackupJob b = repo.findById(id).orElseThrow(() -> new BackupNotFoundException("Backup not found with id: " + id));
        if (b.tenantSlug() == null || !b.tenantSlug().equals(ctx.tenantSlug())) {
            throw new BackupNotFoundException("Backup not found with id: " + id);
        }
        return b;
    }

    public BackupJob getPlatformBackupById(UUID id) {
        return repo.findById(id).orElseThrow(() -> new BackupNotFoundException("Backup not found with id: " + id));
    }

    public BackupJob registerTenantDownload(UUID id) {
        BackupJob b = getTenantBackupById(id);
        audit.recordTenantEvent(BackupAuditEventTypes.BACKUP_DOWNLOADED, "BACKUP", b.id().toString(), Map.of("fileName", b.fileName(), "tenantSlug", b.tenantSlug()));
        return b;
    }

    public BackupJob registerPlatformDownload(UUID id) {
        BackupJob b = getPlatformBackupById(id);
        audit.recordPlatformEvent(BackupAuditEventTypes.BACKUP_DOWNLOADED, "BACKUP", b.id().toString(), Map.of("fileName", b.fileName(), "scope", b.scope().name()));
        return b;
    }

    private BackupJob save(BackupJob j) {
        try {
            return repo.save(j);
        } catch (DataIntegrityViolationException e) {
            throw new BackupOperationNotAllowedException("There is already a backup or restore job running for this scope. Try again later.");
        }
    }

    private void assertRestorable(BackupJob s) {
        if (s.operationType() != BackupOperationType.BACKUP || s.status() != BackupStatus.COMPLETED) {
            throw new BackupOperationNotAllowedException("Only completed backup jobs can be restored");
        }
        if (s.storagePath() == null || s.storagePath().isBlank()) {
            throw new BackupOperationNotAllowedException("Backup artifact path is missing");
        }
    }

    private void executeAfterCommit(Runnable task) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            task.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                task.run();
            }
        });
    }

    private String actor() {
        String s = security.getCurrentSubject();
        return s == null || s.isBlank() ? "SYSTEM" : s;
    }

    private BackupJob createUploadedRestoreJob(
            MultipartFile file,
            BackupScope scope,
            UUID tenantId,
            String tenantSlug,
            String schemaName,
            String reason
    ) {
        if (file == null || file.isEmpty()) {
            throw new BackupOperationNotAllowedException("Backup file is required");
        }

        String originalName = safeFileName(file.getOriginalFilename());
        String storageFileName = "restore_upload_" + System.currentTimeMillis() + "_" + originalName;
        Path target = storage.resolveTargetPath(storageFileName);

        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new BackupOperationNotAllowedException("Unable to store uploaded backup file: " + e.getMessage());
        }

        try {
            BackupJob created = save(BackupJobFactory.restoreJobFromUploadedFile(
                    scope,
                    tenantId,
                    tenantSlug,
                    schemaName,
                    originalName,
                    storage.toStoragePath(target),
                    BackupFormat.CUSTOM,
                    actor(),
                    reason
            ));
            BackupJob withMetadata = repo.save(new BackupJob(
                    created.id(),
                    created.operationType(),
                    created.scope(),
                    created.status(),
                    created.tenantId(),
                    created.tenantSlug(),
                    created.schemaName(),
                    created.sourceBackupId(),
                    created.preRestoreBackupId(),
                    created.fileName(),
                    created.storagePath(),
                    created.format(),
                    safeSize(target),
                    safeChecksum(target),
                    created.requestedBy(),
                    created.reason(),
                    created.failureReason(),
                    created.startedAt(),
                    created.finishedAt(),
                    created.createdAt(),
                    created.updatedAt()
            ));

            executeAfterCommit(() -> async.executeRestore(withMetadata.id()));
            return withMetadata;
        } catch (RuntimeException e) {
            try {
                Files.deleteIfExists(target);
            } catch (Exception ignored) {
                // Best effort cleanup.
            }
            throw e;
        }
    }

    private String safeFileName(String originalName) {
        String name = originalName == null ? "" : originalName.trim();
        if (name.isBlank()) {
            return "backup_upload.dump";
        }

        String sanitized = name.replaceAll("[^a-zA-Z0-9_.-]", "_");
        return sanitized.isBlank() ? "backup_upload.dump" : sanitized;
    }

    private Long safeSize(Path target) {
        try {
            return Files.size(target);
        } catch (Exception e) {
            return null;
        }
    }

    private String safeChecksum(Path target) {
        try {
            return checksumService.sha256(target);
        } catch (Exception e) {
            return null;
        }
    }
}
