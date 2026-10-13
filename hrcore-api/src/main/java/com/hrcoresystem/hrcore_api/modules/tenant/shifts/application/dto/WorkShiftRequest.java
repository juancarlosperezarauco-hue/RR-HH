package com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record WorkShiftRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 150) String name,
        String description,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotNull Boolean crossesMidnight,
        @NotNull @Min(0) Integer lateToleranceMinutes,
        @NotNull @Min(0) Integer earlyDepartureToleranceMinutes
) { }
