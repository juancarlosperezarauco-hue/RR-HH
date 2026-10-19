package com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.exception.BusinessConflictException;
import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.AiFlowException;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.QueryPlanException;
import java.time.Instant;
import java.util.List;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AiConversationController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AiConversationExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(AiConversationExceptionHandler.class);

    public record Error(boolean success, String code, String message, List<String> errors,
                        Instant timestamp) {
        Error(String code, String message) {
            this(false, code, message, List.of(), Instant.now());
        }
    }

    @ExceptionHandler(AiFlowException.class)
    public ResponseEntity<Error> flow(AiFlowException failure) {
        HttpStatus status = switch (failure.code()) {
            case INVALID_QUESTION -> HttpStatus.BAD_REQUEST;
            case INVALID_PLANNER_RESPONSE -> HttpStatus.BAD_GATEWAY;
            case PLANNER_TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
        };
        return ResponseEntity.status(status).body(new Error(failure.code().name(), failure.getMessage()));
    }

    @ExceptionHandler(QueryPlanException.class)
    public ResponseEntity<Error> query(QueryPlanException failure) {
        HttpStatus status = switch (failure.code()) {
            case QUERY_NOT_ALLOWED -> HttpStatus.FORBIDDEN;
            case SERVICE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case QUERY_TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
            case INVALID_QUERY_PLAN, UNSUPPORTED_QUERY, QUERY_TOO_COMPLEX,
                    CATALOG_VERSION_MISMATCH -> HttpStatus.BAD_GATEWAY;
        };
        return ResponseEntity.status(status).body(new Error(failure.code().name(), failure.getMessage()));
    }

    @ExceptionHandler({BusinessConflictException.class, OptimisticLockingFailureException.class,
            DataIntegrityViolationException.class})
    public ResponseEntity<Error> conflict(RuntimeException failure) {
        String message = failure instanceof BusinessConflictException
                ? failure.getMessage() : "La conversación recibió una solicitud concurrente.";
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new Error("AI_CONVERSATION_CONFLICT", message));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Error> notFound(ResourceNotFoundException failure) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new Error("AI_CONVERSATION_NOT_FOUND", failure.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Error> invalid(BusinessException ignored) {
        return ResponseEntity.badRequest().body(new Error("INVALID_REQUEST", "La solicitud no es válida."));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class,
            HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Error> validation(Exception ignored) {
        return ResponseEntity.badRequest().body(new Error("INVALID_REQUEST", "La solicitud no es válida."));
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<Error> forbidden(RuntimeException ignored) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new Error("QUERY_NOT_ALLOWED", "No tienes acceso a esta conversación."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Error> unexpected(Exception failure) {
        log.error("AI conversation request failed: {}", failure.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new Error("AI_CONVERSATION_FAILED", "No se pudo completar la operación."));
    }
}
