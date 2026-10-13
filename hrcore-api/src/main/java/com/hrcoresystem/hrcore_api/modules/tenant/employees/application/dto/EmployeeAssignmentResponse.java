package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.AssignmentType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeeAssignmentResponse(UUID id, UUID employeeId, UUID departmentId, UUID positionId,
        UUID supervisorEmployeeId, LocalDate startDate, LocalDate endDate, AssignmentType assignmentType,
        String reason, Instant createdAt, Instant updatedAt) { }
