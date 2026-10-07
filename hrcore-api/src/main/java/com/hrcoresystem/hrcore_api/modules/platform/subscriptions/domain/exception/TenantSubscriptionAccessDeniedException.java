package com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.exception;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;

public class TenantSubscriptionAccessDeniedException extends BusinessException {

    public TenantSubscriptionAccessDeniedException(String message) {
        super(message);
    }
}
