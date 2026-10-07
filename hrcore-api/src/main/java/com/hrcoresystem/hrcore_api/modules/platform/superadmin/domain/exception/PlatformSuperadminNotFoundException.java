package com.hrcoresystem.hrcore_api.modules.platform.superadmin.domain.exception;

import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;

public class PlatformSuperadminNotFoundException extends ResourceNotFoundException {

    public PlatformSuperadminNotFoundException(String message) {
        super(message);
    }
}