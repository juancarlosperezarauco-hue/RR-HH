package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai;

/** Errors owned by the new HTTP planning flow, never containing user or service data. */
public final class AiFlowException extends RuntimeException {
    public enum Code { INVALID_QUESTION, INVALID_PLANNER_RESPONSE }

    private final Code code;

    public AiFlowException(Code code) {
        super(code == Code.INVALID_QUESTION
                ? "La pregunta no es válida." : "El planificador devolvió una respuesta inválida.");
        this.code = code;
    }

    public Code code() { return code; }
}
