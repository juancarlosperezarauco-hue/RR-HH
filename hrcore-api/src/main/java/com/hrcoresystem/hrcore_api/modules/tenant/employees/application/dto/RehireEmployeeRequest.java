package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record RehireEmployeeRequest(@NotNull LocalDate startDate, String reason,
                                    @Valid EmployeeAssignmentRequest assignment) { }
