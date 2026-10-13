package com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record ShiftBreak(
        UUID id,
        UUID shiftId,
        String name,
        LocalTime startTime,
        LocalTime endTime,
        int durationMinutes,
        boolean paid,
        Instant createdAt
) { }
