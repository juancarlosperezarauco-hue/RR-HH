package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeeRequest(UUID tenantUserId,
        @NotBlank @Size(max = 50) String employeeNumber,
        @NotBlank @Size(max = 100) String firstName,
        @Size(max = 100) String middleName,
        @NotBlank @Size(max = 100) String lastName,
        @Size(max = 100) String secondLastName,
        @NotBlank @Size(max = 30) String documentType,
        @NotBlank @Size(max = 50) String documentNumber,
        LocalDate birthDate,
        @Size(max = 30) String gender,
        @Size(max = 80) String nationality,
        @Email @Size(max = 150) String personalEmail,
        @Email @Size(max = 150) String workEmail,
        @Size(max = 40) String phone,
        @Size(max = 40) String mobilePhone,
        String address,
        @NotNull LocalDate hireDate) { }
