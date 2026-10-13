package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.EmploymentStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeeResponse(UUID id, UUID tenantUserId, String employeeNumber,
        String firstName, String middleName, String lastName, String secondLastName,
        String documentType, String documentNumber, LocalDate birthDate,
        String gender, String nationality, String personalEmail, String workEmail,
        String phone, String mobilePhone, String address, LocalDate hireDate,
        LocalDate terminationDate, EmploymentStatus employmentStatus,
        Instant createdAt, Instant updatedAt) { }
