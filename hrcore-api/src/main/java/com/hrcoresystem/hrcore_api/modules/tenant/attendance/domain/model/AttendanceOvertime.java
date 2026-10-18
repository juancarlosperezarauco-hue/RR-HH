package com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.model;

import java.time.Instant;
import java.util.UUID;

public record AttendanceOvertime(UUID id, UUID attendanceRecordId, int requestedMinutes,
        Integer approvedMinutes, String reason, ReviewStatus status, UUID approvedBy,
        Instant approvedAt, UUID requestedBy, Instant createdAt, Instant updatedAt) {
    public AttendanceOvertime(UUID id, UUID attendanceRecordId, int requestedMinutes,
            Integer approvedMinutes, String reason, ReviewStatus status, UUID approvedBy,
            Instant approvedAt, Instant createdAt, Instant updatedAt) {
        this(id, attendanceRecordId, requestedMinutes, approvedMinutes, reason, status, approvedBy,
                approvedAt, null, createdAt, updatedAt);
    }
}
