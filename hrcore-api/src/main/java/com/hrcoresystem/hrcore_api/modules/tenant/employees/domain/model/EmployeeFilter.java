package com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model;

import java.util.UUID;

public record EmployeeFilter(String name, String employeeNumber, EmploymentStatus status,
                             UUID departmentId, UUID positionId) { }
