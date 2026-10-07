package com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.exception;

import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;

public class PlatformTenantNotFoundException extends ResourceNotFoundException {

    public PlatformTenantNotFoundException(String message) {
        super(message);
    }
}