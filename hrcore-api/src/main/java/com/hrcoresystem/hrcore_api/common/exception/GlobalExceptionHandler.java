package com.hrcoresystem.hrcore_api.common.exception;

import com.hrcoresystem.hrcore_api.common.response.ApiErrorResponse;
import com.hrcoresystem.hrcore_api.common.tenancy.exception.TenantResolutionException;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.exception.FaceRecognitionUnavailableException;
import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.exception.AuthenticationFailedException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(ResourceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of(exception.getMessage(), List.of(exception.getMessage())));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(exception.getMessage(), List.of(exception.getMessage())));
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationFailed(AuthenticationFailedException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of(exception.getMessage(), List.of(exception.getMessage())));
    }

    @ExceptionHandler(FaceRecognitionUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleFaceRecognitionUnavailable(FaceRecognitionUnavailableException exception) {
        String reason = exception.getMessage();
        if (reason == null || reason.isBlank()) {
            reason = "Face recognition service is unavailable";
        }

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiErrorResponse.of(
                        "Face recognition service is unavailable",
                        List.of(reason)
                ));
    }

    @ExceptionHandler(TenantResolutionException.class)
    public ResponseEntity<ApiErrorResponse> handleTenantResolutionException(TenantResolutionException exception) {
        String message = exception.getMessage();
        String userMessage = "Invalid tenant configuration";
        String guidance = "Verify the X-Tenant-Slug header matches an existing tenant and try again.";

        if (message != null && !message.isBlank()) {
            if (message.contains("missing")) {
                userMessage = "Missing tenant header";
            } else if (message.contains("was not found")) {
                userMessage = "Unknown tenant";
            } else if (message.contains("not ready")) {
                userMessage = "Tenant schema is not ready";
            } else if (message.contains("does not match any registered tenant")) {
                userMessage = "Unknown tenant";
            } else if (message.contains("invalid")) {
                userMessage = "Invalid tenant value";
            }
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(
                        userMessage,
                        List.of(
                                message != null && !message.isBlank() ? message : guidance,
                                "Verify the X-Tenant-Slug header matches an existing tenant and that the tenant schema has been migrated."
                        )
                ));
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(RuntimeException exception) {
        String reason = exception.getMessage();
        if (reason == null || reason.isBlank()) {
            reason = "You do not have the required role or permission to access this resource";
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.of(
                        "You do not have the required role or permission to access this resource",
                        List.of(reason)
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        List<String> errors = exception.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> {
                    if (error instanceof FieldError fieldError) {
                        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
                    }
                    return error.getDefaultMessage();
                })
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of("Validation failed", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        List<String> errors = exception.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of("Constraint validation failed", errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException exception) {
        String reason = exception.getMostSpecificCause() != null && exception.getMostSpecificCause().getMessage() != null
                ? exception.getMostSpecificCause().getMessage()
                : "Request body is malformed or contains invalid field values";

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of("Invalid request body", List.of(reason)));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception) {
        String reason = exception.getMessage() != null && !exception.getMessage().isBlank()
                ? exception.getMessage()
                : "The uploaded file exceeds the maximum allowed size";

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiErrorResponse.of(
                        "Uploaded file too large",
                        List.of(reason, "Reduce the photo size and try again")
                ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleArgumentTypeMismatch(MethodArgumentTypeMismatchException exception) {
        String field = exception.getName() != null ? exception.getName() : "parameter";
        String expected = exception.getRequiredType() != null ? exception.getRequiredType().getSimpleName() : "expected type";
        String value = exception.getValue() != null ? String.valueOf(exception.getValue()) : "null";

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(
                        "Invalid request parameter",
                        List.of(field + " value '" + value + "' is not a valid " + expected)
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        String reason = extractDataIntegrityReason(exception);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of("Database constraint violation", List.of(reason)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception exception) {
        logger.error("Unexpected internal error", exception);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of("Unexpected internal error", List.of(
                        exception.getMessage() != null && !exception.getMessage().isBlank()
                                ? exception.getMessage()
                                : "An unexpected internal error occurred"
                )));
    }

    private String extractDataIntegrityReason(DataIntegrityViolationException exception) {
        Throwable current = exception;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && !message.isBlank()) {
                String normalized = message.trim();
                if (normalized.contains("duplicate key value")) {
                    return "Duplicate value violates a unique constraint";
                }
                if (normalized.contains("violates foreign key constraint")) {
                    return "Referenced entity does not exist";
                }
                if (normalized.contains("violates not-null constraint")) {
                    return "A required field was missing";
                }
                if (normalized.contains("violates check constraint")) {
                    return "A field value violates a business constraint";
                }
                return normalized;
            }
            current = current.getCause();
        }

        return "Database constraint violation";
    }
}
