package com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.exception;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;

public class PlatformTenantAlreadyExistsException extends BusinessException {

    public PlatformTenantAlreadyExistsException(String message) {
        super(message);
    }
}