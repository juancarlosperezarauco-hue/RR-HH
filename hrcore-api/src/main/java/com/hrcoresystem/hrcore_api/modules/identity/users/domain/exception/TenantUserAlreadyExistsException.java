package com.hrcoresystem.hrcore_api.modules.identity.users.domain.exception;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;

public class TenantUserAlreadyExistsException extends BusinessException {

    public TenantUserAlreadyExistsException(String message) {
        super(message);
    }
}