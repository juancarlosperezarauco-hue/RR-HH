package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto;
import java.time.LocalDate;
import java.util.List;
public record OrganizationChartResponse(LocalDate asOfDate,int totalEmployees,List<OrganizationChartNodeResponse> roots) {}
