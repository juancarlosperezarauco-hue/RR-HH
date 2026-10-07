package com.hrcoresystem.hrcore_api.modules.identity.access.domain.exception;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;

public class TenantRoleAlreadyExistsException extends BusinessException {

    public TenantRoleAlreadyExistsException(String message) {
        super(message);
    }
}