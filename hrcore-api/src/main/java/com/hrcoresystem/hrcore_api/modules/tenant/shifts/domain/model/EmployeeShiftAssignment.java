package com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record EmployeeShiftAssignment(UUID id, UUID employeeId, UUID shiftId,
        UUID employmentPeriodId, LocalDate startDate, LocalDate endDate, List<Short> weekdays,
        ShiftAssignmentStatus status, UUID createdBy, Instant createdAt, Instant updatedAt) {
    public EmployeeShiftAssignment(UUID id, UUID employeeId, UUID shiftId,
            LocalDate startDate, LocalDate endDate, List<Short> weekdays,
            ShiftAssignmentStatus status, UUID createdBy, Instant createdAt, Instant updatedAt) {
        this(id, employeeId, shiftId, null, startDate, endDate, weekdays, status, createdBy, createdAt, updatedAt);
    }
    public EmployeeShiftAssignment {
        weekdays = weekdays == null ? null : List.copyOf(weekdays);
    }
}
