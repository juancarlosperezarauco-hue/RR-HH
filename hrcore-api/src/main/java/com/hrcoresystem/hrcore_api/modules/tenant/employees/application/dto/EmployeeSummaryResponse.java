package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.EmploymentStatus;
import java.util.UUID;

public record EmployeeSummaryResponse(UUID id, String employeeNumber, String firstName,
                                      String lastName, EmploymentStatus employmentStatus) { }
