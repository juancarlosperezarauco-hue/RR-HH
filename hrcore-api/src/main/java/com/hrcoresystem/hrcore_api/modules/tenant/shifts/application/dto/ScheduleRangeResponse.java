package com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ScheduleRangeResponse(UUID employeeId, LocalDate fromDate, LocalDate toDate,
                                    List<ScheduleResponse> days) { }
