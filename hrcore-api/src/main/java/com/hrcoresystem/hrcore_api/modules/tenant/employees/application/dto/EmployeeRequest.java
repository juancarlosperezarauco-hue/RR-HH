package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = false)
public record EmployeeRequest(UUID tenantUserId,
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
        @NotNull LocalDate hireDate) {
    /** Compatibility constructor for older Java callers; employeeNumber is backend-generated. */
    public EmployeeRequest(UUID tenantUserId, String ignoredEmployeeNumber, String firstName, String middleName,
                           String lastName, String secondLastName, String documentType, String documentNumber,
                           LocalDate birthDate, String gender, String nationality, String personalEmail,
                           String workEmail, String phone, String mobilePhone, String address, LocalDate hireDate) {
        this(tenantUserId, firstName, middleName, lastName, secondLastName, documentType, documentNumber,
                birthDate, gender, nationality, personalEmail, workEmail, phone, mobilePhone, address, hireDate);
    }
}
