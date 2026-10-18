package com.hrcoresystem.hrcore_api.common.exception;
public class InvalidBusinessStateException extends RuntimeException {
    public InvalidBusinessStateException(String message) { super(message); }
    public InvalidBusinessStateException(String message, Throwable cause) { super(message, cause); }
}
