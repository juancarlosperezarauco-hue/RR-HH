package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto;

import jakarta.validation.constraints.*;

public record EmployeeEmergencyContactRequest(@NotBlank @Size(max = 200) String fullName,
        @Size(max = 80) String relationship,
        @NotBlank @Size(max = 40) String phone,
        @Size(max = 40) String alternativePhone,
        String address, Boolean primary) { }
