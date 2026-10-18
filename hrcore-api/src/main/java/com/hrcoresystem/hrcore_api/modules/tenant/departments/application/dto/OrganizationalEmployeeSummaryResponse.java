package com.hrcoresystem.hrcore_api.modules.tenant.departments.application.dto;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.EmploymentStatus; import java.util.UUID;
public record OrganizationalEmployeeSummaryResponse(UUID employeeId,String employeeNumber,String fullName,UUID departmentId,UUID positionId,EmploymentStatus employmentStatus) {}
