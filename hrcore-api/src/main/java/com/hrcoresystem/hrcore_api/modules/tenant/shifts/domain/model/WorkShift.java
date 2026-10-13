package com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record WorkShift(
        UUID id,
        String code,
        String name,
        String description,
        LocalTime startTime,
        LocalTime endTime,
        boolean crossesMidnight,
        int lateToleranceMinutes,
        int earlyDepartureToleranceMinutes,
        ShiftStatus status,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt
) { }
