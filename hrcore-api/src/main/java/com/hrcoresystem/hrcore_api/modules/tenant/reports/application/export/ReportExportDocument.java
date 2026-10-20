package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.export;

import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.AnalyticExportSink;

interface ReportExportDocument extends AnalyticExportSink, AutoCloseable {
    void finish();
    @Override void close();
}
