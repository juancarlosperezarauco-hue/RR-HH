package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai;

/** Stable internal error codes for a future HTTP adapter. Messages contain no plan values. */
public final class QueryPlanException extends RuntimeException {
    public enum Code {
        INVALID_QUERY_PLAN, UNSUPPORTED_QUERY, QUERY_NOT_ALLOWED,
        QUERY_TOO_COMPLEX, CATALOG_VERSION_MISMATCH, QUERY_TIMEOUT,
        SERVICE_UNAVAILABLE
    }

    private final Code code;

    public QueryPlanException(Code code) {
        super(switch (code) {
            case INVALID_QUERY_PLAN -> "El plan de consulta no es válido.";
            case UNSUPPORTED_QUERY -> "La consulta solicitada no está disponible.";
            case QUERY_NOT_ALLOWED -> "No tienes acceso a toda la información solicitada.";
            case QUERY_TOO_COMPLEX -> "La consulta supera los límites permitidos.";
            case CATALOG_VERSION_MISMATCH -> "La versión del catálogo no está disponible.";
            case QUERY_TIMEOUT -> "La consulta superó el tiempo permitido.";
            case SERVICE_UNAVAILABLE -> "No se pudo ejecutar la consulta.";
        });
        this.code = code;
    }

    public Code code() { return code; }
}
