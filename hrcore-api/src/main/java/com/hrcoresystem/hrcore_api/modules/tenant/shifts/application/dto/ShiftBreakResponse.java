package com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record ShiftBreakResponse(
        UUID id,
        UUID shiftId,
        String name,
        LocalTime startTime,
        LocalTime endTime,
        int durationMinutes,
        boolean paid,
        Instant createdAt
) { }
