package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.mapper;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.*;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper {
    public EmployeeResponse toResponse(Employee e) {
        return new EmployeeResponse(e.id(), e.tenantUserId(), e.employeeNumber(), e.firstName(),
                e.middleName(), e.lastName(), e.secondLastName(), e.documentType(), e.documentNumber(),
                e.birthDate(), e.gender(), e.nationality(), e.personalEmail(), e.workEmail(),
                e.phone(), e.mobilePhone(), e.address(), e.hireDate(), e.terminationDate(),
                e.employmentStatus(), e.createdAt(), e.updatedAt());
    }
    public EmployeeSummaryResponse toSummary(Employee e) {
        return new EmployeeSummaryResponse(e.id(), e.employeeNumber(), e.firstName(), e.lastName(), e.employmentStatus());
    }
    public EmployeeAssignmentResponse toResponse(EmployeeAssignment a) {
        return new EmployeeAssignmentResponse(a.id(), a.employeeId(), a.departmentId(), a.positionId(),
                a.supervisorEmployeeId(), a.startDate(), a.endDate(), a.assignmentType(), a.reason(),
                a.createdAt(), a.updatedAt());
    }
    public EmployeeEmergencyContactResponse toResponse(EmployeeEmergencyContact c) {
        return new EmployeeEmergencyContactResponse(c.id(), c.employeeId(), c.fullName(), c.relationship(),
                c.phone(), c.alternativePhone(), c.address(), c.primary(), c.createdAt(), c.updatedAt());
    }
}
