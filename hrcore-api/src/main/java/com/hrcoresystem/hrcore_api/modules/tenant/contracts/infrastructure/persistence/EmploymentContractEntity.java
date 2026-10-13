package com.hrcoresystem.hrcore_api.modules.tenant.contracts.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.contracts.domain.model.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "employment_contracts")
@Getter @Setter
public class EmploymentContractEntity {
    @Id @GeneratedValue private UUID id;
    @Column(name = "employee_id", nullable = false) private UUID employeeId;
    @Column(name = "employee_assignment_id") private UUID employeeAssignmentId;
    @Column(name = "contract_number", nullable = false, length = 80) private String contractNumber;
    @Enumerated(EnumType.STRING) @Column(name = "contract_type", nullable = false, length = 30) private ContractType contractType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private ContractStatus status;
    @Enumerated(EnumType.STRING) @Column(name = "contract_form", nullable = false, length = 20) private ContractForm contractForm;
    @Enumerated(EnumType.STRING) @Column(name = "work_schedule", nullable = false, length = 20) private WorkSchedule workSchedule;
    @Enumerated(EnumType.STRING) @Column(name = "work_modality", nullable = false, length = 20) private WorkModality workModality;
    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @Column(name = "end_date") private LocalDate endDate;
    @Column(name = "signed_at") private Instant signedAt;
    @Column(name = "base_salary", nullable = false, precision = 19, scale = 4) private BigDecimal baseSalary;
    @Column(nullable = false, length = 3) private String currency;
    @Enumerated(EnumType.STRING) @Column(name = "payment_frequency", nullable = false, length = 20) private PaymentFrequency paymentFrequency;
    @Column(name = "weekly_hours", precision = 6, scale = 2) private BigDecimal weeklyHours;
    @Column(name = "probation_end_date") private LocalDate probationEndDate;
    @Column(name = "termination_date") private LocalDate terminationDate;
    @Column(name = "termination_reason", columnDefinition = "TEXT") private String terminationReason;
    @Column(columnDefinition = "TEXT") private String terms;
    @Column(name = "renewed_from_contract_id") private UUID renewedFromContractId;
    @Column(name = "created_by", updatable = false) private UUID createdBy;
    @Column(name = "updated_by") private UUID updatedBy;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @PrePersist void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (status == null) status = ContractStatus.DRAFT;
        updatedAt = now;
    }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }
}
