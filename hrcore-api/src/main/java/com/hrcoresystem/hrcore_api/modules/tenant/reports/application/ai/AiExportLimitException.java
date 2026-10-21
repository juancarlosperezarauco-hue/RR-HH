package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai;

public final class AiExportLimitException extends RuntimeException {
    public AiExportLimitException(String safeMessage) { super(safeMessage); }
    public AiExportLimitException() {
        super("El reporte supera los límites de exportación. Aplica filtros más específicos; para PDF, prueba Excel o CSV. No se generó una descarga parcial.");
    }
}
