package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.EmployeeRequest;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.*;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class EmployeePolicy {
    private final EmployeeRepository employees;
    private final EmployeeAssignmentRepository assignments;
    private final TenantUserRepository users;
    private final TenantPlanEnforcementService plans;
    private final SecurityContextFacade security;

    public EmployeePolicy(EmployeeRepository employees, EmployeeAssignmentRepository assignments,
                          TenantUserRepository users, TenantPlanEnforcementService plans,
                          SecurityContextFacade security) {
        this.employees = employees; this.assignments = assignments; this.users = users;
        this.plans = plans; this.security = security;
    }

    public void assertOperational() { plans.assertCanOperate(); }
    public Employee require(UUID id) {
        return employees.findById(id).orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
    }
    public Employee requireForUpdate(UUID id) {
        return employees.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
    }
    public UUID actorId() {
        try { return UUID.fromString(security.getCurrentSubject()); }
        catch (RuntimeException ex) { throw new BusinessException("Authenticated tenant user is required"); }
    }
    public void assertNotTerminated(Employee e) {
        if (e.employmentStatus() == EmploymentStatus.TERMINATED)
            throw new BusinessException("Terminated employees cannot be changed in this module");
    }

    public Employee normalized(UUID id, EmployeeRequest r, Employee before, UUID actor) {
        if (before != null) assertNotTerminated(before);
        String employeeNumber = required(r.employeeNumber(), 50, "employeeNumber");
        String documentType = required(r.documentType(), 30, "documentType");
        String documentNumber = required(r.documentNumber(), 50, "documentNumber");
        if (employees.existsNumberOtherThan(employeeNumber, id)) throw new BusinessException("Employee number already exists");
        if (employees.existsDocumentOtherThan(documentType, documentNumber, id))
            throw new BusinessException("Employee document already exists");
        if (r.tenantUserId() != null) {
            if (users.findById(r.tenantUserId()).isEmpty()) throw new BusinessException("Tenant user does not exist");
            if (employees.existsTenantUserOtherThan(r.tenantUserId(), id))
                throw new BusinessException("Tenant user is already linked to an employee");
        }
        if (r.hireDate() == null) throw new BusinessException("hireDate is required");
        if (r.birthDate() != null && r.birthDate().isAfter(LocalDate.now()))
            throw new BusinessException("birthDate cannot be in the future");
        if (r.birthDate() != null && r.hireDate().isBefore(r.birthDate()))
            throw new BusinessException("hireDate cannot precede birthDate");
        if (before != null && assignments.earliestStartDate(id).filter(d -> d.isBefore(r.hireDate())).isPresent())
            throw new BusinessException("hireDate cannot follow an existing assignment");
        return new Employee(id, r.tenantUserId(), employeeNumber,
                required(r.firstName(), 100, "firstName"), optional(r.middleName(), 100, "middleName"),
                required(r.lastName(), 100, "lastName"), optional(r.secondLastName(), 100, "secondLastName"),
                documentType, documentNumber, r.birthDate(), optional(r.gender(), 30, "gender"),
                optional(r.nationality(), 80, "nationality"), email(r.personalEmail(), "personalEmail"),
                email(r.workEmail(), "workEmail"), phone(r.phone(), false, "phone"),
                phone(r.mobilePhone(), false, "mobilePhone"), optional(r.address(), 0, "address"),
                r.hireDate(), before == null ? null : before.terminationDate(),
                before == null ? EmploymentStatus.ACTIVE : before.employmentStatus(),
                before == null ? actor : before.createdBy(), actor,
                before == null ? null : before.createdAt(), before == null ? null : before.updatedAt());
    }

    public Employee changedStatus(Employee before, EmploymentStatus requested, UUID actor) {
        assertNotTerminated(before);
        if (requested == null || requested == EmploymentStatus.TERMINATED)
            throw new BusinessException("Unsupported employment status");
        if (requested == EmploymentStatus.INACTIVE && assignments.hasOpenOrFutureAssignments(before.id()))
            throw new BusinessException("Employee has active or future assignments");
        return new Employee(before.id(), before.tenantUserId(), before.employeeNumber(), before.firstName(),
                before.middleName(), before.lastName(), before.secondLastName(), before.documentType(),
                before.documentNumber(), before.birthDate(), before.gender(), before.nationality(),
                before.personalEmail(), before.workEmail(), before.phone(), before.mobilePhone(),
                before.address(), before.hireDate(), before.terminationDate(), requested,
                before.createdBy(), actor, before.createdAt(), before.updatedAt());
    }

    public List<String> changedFields(Employee a, Employee b) {
        List<String> fields = new ArrayList<>();
        changed(fields, "tenantUserId", a.tenantUserId(), b.tenantUserId());
        changed(fields, "employeeNumber", a.employeeNumber(), b.employeeNumber());
        changed(fields, "firstName", a.firstName(), b.firstName());
        changed(fields, "middleName", a.middleName(), b.middleName());
        changed(fields, "lastName", a.lastName(), b.lastName());
        changed(fields, "secondLastName", a.secondLastName(), b.secondLastName());
        changed(fields, "documentType", a.documentType(), b.documentType());
        changed(fields, "documentNumber", a.documentNumber(), b.documentNumber());
        changed(fields, "birthDate", a.birthDate(), b.birthDate());
        changed(fields, "gender", a.gender(), b.gender());
        changed(fields, "nationality", a.nationality(), b.nationality());
        changed(fields, "personalEmail", a.personalEmail(), b.personalEmail());
        changed(fields, "workEmail", a.workEmail(), b.workEmail());
        changed(fields, "phone", a.phone(), b.phone());
        changed(fields, "mobilePhone", a.mobilePhone(), b.mobilePhone());
        changed(fields, "address", a.address(), b.address());
        changed(fields, "hireDate", a.hireDate(), b.hireDate());
        changed(fields, "employmentStatus", a.employmentStatus(), b.employmentStatus());
        return fields;
    }

    public String required(String value, int max, String field) {
        if (value == null || value.isBlank()) throw new BusinessException(field + " is required");
        return optional(value, max, field);
    }
    public String optional(String value, int max, String field) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (max > 0 && normalized.length() > max) throw new BusinessException(field + " exceeds " + max + " characters");
        return normalized;
    }
    public String phone(String value, boolean required, String field) {
        String result = required ? required(value, 40, field) : optional(value, 40, field);
        if (result != null && !result.matches("[+0-9() .-]{6,40}"))
            throw new BusinessException(field + " has an invalid format");
        return result;
    }
    private String email(String value, String field) {
        String result = optional(value, 150, field);
        if (result != null && !result.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new BusinessException(field + " has an invalid format");
        return result;
    }
    private void changed(List<String> fields, String field, Object a, Object b) {
        if (!Objects.equals(a, b)) fields.add(field);
    }
}
