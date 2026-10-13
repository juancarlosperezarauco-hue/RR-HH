package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto;

import java.time.Instant;
import java.util.UUID;

public record EmployeeEmergencyContactResponse(UUID id, UUID employeeId, String fullName,
        String relationship, String phone, String alternativePhone, String address,
        boolean primary, Instant createdAt, Instant updatedAt) { }
