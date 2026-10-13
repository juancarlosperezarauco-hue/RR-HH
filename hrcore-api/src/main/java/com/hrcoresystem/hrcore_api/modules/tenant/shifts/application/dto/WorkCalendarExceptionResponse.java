package com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto;

import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.CalendarExceptionType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record WorkCalendarExceptionResponse(UUID id, UUID employeeId, UUID departmentId,
                                            LocalDate exceptionDate, CalendarExceptionType exceptionType,
                                            String description, boolean workRequired,
                                            Instant createdAt) { }
