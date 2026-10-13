package com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ScheduleInterval(UUID shiftId, LocalDateTime start, LocalDateTime end) { }
