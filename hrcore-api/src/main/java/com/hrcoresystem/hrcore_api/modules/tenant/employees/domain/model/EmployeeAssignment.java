package com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeeAssignment(UUID id, UUID employeeId, UUID departmentId, UUID positionId,
                                 UUID supervisorEmployeeId, LocalDate startDate, LocalDate endDate,
                                 AssignmentType assignmentType, String reason, UUID createdBy,
                                 Instant createdAt, Instant updatedAt) { }
