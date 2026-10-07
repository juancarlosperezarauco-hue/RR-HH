package com.hrcoresystem.hrcore_api.modules.identity.users.domain.exception;

import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;

public class TenantUserNotFoundException extends ResourceNotFoundException {

    public TenantUserNotFoundException(String message) {
        super(message);
    }
}