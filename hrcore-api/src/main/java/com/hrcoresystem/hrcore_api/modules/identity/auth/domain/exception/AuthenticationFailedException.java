package com.hrcoresystem.hrcore_api.modules.identity.auth.domain.exception;

public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException(String message) {
        super(message);
    }
}