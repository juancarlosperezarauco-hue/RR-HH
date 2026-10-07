package com.hrcoresystem.hrcore_api.modules.identity.access.domain.exception;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;

public class InvalidSystemPermissionException extends BusinessException {

    public InvalidSystemPermissionException(String message) {
        super(message);
    }
}