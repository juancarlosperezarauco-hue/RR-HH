package com.hrcoresystem.hrcore_api.modules.tenant.departments.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.model.Department;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.model.DepartmentStatus;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.repository.DepartmentRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.Employee;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.EmploymentStatus;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service.OrganizationalReportingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service.TenantTimezoneService;

@Component
public class DepartmentPolicy {
    private final DepartmentRepository departments;
    private final EmployeeRepository employees;
    private final TenantPlanEnforcementService plans;
    private final SecurityContextFacade security;
    private final TenantTimezoneService timezone;
    private final OrganizationalReportingService reporting;

    @Autowired
    public DepartmentPolicy(DepartmentRepository departments, EmployeeRepository employees, TenantPlanEnforcementService plans,
                            SecurityContextFacade security, TenantTimezoneService timezone,
                            OrganizationalReportingService reporting) {
        this.departments = departments;
        this.employees = employees;
        this.plans = plans;
        this.security = security;
        this.timezone = timezone;
        this.reporting = reporting;
    }

    public DepartmentPolicy(DepartmentRepository departments, EmployeeRepository employees, TenantPlanEnforcementService plans,
                            SecurityContextFacade security, TenantTimezoneService timezone) {
        this(departments, employees, plans, security, timezone, null);
    }

    /** Compatibility constructor for isolated unit tests that predate manager locking. */
    public DepartmentPolicy(DepartmentRepository departments, TenantPlanEnforcementService plans,
                            SecurityContextFacade security, TenantTimezoneService timezone) {
        this(departments, null, plans, security, timezone);
    }

    public void assertOperational() { plans.assertCanOperate(); }
    public void lockForWrite() { departments.lockHierarchy(); }

    public void validateReportingAfterChange() {
        if (reporting != null) reporting.validateNoCycles(timezone.today());
    }

    public Department require(UUID id) {
        return departments.findById(id).orElseThrow(() -> new ResourceNotFoundException("Department not found: " + id));
    }

    public UUID actorId() {
        String subject = security.getCurrentSubject();
        try { return UUID.fromString(subject); }
        catch (RuntimeException ex) { throw new BusinessException("Authenticated tenant user is required"); }
    }

    public String required(String value, int max, String field) {
        if (value == null || value.trim().isEmpty()) throw new BusinessException(field + " is required");
        String normalized = value.trim();
        if (normalized.length() > max) throw new BusinessException(field + " exceeds " + max + " characters");
        return normalized;
    }

    public String optional(String value, int max, String field) {
        if (value == null || value.trim().isEmpty()) return null;
        String normalized = value.trim();
        if (max > 0 && normalized.length() > max) throw new BusinessException(field + " exceeds " + max + " characters");
        return normalized;
    }

    public String requiredReason(String reason) { return required(reason, Integer.MAX_VALUE, "reason"); }

    public void assertCodeAvailable(String code, UUID id) {
        if (departments.existsByCodeOtherThan(code, id)) throw new BusinessException("Department code already exists");
    }

    public void validateReferences(UUID departmentId, UUID parentId, UUID managerId) {
        if (parentId != null) {
            if (parentId.equals(departmentId)) throw new BusinessException("Department cannot be its own parent");
            Department parent = require(parentId);
            if (parent.status() != DepartmentStatus.ACTIVE) throw new BusinessException("Parent department must be active");
            Set<UUID> visited = new HashSet<>();
            Department cursor = parent;
            while (cursor != null) {
                if (!visited.add(cursor.id()) || cursor.id().equals(departmentId))
                    throw new BusinessException("Department hierarchy contains a cycle");
                cursor = cursor.parentDepartmentId() == null ? null : require(cursor.parentDepartmentId());
            }
        }
        if (managerId != null) {
            if (employees == null) {
                if (!departments.existsActiveEmployee(managerId))
                    throw new BusinessException("Manager employee must exist and be active");
                return;
            }
            Employee manager = employees.findByIdForUpdate(managerId)
                    .orElseThrow(() -> new BusinessException("Manager employee must exist and be active"));
            if (manager.employmentStatus() != EmploymentStatus.ACTIVE)
                throw new BusinessException("Manager employee must exist and be active");
        }
    }

    public void validateDeactivation(UUID id) {
        if (departments.hasActiveChildren(id)) throw new BusinessException("Department has active child departments");
        if (departments.hasCurrentOrFutureAssignments(id, timezone.today())) throw new BusinessException("El departamento tiene asignaciones laborales vigentes o futuras");
    }
}
