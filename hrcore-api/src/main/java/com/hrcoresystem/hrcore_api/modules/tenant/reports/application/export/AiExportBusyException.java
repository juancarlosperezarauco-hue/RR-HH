package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.export;

public final class AiExportBusyException extends RuntimeException {
    public AiExportBusyException() { super("Hay otras exportaciones en proceso. Inténtalo de nuevo en unos momentos."); }
}
