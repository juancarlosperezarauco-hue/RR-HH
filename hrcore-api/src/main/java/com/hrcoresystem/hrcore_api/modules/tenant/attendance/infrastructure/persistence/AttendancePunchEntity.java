package com.hrcoresystem.hrcore_api.modules.tenant.attendance.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.model.PunchSource;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.model.PunchType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "attendance_punches")
@Getter @Setter
public class AttendancePunchEntity {
    @Id @GeneratedValue private UUID id;
    @Column(name = "employee_id", nullable = false, updatable = false) private UUID employeeId;
    @Column(name = "attendance_record_id") private UUID attendanceRecordId;
    @Enumerated(EnumType.STRING) @Column(name = "punch_type", nullable = false, length = 20, updatable = false) private PunchType punchType;
    @Column(name = "punched_at", nullable = false, updatable = false) private Instant punchedAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30, updatable = false) private PunchSource source;
    @Column(name = "device_identifier", length = 150, updatable = false) private String deviceIdentifier;
    @Column(precision = 10, scale = 7, updatable = false) private BigDecimal latitude;
    @Column(precision = 10, scale = 7, updatable = false) private BigDecimal longitude;
    @Column(name = "recorded_by", updatable = false) private UUID recordedBy;
    @Column(columnDefinition = "TEXT", updatable = false) private String notes;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    @PrePersist void create() { if (createdAt == null) createdAt = Instant.now(); }
}
