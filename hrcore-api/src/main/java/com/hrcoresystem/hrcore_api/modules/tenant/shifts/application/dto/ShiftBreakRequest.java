package com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record ShiftBreakRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotNull @Min(1) @Max(1440) Integer durationMinutes,
        @NotNull Boolean paid
) { }
