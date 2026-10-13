package com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.model;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AttendanceRecord(UUID id, UUID employeeId, UUID shiftAssignmentId, LocalDate workDate,
        Instant expectedStartAt, Instant expectedEndAt, Instant actualFirstInAt, Instant actualLastOutAt,
        int workedMinutes, int lateMinutes, int earlyDepartureMinutes, AttendanceStatus status,
        JsonNode calculationDetails, Instant calculatedAt, Instant createdAt, Instant updatedAt) { }
