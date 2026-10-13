package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.AssignmentType;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeeAssignmentRequest(@NotNull UUID departmentId, @NotNull UUID positionId,
        UUID supervisorEmployeeId, @NotNull LocalDate startDate, LocalDate endDate,
        AssignmentType assignmentType, @Size(max = 255) String reason) { }
