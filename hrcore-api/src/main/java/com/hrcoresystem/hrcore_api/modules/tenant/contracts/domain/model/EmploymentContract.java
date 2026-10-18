package com.hrcoresystem.hrcore_api.modules.tenant.contracts.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EmploymentContract(
        UUID id, UUID employeeId, UUID employeeAssignmentId, String contractNumber,
        ContractType contractType, ContractStatus status, ContractForm contractForm,
        WorkSchedule workSchedule, WorkModality workModality,
        LocalDate startDate, LocalDate endDate, Instant signedAt,
        BigDecimal baseSalary, String currency, PaymentFrequency paymentFrequency,
        BigDecimal weeklyHours, LocalDate probationEndDate,
        LocalDate terminationDate, String terminationReason, String terms,
        UUID renewedFromContractId, UUID createdBy, UUID updatedBy,
        Instant createdAt, Instant updatedAt, UUID employmentPeriodId
) {
    public EmploymentContract(UUID id, UUID employeeId, UUID employeeAssignmentId, String contractNumber,
            ContractType contractType, ContractStatus status, ContractForm contractForm, WorkSchedule workSchedule,
            WorkModality workModality, LocalDate startDate, LocalDate endDate, Instant signedAt,
            BigDecimal baseSalary, String currency, PaymentFrequency paymentFrequency, BigDecimal weeklyHours,
            LocalDate probationEndDate, LocalDate terminationDate, String terminationReason, String terms,
            UUID renewedFromContractId, UUID createdBy, UUID updatedBy, Instant createdAt, Instant updatedAt) {
        this(id, employeeId, employeeAssignmentId, contractNumber, contractType, status, contractForm, workSchedule,
                workModality, startDate, endDate, signedAt, baseSalary, currency, paymentFrequency, weeklyHours,
                probationEndDate, terminationDate, terminationReason, terms, renewedFromContractId, createdBy, updatedBy,
                createdAt, updatedAt, null);
    }
}
