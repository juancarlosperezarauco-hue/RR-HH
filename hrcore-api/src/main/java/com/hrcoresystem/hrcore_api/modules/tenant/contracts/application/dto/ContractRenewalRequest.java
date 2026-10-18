package com.hrcoresystem.hrcore_api.modules.tenant.contracts.application.dto;

import com.hrcoresystem.hrcore_api.modules.tenant.contracts.domain.model.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = false)
public record ContractRenewalRequest(
        @NotNull UUID employeeAssignmentId,
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
) {
    public ContractRenewalRequest(UUID employeeAssignmentId, String ignoredContractNumber, ContractType contractType,
                                 ContractForm contractForm, WorkSchedule workSchedule, WorkModality workModality,
                                 LocalDate startDate, LocalDate endDate, BigDecimal baseSalary, String currency,
                                 PaymentFrequency paymentFrequency, BigDecimal weeklyHours, LocalDate probationEndDate,
                                 String terms, String reason) {
        this(employeeAssignmentId, contractType, contractForm, workSchedule, workModality, startDate, endDate,
                baseSalary, currency, paymentFrequency, weeklyHours, probationEndDate, terms, reason);
    }
}
