package com.hrcoresystem.hrcore_api.modules.governance.backups.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.governance.backups.domain.model.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "public", name = "backup_jobs")
public class BackupJobEntity {

    @Id
    @GeneratedValue
    private UUID id;
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 30)
    private BackupOperationType operationType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BackupScope scope;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BackupStatus status;
    @Column(name = "tenant_id")
    private UUID tenantId;
    @Column(name = "tenant_slug", length = 100)
    private String tenantSlug;
    @Column(name = "schema_name", length = 128)
    private String schemaName;
    @Column(name = "source_backup_id")
    private UUID sourceBackupId;
    @Column(name = "pre_restore_backup_id")
    private UUID preRestoreBackupId;
    @Column(name = "file_name", nullable = false)
    private String fileName;
    @Column(name = "storage_path", nullable = false, columnDefinition = "text")
    private String storagePath;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BackupFormat format;
    @Column(name = "size_bytes")
    private Long sizeBytes;
    @Column(name = "checksum_sha256", length = 128)
    private String checksumSha256;
    @Column(name = "requested_by", length = 150)
    private String requestedBy;
    @Column(columnDefinition = "text")
    private String reason;
    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;
    @Column(name = "started_at")
    private Instant startedAt;
    @Column(name = "finished_at")
    private Instant finishedAt;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (format == null) {
            format = BackupFormat.CUSTOM;
        
        }if (status == null) {
            status = BackupStatus.PENDING;
    
        }}

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public BackupOperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(BackupOperationType v) {
        operationType = v;
    }

    public BackupScope getScope() {
        return scope;
    }

    public void setScope(BackupScope v) {
        scope = v;
    }

    public BackupStatus getStatus() {
        return status;
    }

    public void setStatus(BackupStatus v) {
        status = v;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID v) {
        tenantId = v;
    }

    public String getTenantSlug() {
        return tenantSlug;
    }

    public void setTenantSlug(String v) {
        tenantSlug = v;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String v) {
        schemaName = v;
    }

    public UUID getSourceBackupId() {
        return sourceBackupId;
    }

    public void setSourceBackupId(UUID v) {
        sourceBackupId = v;
    }

    public UUID getPreRestoreBackupId() {
        return preRestoreBackupId;
    }

    public void setPreRestoreBackupId(UUID v) {
        preRestoreBackupId = v;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String v) {
        fileName = v;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String v) {
        storagePath = v;
    }

    public BackupFormat getFormat() {
        return format;
    }

    public void setFormat(BackupFormat v) {
        format = v;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long v) {
        sizeBytes = v;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public void setChecksumSha256(String v) {
        checksumSha256 = v;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String v) {
        requestedBy = v;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String v) {
        reason = v;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String v) {
        failureReason = v;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant v) {
        startedAt = v;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant v) {
        finishedAt = v;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant v) {
        createdAt = v;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant v) {
        updatedAt = v;
    }
}
