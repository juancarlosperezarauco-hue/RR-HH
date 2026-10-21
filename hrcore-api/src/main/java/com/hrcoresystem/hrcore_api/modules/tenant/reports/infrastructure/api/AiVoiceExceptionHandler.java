package com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.api;

import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.voice.AiVoiceException;
import java.time.Instant;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AiVoiceController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AiVoiceExceptionHandler {
    public record Error(boolean success, String code, String message, List<String> errors, Instant timestamp) {}
    @ExceptionHandler(AiVoiceException.class)
    public ResponseEntity<Error> voice(AiVoiceException failure) {
        return ResponseEntity.status(failure.code().status()).header("Cache-Control", "no-store")
                .body(new Error(false, "AI_VOICE_" + failure.code().name(), failure.getMessage(),
                        List.of(), Instant.now()));
    }
}
