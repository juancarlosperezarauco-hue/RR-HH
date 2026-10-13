package com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto;

import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.ShiftAssignmentStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ShiftAssignmentResponse(UUID id, UUID employeeId, UUID shiftId,
        LocalDate startDate, LocalDate endDate, List<Short> weekdays,
        ShiftAssignmentStatus status, Instant createdAt, Instant updatedAt) { }
