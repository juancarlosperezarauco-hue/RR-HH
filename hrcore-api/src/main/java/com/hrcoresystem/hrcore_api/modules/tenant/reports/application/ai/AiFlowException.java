package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai;

/** Errors owned by the new HTTP planning flow, never containing user or service data. */
public final class AiFlowException extends RuntimeException {
    public enum Code { INVALID_QUESTION, INVALID_PLANNER_RESPONSE, PLANNER_TIMEOUT }

    private final Code code;

    public AiFlowException(Code code) {
        super(switch (code) {
            case INVALID_QUESTION -> "La pregunta no es válida.";
            case INVALID_PLANNER_RESPONSE -> "El planificador devolvió una respuesta inválida.";
            case PLANNER_TIMEOUT -> "El planificador excedió el tiempo permitido.";
        });
        this.code = code;
    }

    public Code code() { return code; }
}
