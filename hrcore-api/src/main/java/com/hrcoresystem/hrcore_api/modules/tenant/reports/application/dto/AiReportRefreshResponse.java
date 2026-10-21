package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.dto;

import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.QueryResultV1;
import java.time.Instant;
import java.util.UUID;

public record AiReportRefreshResponse(
        UUID reportRunId,
        UUID refreshedFromRunId,
        Instant refreshedAt,
        QueryResultV1 result) {
}
