package com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model;

import java.time.Instant;
import java.util.UUID;

public record EmployeeEmergencyContact(UUID id, UUID employeeId, String fullName, String relationship,
                                       String phone, String alternativePhone, String address,
                                       boolean primary, Instant createdAt, Instant updatedAt) { }
