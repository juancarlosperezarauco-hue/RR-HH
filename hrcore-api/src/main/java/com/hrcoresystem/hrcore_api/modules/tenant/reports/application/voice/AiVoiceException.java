package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.voice;

import org.springframework.http.HttpStatus;

public final class AiVoiceException extends RuntimeException {
    public enum Code {
        INVALID_AUDIO(HttpStatus.BAD_REQUEST, "La grabación no tiene un formato de audio válido."),
        TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "La grabación supera el tamaño permitido."),
        TOO_LONG(HttpStatus.UNPROCESSABLE_ENTITY, "La grabación supera la duración permitida."),
        BUSY(HttpStatus.TOO_MANY_REQUESTS, "Hay demasiadas transcripciones en curso. Inténtalo más tarde."),
        QUOTA(HttpStatus.TOO_MANY_REQUESTS, "La cuota de transcripción está agotada. Inténtalo más tarde."),
        UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "La transcripción no está disponible."),
        PROVIDER(HttpStatus.BAD_GATEWAY, "El proveedor de transcripción no respondió correctamente."),
        TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "La transcripción superó el tiempo de espera.");
        private final HttpStatus status;
        private final String message;
        Code(HttpStatus status, String message) { this.status = status; this.message = message; }
        public HttpStatus status() { return status; }
    }
    private final Code code;
    public AiVoiceException(Code code) { super(code.message); this.code = code; }
    public Code code() { return code; }
}
