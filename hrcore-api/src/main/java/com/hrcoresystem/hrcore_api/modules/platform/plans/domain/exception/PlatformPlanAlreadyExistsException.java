package com.hrcoresystem.hrcore_api.modules.platform.plans.domain.exception;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;

public class PlatformPlanAlreadyExistsException extends BusinessException {

    public PlatformPlanAlreadyExistsException(String message) {
        super(message);
    }
}