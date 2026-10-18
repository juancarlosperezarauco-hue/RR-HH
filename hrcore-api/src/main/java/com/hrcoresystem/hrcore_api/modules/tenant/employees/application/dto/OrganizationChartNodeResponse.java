package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto;
import java.util.List;
import java.util.UUID;
public record OrganizationChartNodeResponse(UUID employeeId,String employeeNumber,String fullName,UUID departmentId,String departmentName,UUID positionId,String positionName,UUID supervisorEmployeeId,List<OrganizationChartNodeResponse> children) {}
