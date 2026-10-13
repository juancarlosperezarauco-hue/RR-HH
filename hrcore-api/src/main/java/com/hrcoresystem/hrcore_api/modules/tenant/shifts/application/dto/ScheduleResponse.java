package com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ScheduleResponse(UUID employeeId, LocalDate date, UUID departmentId,
                               UUID calendarExceptionId, String source,
                               ScheduleDetermination determination, String reason,
                               List<ScheduleInterval> intervals) { }
