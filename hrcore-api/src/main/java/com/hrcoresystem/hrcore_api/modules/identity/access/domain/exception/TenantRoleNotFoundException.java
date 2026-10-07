package com.hrcoresystem.hrcore_api.modules.identity.access.domain.exception;

import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;

public class TenantRoleNotFoundException extends ResourceNotFoundException {

    public TenantRoleNotFoundException(String message) {
        super(message);
    }
}