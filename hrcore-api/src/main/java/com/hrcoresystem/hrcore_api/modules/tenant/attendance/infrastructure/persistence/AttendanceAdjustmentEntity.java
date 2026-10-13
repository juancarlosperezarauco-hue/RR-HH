package com.hrcoresystem.hrcore_api.modules.tenant.attendance.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.model.AdjustmentType;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.model.ReviewStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "attendance_adjustments")
@Getter @Setter
public class AttendanceAdjustmentEntity {
    @Id @GeneratedValue private UUID id;
    @Column(name = "attendance_record_id", nullable = false, updatable = false) private UUID attendanceRecordId;
    @Enumerated(EnumType.STRING) @Column(name = "adjustment_type", nullable = false, length = 30, updatable = false) private AdjustmentType adjustmentType;
    @Column(nullable = false, columnDefinition = "TEXT", updatable = false) private String reason;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "previous_values", columnDefinition = "jsonb", updatable = false) private JsonNode previousValues;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "requested_values", nullable = false, columnDefinition = "jsonb", updatable = false) private JsonNode requestedValues;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ReviewStatus status;
    @Column(name = "requested_by", nullable = false, updatable = false) private UUID requestedBy;
    @Column(name = "reviewed_by") private UUID reviewedBy;
    @Column(name = "reviewed_at") private Instant reviewedAt;
    @Column(name = "review_comment", columnDefinition = "TEXT") private String reviewComment;
    @Column(name = "applied_at") private Instant appliedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @PrePersist void create() { Instant now = Instant.now(); if (createdAt == null) createdAt = now; updatedAt = now; }
    @PreUpdate void update() { updatedAt = Instant.now(); }
}
