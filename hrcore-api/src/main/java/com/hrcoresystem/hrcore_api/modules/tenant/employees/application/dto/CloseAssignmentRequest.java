package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CloseAssignmentRequest(@NotNull LocalDate endDate) { }
