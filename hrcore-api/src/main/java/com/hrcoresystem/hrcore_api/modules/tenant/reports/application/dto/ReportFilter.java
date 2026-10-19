package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ReportFilter(
        LocalDate fromDate,
        LocalDate toDate,
        UUID departmentId,
        UUID positionId,
        UUID employeeId,
        UUID employmentPeriodId,
        String status
) {}
