package com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.exception;

import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;

public class PlatformSubscriptionNotFoundException extends ResourceNotFoundException {

    public PlatformSubscriptionNotFoundException(String message) {
        super(message);
    }
}
