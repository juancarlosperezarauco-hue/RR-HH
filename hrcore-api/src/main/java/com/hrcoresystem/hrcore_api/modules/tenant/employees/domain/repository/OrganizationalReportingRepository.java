package com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.EmploymentStatus;

public interface OrganizationalReportingRepository {
    List<OrganizationalEmployee> findCurrent(LocalDate asOfDate);
    record OrganizationalEmployee(UUID employeeId,String employeeNumber,String fullName,EmploymentStatus status,UUID departmentId,String departmentName,UUID parentDepartmentId,UUID managerEmployeeId,UUID positionId,String positionName,UUID directSupervisorEmployeeId,boolean accountActive) {}
}
