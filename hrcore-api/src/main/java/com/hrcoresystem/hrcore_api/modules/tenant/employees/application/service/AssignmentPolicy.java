package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.model.DepartmentStatus;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.repository.DepartmentRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.model.PositionStatus;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.repository.PositionRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.EmployeeAssignmentRequest;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.*;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service.TenantTimezoneService;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class AssignmentPolicy {
    private final EmployeeAssignmentRepository assignments;
    private final EmployeeRepository employees;
    private final DepartmentRepository departments;
    private final PositionRepository positions;
    private final EmployeePolicy employeePolicy;
    private final TenantTimezoneService timezone;
    private final EmploymentPeriodService periods;

    @Autowired
    public AssignmentPolicy(EmployeeAssignmentRepository assignments, EmployeeRepository employees,
                            DepartmentRepository departments, PositionRepository positions, EmployeePolicy employeePolicy, TenantTimezoneService timezone, EmploymentPeriodService periods) {
        this.assignments = assignments; this.employees = employees; this.departments = departments;
        this.positions = positions; this.employeePolicy = employeePolicy; this.timezone = timezone; this.periods = periods;
    }
    /** Compatibilidad para pruebas aisladas; el contenedor usa el constructor completo. */
    public AssignmentPolicy(EmployeeAssignmentRepository assignments, EmployeeRepository employees,
            DepartmentRepository departments, PositionRepository positions, EmployeePolicy employeePolicy,
            TenantTimezoneService timezone) {
        this(assignments, employees, departments, positions, employeePolicy, timezone, null);
    }
    public EmployeeAssignment require(UUID employeeId, UUID assignmentId) {
        return assignments.findByIdAndEmployeeId(assignmentId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee assignment not found: " + assignmentId));
    }

    public EmployeeAssignment normalized(Employee employee, EmployeeAssignmentRequest r,
                                         EmployeeAssignment before, UUID actor) {
        if (employee.employmentStatus() != EmploymentStatus.ACTIVE)
            throw new BusinessException("Employee must be active for an assignment");
        if (before != null && (before.status() != EmployeeAssignmentStatus.ACTIVE || before.endDate() != null || !before.startDate().isAfter(timezone.today())))
            throw new BusinessException("Only future assignments may be edited");
        departments.lockHierarchy();
        if (r.departmentId() == null || departments.findById(r.departmentId())
                .filter(d -> d.status() == DepartmentStatus.ACTIVE).isEmpty())
            throw new BusinessException("Active department is required");
        if (r.positionId() == null || positions.findByIdForUpdate(r.positionId())
                .filter(p -> p.status() == PositionStatus.ACTIVE).isEmpty())
            throw new BusinessException("Active position is required");
        if (r.supervisorEmployeeId() != null) {
            if (r.supervisorEmployeeId().equals(employee.id()))
                throw new BusinessException("Employee cannot supervise their own assignment");
            if (employees.findById(r.supervisorEmployeeId())
                    .filter(e -> e.employmentStatus() == EmploymentStatus.ACTIVE).isEmpty())
                throw new BusinessException("Active supervisor is required");
        }
        if (r.startDate() == null || r.startDate().isBefore(employee.hireDate()))
            throw new BusinessException("startDate must be on or after hireDate");
        if (r.endDate() != null && r.endDate().isBefore(r.startDate()))
            throw new BusinessException("endDate must be on or after startDate");
        AssignmentType type = r.assignmentType() == null ? AssignmentType.PRIMARY : r.assignmentType();
        if (type == AssignmentType.PRIMARY && assignments.hasOverlappingPrimary(employee.id(),
                before == null ? null : before.id(), r.startDate(), r.endDate()))
            throw new BusinessException("Primary assignment overlaps an existing assignment");
        UUID periodId = periods == null ? null : periods.requireForDate(employee.id(), r.startDate()).id();
        return new EmployeeAssignment(before == null ? null : before.id(), employee.id(),
                r.departmentId(), r.positionId(), r.supervisorEmployeeId(), r.startDate(), r.endDate(), type,
                employeePolicy.optional(r.reason(), 255, "reason"), before == null ? actor : before.createdBy(),
                before == null ? null : before.createdAt(), before == null ? null : before.updatedAt(), EmployeeAssignmentStatus.ACTIVE, periodId);
    }

    public EmployeeAssignment closed(EmployeeAssignment before, LocalDate endDate) {
        if (before.endDate() != null) throw new BusinessException("Assignment is already closed");
        if (endDate == null || endDate.isBefore(before.startDate()) || endDate.isAfter(timezone.today()))
            throw new BusinessException("Invalid assignment endDate");
        return new EmployeeAssignment(before.id(), before.employeeId(), before.departmentId(), before.positionId(),
                before.supervisorEmployeeId(), before.startDate(), endDate, before.assignmentType(),
                before.reason(), before.createdBy(), before.createdAt(), before.updatedAt(), before.status(), before.employmentPeriodId());
    }

    public EmployeeAssignment cancelled(EmployeeAssignment before) {
        if (before.status() != EmployeeAssignmentStatus.ACTIVE)
            throw new BusinessException("Only active assignments may be cancelled");
        if (!before.startDate().isAfter(timezone.today()))
            throw new BusinessException("Only future assignments may be cancelled");
        return new EmployeeAssignment(before.id(), before.employeeId(), before.departmentId(), before.positionId(),
                before.supervisorEmployeeId(), before.startDate(), before.endDate(), before.assignmentType(),
                before.reason(), before.createdBy(), before.createdAt(), before.updatedAt(), EmployeeAssignmentStatus.CANCELLED, before.employmentPeriodId());
    }

    public EmployeeAssignment transferred(EmployeeAssignment before, LocalDate endDate) {
        if (before.status() != EmployeeAssignmentStatus.ACTIVE || before.endDate() != null)
            throw new BusinessException("Only an open active assignment may be transferred");
        if (endDate == null || !endDate.isAfter(before.startDate()))
            throw new BusinessException("Transfer date must follow the assignment start date");
        return new EmployeeAssignment(before.id(), before.employeeId(), before.departmentId(), before.positionId(),
                before.supervisorEmployeeId(), before.startDate(), endDate, before.assignmentType(), before.reason(),
                before.createdBy(), before.createdAt(), before.updatedAt(), EmployeeAssignmentStatus.ACTIVE, before.employmentPeriodId());
    }

    public List<String> changedFields(EmployeeAssignment a, EmployeeAssignment b) {
        List<String> fields = new ArrayList<>();
        changed(fields, "departmentId", a.departmentId(), b.departmentId());
        changed(fields, "positionId", a.positionId(), b.positionId());
        changed(fields, "supervisorEmployeeId", a.supervisorEmployeeId(), b.supervisorEmployeeId());
        changed(fields, "startDate", a.startDate(), b.startDate());
        changed(fields, "endDate", a.endDate(), b.endDate());
        changed(fields, "assignmentType", a.assignmentType(), b.assignmentType());
        changed(fields, "reason", a.reason(), b.reason());
        return fields;
    }
    private void changed(List<String> fields, String field, Object a, Object b) {
        if (!Objects.equals(a, b)) fields.add(field);
    }
}
