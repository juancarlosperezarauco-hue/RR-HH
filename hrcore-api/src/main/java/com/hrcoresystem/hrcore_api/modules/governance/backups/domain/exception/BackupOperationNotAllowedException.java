package com.hrcoresystem.hrcore_api.modules.governance.backups.domain.exception;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;

public class BackupOperationNotAllowedException extends BusinessException {

    public BackupOperationNotAllowedException(String message) {
        super(message);
    }
}
