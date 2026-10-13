package com.hrcoresystem.hrcore_api.modules.tenant.attendance.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.model.AttendanceStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "attendance_records")
@Getter @Setter
public class AttendanceRecordEntity {
    @Id @GeneratedValue private UUID id;
    @Column(name = "employee_id", nullable = false, updatable = false) private UUID employeeId;
    @Column(name = "shift_assignment_id") private UUID shiftAssignmentId;
    @Column(name = "work_date", nullable = false, updatable = false) private LocalDate workDate;
    @Column(name = "expected_start_at") private Instant expectedStartAt;
    @Column(name = "expected_end_at") private Instant expectedEndAt;
    @Column(name = "actual_first_in_at") private Instant actualFirstInAt;
    @Column(name = "actual_last_out_at") private Instant actualLastOutAt;
    @Column(name = "worked_minutes", nullable = false) private int workedMinutes;
    @Column(name = "late_minutes", nullable = false) private int lateMinutes;
    @Column(name = "early_departure_minutes", nullable = false) private int earlyDepartureMinutes;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private AttendanceStatus status;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "calculation_details", columnDefinition = "jsonb") private JsonNode calculationDetails;
    @Column(name = "calculated_at") private Instant calculatedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @PrePersist void create() { Instant now = Instant.now(); if (createdAt == null) createdAt = now; updatedAt = now; }
    @PreUpdate void update() { updatedAt = Instant.now(); }
}
