package com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model;

import java.time.LocalDate;
import java.util.UUID;

public record WorkCalendarExceptionFilter(UUID employeeId, UUID departmentId,
                                          LocalDate fromDate, LocalDate toDate,
                                          CalendarExceptionType exceptionType,
                                          Boolean workRequired, Boolean organizationOnly) { }
