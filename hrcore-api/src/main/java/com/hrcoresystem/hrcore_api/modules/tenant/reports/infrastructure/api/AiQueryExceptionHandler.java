package com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.api;

import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.AiFlowException;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.QueryPlanException;
import java.time.Instant;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Adds stable codes only to this new endpoint; existing API error contracts stay intact. */
@RestControllerAdvice(assignableTypes = AiQueryController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class AiQueryExceptionHandler {
    public record Error(boolean success, String code, String message, List<String> errors,
                        Instant timestamp) {
        Error(String code, String message) {
            this(false, code, message, List.of(), Instant.now());
        }
    }

    @ExceptionHandler(AiFlowException.class)
    public ResponseEntity<Error> flow(AiFlowException failure) {
        HttpStatus status = failure.code() == AiFlowException.Code.INVALID_QUESTION
                ? HttpStatus.BAD_REQUEST : HttpStatus.BAD_GATEWAY;
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
}
