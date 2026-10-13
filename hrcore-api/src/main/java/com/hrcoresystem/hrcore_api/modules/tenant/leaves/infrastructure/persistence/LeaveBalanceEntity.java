package com.hrcoresystem.hrcore_api.modules.tenant.leaves.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "leave_balances") @Getter @Setter
public class LeaveBalanceEntity {
    @Id private UUID id;
    @Column(name = "employee_id", nullable = false, updatable = false) private UUID employeeId;
    @Column(name = "leave_type_id", nullable = false, updatable = false) private UUID leaveTypeId;
    @Column(name = "balance_year", nullable = false, updatable = false) private int balanceYear;
    @Column(name = "opening_days", nullable = false, precision = 8, scale = 2) private BigDecimal openingDays;
    @Column(name = "accrued_days", nullable = false, precision = 8, scale = 2) private BigDecimal accruedDays;
    @Column(name = "used_days", nullable = false, precision = 8, scale = 2) private BigDecimal usedDays;
    @Column(name = "adjusted_days", nullable = false, precision = 8, scale = 2) private BigDecimal adjustedDays;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @PrePersist @PreUpdate void update() { updatedAt = Instant.now(); }
}
