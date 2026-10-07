package com.hrcoresystem.hrcore_api.modules.governance.backups.application.service;

public final class BackupAuditEventTypes {

    public static final String BACKUP_CREATED = "BACKUP_CREATED";
    public static final String BACKUP_COMPLETED = "BACKUP_COMPLETED";
    public static final String BACKUP_FAILED = "BACKUP_FAILED";
    public static final String BACKUP_DOWNLOADED = "BACKUP_DOWNLOADED";
    public static final String RESTORE_STARTED = "RESTORE_STARTED";
    public static final String RESTORE_COMPLETED = "RESTORE_COMPLETED";
    public static final String RESTORE_FAILED = "RESTORE_FAILED";

    private BackupAuditEventTypes() {
    }
}
