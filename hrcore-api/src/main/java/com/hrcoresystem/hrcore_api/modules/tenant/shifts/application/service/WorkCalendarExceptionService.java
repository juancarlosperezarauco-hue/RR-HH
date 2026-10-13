package com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.repository.DepartmentRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.WorkCalendarExceptionRequest;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.WorkCalendarExceptionResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.CalendarExceptionType;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.WorkCalendarException;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.WorkCalendarExceptionFilter;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.WorkCalendarExceptionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class WorkCalendarExceptionService {
    private final WorkCalendarExceptionRepository exceptions;
    private final EmployeeRepository employees;
    private final DepartmentRepository departments;
    private final TenantPlanEnforcementService plans;
    private final TenantShiftClock clock;
    private final SecurityContextFacade security;
    private final AuditTrailService audit;

    public WorkCalendarExceptionService(WorkCalendarExceptionRepository exceptions,
                                        EmployeeRepository employees, DepartmentRepository departments,
                                        TenantPlanEnforcementService plans, TenantShiftClock clock,
                                        SecurityContextFacade security, AuditTrailService audit) {
        this.exceptions = exceptions;
        this.employees = employees;
        this.departments = departments;
        this.plans = plans;
        this.clock = clock;
        this.security = security;
        this.audit = audit;
    }

    @Transactional
    public WorkCalendarExceptionResponse create(WorkCalendarExceptionRequest request) {
        plans.assertCanOperate();
        LocalDate today = clock.today();
        validate(request, today);
        exceptions.lockScopeDate(request.employeeId(), request.departmentId(), request.exceptionDate());
        assertAvailable(request.employeeId(), request.departmentId(), request.exceptionDate(), null);
        WorkCalendarException saved = exceptions.save(new WorkCalendarException(null, request.employeeId(),
                request.departmentId(), request.exceptionDate(), request.exceptionType(),
                normalizeDescription(request.description()), request.workRequired(), actor(), null));
        record("CREATED", saved.id(), List.of("employeeId", "departmentId", "exceptionDate",
                "exceptionType", "description", "workRequired"));
        return response(saved);
    }

    public Page<WorkCalendarExceptionResponse> list(WorkCalendarExceptionFilter filter, Pageable pageable) {
        plans.assertCanOperate();
        if (filter == null) throw new BusinessException("Calendar exception filter is required");
        if (filter.employeeId() != null && filter.departmentId() != null)
            throw new BusinessException("Only one calendar scope can be filtered at a time");
        if (Boolean.TRUE.equals(filter.organizationOnly())
                && (filter.employeeId() != null || filter.departmentId() != null))
            throw new BusinessException("Organization scope cannot be combined with employee or department");
        if (filter.fromDate() != null && filter.toDate() != null && filter.fromDate().isAfter(filter.toDate()))
            throw new BusinessException("fromDate cannot follow toDate");
        return exceptions.findAll(filter, pageable).map(this::response);
    }

    public WorkCalendarExceptionResponse get(UUID id) {
        plans.assertCanOperate();
        return response(require(id));
    }

    @Transactional
    public WorkCalendarExceptionResponse update(UUID id, WorkCalendarExceptionRequest request) {
        plans.assertCanOperate();
        LocalDate today = clock.today();
        validate(request, today);
        WorkCalendarException observed = require(id);
        assertFuture(observed.exceptionDate(), today);
        lockBoth(observed, request);
        WorkCalendarException before = requireFresh(id);
        if (!sameScopeAndDate(observed, before))
            throw new BusinessException("Calendar exception changed concurrently; retry the request");
        assertAvailable(request.employeeId(), request.departmentId(), request.exceptionDate(), id);
        WorkCalendarException after = new WorkCalendarException(id, request.employeeId(), request.departmentId(),
                request.exceptionDate(), request.exceptionType(), normalizeDescription(request.description()),
                request.workRequired(), before.createdBy(), before.createdAt());
        List<String> changed = changes(before, after);
        if (changed.isEmpty()) return response(before);
        WorkCalendarException saved = exceptions.save(after);
        record("UPDATED", saved.id(), changed);
        return response(saved);
    }

    @Transactional
    public void delete(UUID id) {
        plans.assertCanOperate();
        LocalDate today = clock.today();
        WorkCalendarException observed = require(id);
        assertFuture(observed.exceptionDate(), today);
        exceptions.lockScopeDate(observed.employeeId(), observed.departmentId(), observed.exceptionDate());
        WorkCalendarException current = requireFresh(id);
        if (!sameScopeAndDate(observed, current))
            throw new BusinessException("Calendar exception changed concurrently; retry the request");
        exceptions.delete(id);
        record("DELETED", id, List.of("deleted"));
    }

    private WorkCalendarException require(UUID id) {
        return exceptions.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Calendar exception not found: " + id));
    }

    private WorkCalendarException requireFresh(UUID id) {
        return exceptions.findByIdFresh(id)
                .orElseThrow(() -> new ResourceNotFoundException("Calendar exception not found: " + id));
    }

    private void validate(WorkCalendarExceptionRequest request, LocalDate today) {
        if (request == null) throw new BusinessException("Calendar exception request is required");
        if (request.employeeId() != null && request.departmentId() != null)
            throw new BusinessException("Employee and department scopes cannot be combined");
        if (request.exceptionDate() == null) throw new BusinessException("exceptionDate is required");
        assertFuture(request.exceptionDate(), today);
        if (request.exceptionType() == null) throw new BusinessException("exceptionType is required");
        if (request.workRequired() == null) throw new BusinessException("workRequired is required");
        if ((request.exceptionType() == CalendarExceptionType.HOLIDAY
                || request.exceptionType() == CalendarExceptionType.NON_WORKING_DAY)
                && request.workRequired())
            throw new BusinessException(request.exceptionType() + " requires workRequired=false");
        if (request.exceptionType() == CalendarExceptionType.SPECIAL_WORKING_DAY
                && !request.workRequired())
            throw new BusinessException("SPECIAL_WORKING_DAY requires workRequired=true");
        if (request.employeeId() != null && employees.findById(request.employeeId()).isEmpty())
            throw new BusinessException("Employee does not exist in this tenant");
        if (request.departmentId() != null && departments.findById(request.departmentId()).isEmpty())
            throw new BusinessException("Department does not exist in this tenant");
    }

    private void assertFuture(LocalDate date, LocalDate today) {
        if (!date.isAfter(today)) throw new BusinessException("Calendar exception date must be after today");
    }

    private void assertAvailable(UUID employeeId, UUID departmentId, LocalDate date, UUID exceptId) {
        if (exceptions.findByScopeAndDate(employeeId, departmentId, date).stream()
                .anyMatch(e -> !e.id().equals(exceptId)))
            throw new BusinessException("Calendar exception already exists for this scope and date");
    }

    private void lockBoth(WorkCalendarException before, WorkCalendarExceptionRequest after) {
        String oldKey = key(before.employeeId(), before.departmentId(), before.exceptionDate());
        String newKey = key(after.employeeId(), after.departmentId(), after.exceptionDate());
        if (oldKey.compareTo(newKey) <= 0) {
            exceptions.lockScopeDate(before.employeeId(), before.departmentId(), before.exceptionDate());
            if (!oldKey.equals(newKey))
                exceptions.lockScopeDate(after.employeeId(), after.departmentId(), after.exceptionDate());
        } else {
            exceptions.lockScopeDate(after.employeeId(), after.departmentId(), after.exceptionDate());
            exceptions.lockScopeDate(before.employeeId(), before.departmentId(), before.exceptionDate());
        }
    }

    private String key(UUID employeeId, UUID departmentId, LocalDate date) {
        return (employeeId != null ? "employee:" + employeeId
                : departmentId != null ? "department:" + departmentId : "organization") + ":" + date;
    }

    private boolean sameScopeAndDate(WorkCalendarException a, WorkCalendarException b) {
        return Objects.equals(a.employeeId(), b.employeeId())
                && Objects.equals(a.departmentId(), b.departmentId())
                && a.exceptionDate().equals(b.exceptionDate());
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) return null;
        return description.trim();
    }

    private UUID actor() {
        try { return UUID.fromString(security.getCurrentSubject()); }
        catch (RuntimeException ex) { throw new BusinessException("Authenticated tenant user is required"); }
    }

    private List<String> changes(WorkCalendarException a, WorkCalendarException b) {
        List<String> changed = new ArrayList<>();
        if (!Objects.equals(a.employeeId(), b.employeeId())) changed.add("employeeId");
        if (!Objects.equals(a.departmentId(), b.departmentId())) changed.add("departmentId");
        if (!a.exceptionDate().equals(b.exceptionDate())) changed.add("exceptionDate");
        if (a.exceptionType() != b.exceptionType()) changed.add("exceptionType");
        if (!Objects.equals(a.description(), b.description())) changed.add("description");
        if (a.workRequired() != b.workRequired()) changed.add("workRequired");
        return changed;
    }

    private void record(String action, UUID id, List<String> changed) {
        audit.recordTenantEvent("WORK_CALENDAR_EXCEPTION_" + action, "WORK_CALENDAR_EXCEPTION",
                id.toString(), Map.of("changedFields", changed));
    }

    private WorkCalendarExceptionResponse response(WorkCalendarException value) {
        return new WorkCalendarExceptionResponse(value.id(), value.employeeId(), value.departmentId(),
                value.exceptionDate(), value.exceptionType(), value.description(),
                value.workRequired(), value.createdAt());
    }
}
