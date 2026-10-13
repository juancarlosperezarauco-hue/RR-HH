package com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto;

import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.ShiftStatus;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record WorkShiftResponse(
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
        Instant createdAt,
        Instant updatedAt
) { }
