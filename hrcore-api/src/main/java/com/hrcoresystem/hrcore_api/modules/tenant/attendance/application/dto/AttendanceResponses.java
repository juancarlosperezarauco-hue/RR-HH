package com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.model.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class AttendanceResponses {
    private AttendanceResponses() { }

    public record Record(UUID id, UUID employeeId, UUID shiftAssignmentId, LocalDate workDate,
            Instant expectedStartAt, Instant expectedEndAt, Instant actualFirstInAt, Instant actualLastOutAt,
            int workedMinutes, int lateMinutes, int earlyDepartureMinutes, AttendanceStatus status,
            JsonNode calculationDetails, Instant calculatedAt, Instant createdAt, Instant updatedAt) { }

    public record Punch(UUID id, UUID employeeId, UUID attendanceRecordId, PunchType punchType,
            Instant punchedAt, PunchSource source, BigDecimal latitude, BigDecimal longitude,
            UUID recordedBy, String notes, Instant createdAt) { }

    public record Adjustment(UUID id, UUID attendanceRecordId, AdjustmentType adjustmentType,
            String reason, JsonNode previousValues, JsonNode requestedValues, ReviewStatus status,
            UUID requestedBy, UUID reviewedBy, Instant reviewedAt, String reviewComment,
            Instant appliedAt, Instant createdAt, Instant updatedAt) { }

    public record Overtime(UUID id, UUID attendanceRecordId, int requestedMinutes, Integer approvedMinutes,
            String reason, ReviewStatus status, UUID approvedBy, Instant approvedAt,
            Instant createdAt, Instant updatedAt) { }
}
