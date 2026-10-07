package com.hrcoresystem.hrcore_api.modules.identity.auth.domain.exception;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;

public class InvalidPasswordResetTokenException extends BusinessException {

    public InvalidPasswordResetTokenException(String message) {
        super(message);
    }
}