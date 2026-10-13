package com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AttendancePunch(UUID id, UUID employeeId, UUID attendanceRecordId, PunchType punchType,
        Instant punchedAt, PunchSource source, String deviceIdentifier, BigDecimal latitude,
        BigDecimal longitude, UUID recordedBy, String notes, Instant createdAt) { }
