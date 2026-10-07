package com.hrcoresystem.hrcore_api.modules.identity.auth.domain.exception;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;

public class InvalidAccountActivationTokenException extends BusinessException {

    public InvalidAccountActivationTokenException(String message) {
        super(message);
    }
}
