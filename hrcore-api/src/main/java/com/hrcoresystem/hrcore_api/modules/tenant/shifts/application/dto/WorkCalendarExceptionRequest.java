package com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto;

import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.CalendarExceptionType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record WorkCalendarExceptionRequest(UUID employeeId, UUID departmentId,
                                           @NotNull LocalDate exceptionDate,
                                           @NotNull CalendarExceptionType exceptionType,
                                           String description,
                                           @NotNull Boolean workRequired) { }
