package com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.model;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record AttendanceAdjustment(UUID id, UUID attendanceRecordId, AdjustmentType adjustmentType,
        String reason, JsonNode previousValues, JsonNode requestedValues, ReviewStatus status,
        UUID requestedBy, UUID reviewedBy, Instant reviewedAt, String reviewComment,
        Instant appliedAt, Instant createdAt, Instant updatedAt) { }
