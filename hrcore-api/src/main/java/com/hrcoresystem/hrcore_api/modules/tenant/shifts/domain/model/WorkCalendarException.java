package com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record WorkCalendarException(UUID id, UUID employeeId, UUID departmentId,
                                    LocalDate exceptionDate, CalendarExceptionType exceptionType,
                                    String description, boolean workRequired,
                                    UUID createdBy, Instant createdAt) { }
