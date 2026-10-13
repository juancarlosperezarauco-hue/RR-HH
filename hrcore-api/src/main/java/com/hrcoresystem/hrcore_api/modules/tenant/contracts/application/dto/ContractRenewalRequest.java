package com.hrcoresystem.hrcore_api.modules.tenant.contracts.application.dto;

import com.hrcoresystem.hrcore_api.modules.tenant.contracts.domain.model.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ContractRenewalRequest(
        @NotNull UUID employeeAssignmentId,
        @NotBlank @Size(max = 80) String contractNumber,
        @NotNull ContractType contractType,
        @NotNull ContractForm contractForm,
        @NotNull WorkSchedule workSchedule,
        @NotNull WorkModality workModality,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        @NotNull @DecimalMin("0.00") BigDecimal baseSalary,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency,
        @NotNull PaymentFrequency paymentFrequency,
        @DecimalMin("0.01") @DecimalMax("168.00") BigDecimal weeklyHours,
        LocalDate probationEndDate,
        @Size(max = 10000) String terms,
        @Size(max = 2000) String reason
) { }
