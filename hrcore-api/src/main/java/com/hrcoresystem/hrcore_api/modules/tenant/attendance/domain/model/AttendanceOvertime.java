package com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.model;

import java.time.Instant;
import java.util.UUID;

public record AttendanceOvertime(UUID id, UUID attendanceRecordId, int requestedMinutes,
        Integer approvedMinutes, String reason, ReviewStatus status, UUID approvedBy,
        Instant approvedAt, Instant createdAt, Instant updatedAt) { }
