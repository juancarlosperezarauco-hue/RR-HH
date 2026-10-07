package com.hrcoresystem.hrcore_api.modules.governance.backups.domain.exception;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;

public class BackupExecutionException extends BusinessException {

    public BackupExecutionException(String message) {
        super(message);
    }

    public BackupExecutionException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
