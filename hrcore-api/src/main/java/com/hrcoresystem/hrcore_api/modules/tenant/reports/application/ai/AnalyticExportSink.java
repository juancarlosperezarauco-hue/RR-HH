package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai;

import java.util.List;
import java.util.Map;

/** Receives authorized columns and one row at a time, on the authenticated request thread. */
public interface AnalyticExportSink {
    void columns(List<QueryResultV1.Column> columns);
    void row(Map<String, Object> row);
    /** Remaining generation budget; the cursor uses it before a blocking fetch. */
    default long remainingMillis() { return Long.MAX_VALUE; }
}
