package com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateShiftAssignmentsRequest(
        @NotEmpty @Size(max = 100) List<@NotNull UUID> employeeIds,
        @NotNull UUID shiftId,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        @NotNull @Size(min = 1, max = 7) List<@NotNull Short> weekdays
) { }
