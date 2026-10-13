package com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.model.AdjustmentType;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.model.PunchType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class AttendanceRequests {
    private AttendanceRequests() { }

    public record Calculate(@NotNull UUID employeeId, @NotNull LocalDate workDate) { }

    public record Punch(UUID employeeId, @NotNull PunchType punchType, Instant punchedAt,
            @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
            @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
            @Size(max = 2000) String notes) { }

    public record Adjustment(@NotNull UUID attendanceRecordId, @NotNull AdjustmentType adjustmentType,
            @NotBlank String reason, @NotNull JsonNode requestedValues) { }

    public record Overtime(@NotNull UUID attendanceRecordId, @NotNull @Positive Integer requestedMinutes,
            @Size(max = 2000) String reason) { }

    public record Review(@Size(max = 2000) String comment) { }

    public record OvertimeApproval(@NotNull @Min(0) Integer approvedMinutes,
            @Size(max = 2000) String comment) { }
}
