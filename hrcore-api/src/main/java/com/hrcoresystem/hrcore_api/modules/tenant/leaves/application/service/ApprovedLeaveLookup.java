package com.hrcoresystem.hrcore_api.modules.tenant.leaves.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** Contrato interno que integra ausencias aprobadas (HU18) con asistencia (HU17). */
public interface ApprovedLeaveLookup {
    List<ApprovedPeriod> onDate(UUID employeeId, LocalDate date);

    record ApprovedPeriod(UUID requestId, UUID leaveTypeId, LocalDate startDate, LocalDate endDate,
            LocalTime startTime, LocalTime endTime, BigDecimal requestedDays) { }
}
