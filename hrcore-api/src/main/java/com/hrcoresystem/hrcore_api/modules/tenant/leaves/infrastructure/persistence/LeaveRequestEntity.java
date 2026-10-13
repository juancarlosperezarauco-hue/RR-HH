package com.hrcoresystem.hrcore_api.modules.tenant.leaves.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.leaves.domain.model.LeaveModels.RequestStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "leave_requests") @Getter @Setter
public class LeaveRequestEntity {
    @Id private UUID id;
    @Column(name = "employee_id", nullable = false) private UUID employeeId;
    @Column(name = "leave_type_id", nullable = false) private UUID leaveTypeId;
    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @Column(name = "end_date", nullable = false) private LocalDate endDate;
    @Column(name = "start_time") private LocalTime startTime;
    @Column(name = "end_time") private LocalTime endTime;
    @Column(name = "requested_days", nullable = false, precision = 8, scale = 2) private BigDecimal requestedDays;
    @Column(nullable = false, columnDefinition = "TEXT") private String reason;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private RequestStatus status;
    @Column(name = "requested_by") private UUID requestedBy;
    @Column(name = "submitted_at") private Instant submittedAt;
    @Column(name = "reviewed_by") private UUID reviewedBy;
    @Column(name = "reviewed_at") private Instant reviewedAt;
    @Column(name = "review_comment", columnDefinition = "TEXT") private String reviewComment;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @PrePersist void create() { if (createdAt == null) createdAt = Instant.now(); updatedAt = Instant.now(); }
    @PreUpdate void update() { updatedAt = Instant.now(); }
}
