package com.hrcoresystem.hrcore_api.modules.governance.backups.domain.exception;

import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;

public class BackupNotFoundException extends ResourceNotFoundException {

    public BackupNotFoundException(String message) {
        super(message);
    }
}
