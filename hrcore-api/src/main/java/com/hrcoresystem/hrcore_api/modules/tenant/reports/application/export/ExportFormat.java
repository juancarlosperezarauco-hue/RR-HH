package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.export;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;

public enum ExportFormat {
    CSV("csv", "text/csv;charset=UTF-8"),
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    PDF("pdf", "application/pdf");

    public final String extension;
    public final String mimeType;
    ExportFormat(String extension, String mimeType) { this.extension = extension; this.mimeType = mimeType; }
    public static ExportFormat parse(String value) {
        for (var format : values()) if (format.extension.equals(value)) return format;
        throw new BusinessException("Formato de exportación no válido");
    }
}
