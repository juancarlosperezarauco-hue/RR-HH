package com.hrcoresystem.hrcore_api.modules.tenant.attendance.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.model.ReviewStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "attendance_overtime")
@Getter @Setter
public class AttendanceOvertimeEntity {
    @Id @GeneratedValue private UUID id;
    @Column(name = "attendance_record_id", nullable = false, updatable = false) private UUID attendanceRecordId;
    @Column(name = "requested_minutes", nullable = false, updatable = false) private int requestedMinutes;
    @Column(name = "approved_minutes") private Integer approvedMinutes;
    @Column(columnDefinition = "TEXT", updatable = false) private String reason;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ReviewStatus status;
    @Column(name = "approved_by") private UUID approvedBy;
    @Column(name = "approved_at") private Instant approvedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @PrePersist void create() { Instant now = Instant.now(); if (createdAt == null) createdAt = now; updatedAt = now; }
    @PreUpdate void update() { updatedAt = Instant.now(); }
}
