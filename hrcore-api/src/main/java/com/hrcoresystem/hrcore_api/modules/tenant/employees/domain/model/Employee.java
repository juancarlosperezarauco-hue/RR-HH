package com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record Employee(UUID id, UUID tenantUserId, String employeeNumber,
                       String firstName, String middleName, String lastName, String secondLastName,
                       String documentType, String documentNumber, LocalDate birthDate,
                       String gender, String nationality, String personalEmail, String workEmail,
                       String phone, String mobilePhone, String address, LocalDate hireDate,
                       LocalDate terminationDate, EmploymentStatus employmentStatus,
                       UUID createdBy, UUID updatedBy, Instant createdAt, Instant updatedAt) { }
