package com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.exception;

import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;

public class PlatformPlanNotFoundException extends ResourceNotFoundException {

    public PlatformPlanNotFoundException(String message) {
        super(message);
    }
}