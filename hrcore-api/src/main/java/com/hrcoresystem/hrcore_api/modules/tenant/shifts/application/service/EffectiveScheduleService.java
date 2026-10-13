package com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.ScheduleDetermination;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.ScheduleInterval;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.ScheduleResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.CalendarExceptionType;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.WorkCalendarException;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.EmployeeDepartmentAtDateRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.ShiftAssignmentRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.WorkCalendarExceptionRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.WorkShiftRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class EffectiveScheduleService {
    private final EmployeeRepository employees;
    private final EmployeeDepartmentAtDateRepository placements;
    private final WorkCalendarExceptionRepository exceptions;
    private final ShiftAssignmentRepository assignments;
    private final WorkShiftRepository shifts;
    private final TenantPlanEnforcementService plans;

    public EffectiveScheduleService(EmployeeRepository employees,
                                    EmployeeDepartmentAtDateRepository placements,
                                    WorkCalendarExceptionRepository exceptions,
                                    ShiftAssignmentRepository assignments,
                                    WorkShiftRepository shifts,
                                    TenantPlanEnforcementService plans) {
        this.employees = employees;
        this.placements = placements;
        this.exceptions = exceptions;
        this.assignments = assignments;
        this.shifts = shifts;
        this.plans = plans;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ScheduleResponse get(UUID employeeId, LocalDate date) {
        plans.assertCanOperate();
        if (employeeId == null || date == null) throw new BusinessException("employeeId and date are required");
        if (employees.findById(employeeId).isEmpty())
            throw new ResourceNotFoundException("Employee not found: " + employeeId);

        List<WorkCalendarException> employeeExceptions = exceptions.findByScopeAndDate(employeeId, null, date);
        if (employeeExceptions.size() > 1)
            return indeterminate(employeeId, date, null, null, "EMPLOYEE_EXCEPTION", "DUPLICATE_EXCEPTION");
        if (employeeExceptions.size() == 1)
            return applyException(employeeId, date, null, employeeExceptions.getFirst(), "EMPLOYEE_EXCEPTION");

        DepartmentResolution department = resolveDepartment(placements.findAtDate(employeeId, date));
        if (department.ambiguous())
            return indeterminate(employeeId, date, null, null, "DEPARTMENT_RESOLUTION", "AMBIGUOUS_DEPARTMENT");
        if (department.id() != null) {
            List<WorkCalendarException> departmentExceptions = exceptions.findByScopeAndDate(
                    null, department.id(), date);
            if (departmentExceptions.size() > 1)
                return indeterminate(employeeId, date, department.id(), null,
                        "DEPARTMENT_EXCEPTION", "DUPLICATE_EXCEPTION");
            if (departmentExceptions.size() == 1)
                return applyException(employeeId, date, department.id(), departmentExceptions.getFirst(),
                        "DEPARTMENT_EXCEPTION");
        }

        List<WorkCalendarException> organizationExceptions = exceptions.findByScopeAndDate(null, null, date);
        if (organizationExceptions.size() > 1)
            return indeterminate(employeeId, date, department.id(), null,
                    "ORGANIZATION_EXCEPTION", "DUPLICATE_EXCEPTION");
        if (organizationExceptions.size() == 1)
            return applyException(employeeId, date, department.id(), organizationExceptions.getFirst(),
                    "ORGANIZATION_EXCEPTION");
        return usualSchedule(employeeId, date, department.id(), null, "USUAL_ASSIGNMENT");
    }

    private ScheduleResponse applyException(UUID employeeId, LocalDate date, UUID departmentId,
                                            WorkCalendarException exception, String source) {
        if ((exception.exceptionType() == CalendarExceptionType.HOLIDAY
                || exception.exceptionType() == CalendarExceptionType.NON_WORKING_DAY)
                && exception.workRequired())
            return indeterminate(employeeId, date, departmentId, exception.id(), source,
                    "INVALID_EXCEPTION_CONFIGURATION");
        if (exception.exceptionType() == CalendarExceptionType.SPECIAL_WORKING_DAY
                && !exception.workRequired())
            return indeterminate(employeeId, date, departmentId, exception.id(), source,
                    "INVALID_EXCEPTION_CONFIGURATION");
        if (!exception.workRequired())
            return new ScheduleResponse(employeeId, date, departmentId, exception.id(), source,
                    ScheduleDetermination.NO_WORK, null, List.of());
        if (exception.exceptionType() == CalendarExceptionType.SCHEDULE_CHANGE)
            return indeterminate(employeeId, date, departmentId, exception.id(), source,
                    "SCHEDULE_CHANGE_WITHOUT_HOURS");
        ScheduleResponse usual = usualSchedule(employeeId, date, departmentId, exception.id(), source);
        if (usual.determination() == ScheduleDetermination.NO_WORK)
            return indeterminate(employeeId, date, departmentId, exception.id(), source,
                    "REQUIRED_WORK_WITHOUT_SHIFT");
        return usual;
    }

    private ScheduleResponse usualSchedule(UUID employeeId, LocalDate date, UUID departmentId,
                                           UUID exceptionId, String source) {
        var effective = assignments.findEffectiveOn(employeeId, date);
        if (effective.isEmpty())
            return new ScheduleResponse(employeeId, date, departmentId, exceptionId, source,
                    ScheduleDetermination.NO_WORK, null, List.of());
        List<ScheduleInterval> intervals = new ArrayList<>();
        for (var assignment : effective) {
            var shift = shifts.findById(assignment.shiftId());
            if (shift.isEmpty())
                return indeterminate(employeeId, date, departmentId, exceptionId, source,
                        "MISSING_SHIFT");
            try {
                var interval = ShiftTimeRules.shiftInterval(shift.get(), date);
                intervals.add(new ScheduleInterval(assignment.shiftId(), interval.start(), interval.end()));
            } catch (BusinessException invalidShift) {
                return indeterminate(employeeId, date, departmentId, exceptionId, source,
                        "INVALID_SHIFT_HOURS");
            }
        }
        intervals.sort(Comparator.comparing(ScheduleInterval::start).thenComparing(ScheduleInterval::end));
        for (int i = 1; i < intervals.size(); i++) {
            if (intervals.get(i - 1).end().isAfter(intervals.get(i).start()))
                return indeterminate(employeeId, date, departmentId, exceptionId, source,
                        "OVERLAPPING_SHIFTS");
        }
        if (!date.equals(LocalDate.MIN)) {
            LocalDate previousDate = date.minusDays(1);
            for (var previous : assignments.findEffectiveOn(employeeId, previousDate)) {
                var previousShift = shifts.findById(previous.shiftId());
                if (previousShift.isEmpty())
                    return indeterminate(employeeId, date, departmentId, exceptionId, source,
                            "MISSING_SHIFT");
                if (!previousShift.get().crossesMidnight()) continue;
                ShiftTimeRules.LocalInterval prior;
                try {
                    prior = ShiftTimeRules.shiftInterval(previousShift.get(), previousDate);
                } catch (BusinessException invalidShift) {
                    return indeterminate(employeeId, date, departmentId, exceptionId, source,
                            "INVALID_SHIFT_HOURS");
                }
                for (ScheduleInterval current : intervals) {
                    if (prior.start().isBefore(current.end())
                            && current.start().isBefore(prior.end()))
                        return indeterminate(employeeId, date, departmentId, exceptionId, source,
                                "OVERLAPPING_SHIFTS");
                }
            }
        }
        return new ScheduleResponse(employeeId, date, departmentId, exceptionId, source,
                ScheduleDetermination.EXACT, null, List.copyOf(intervals));
    }

    private DepartmentResolution resolveDepartment(List<EmployeeDepartmentAtDateRepository.Placement> values) {
        List<EmployeeDepartmentAtDateRepository.Placement> primary = values.stream()
                .filter(EmployeeDepartmentAtDateRepository.Placement::primary).toList();
        if (primary.size() == 1) return new DepartmentResolution(primary.getFirst().departmentId(), false);
        if (primary.size() > 1) return new DepartmentResolution(null, true);
        if (values.size() == 1) return new DepartmentResolution(values.getFirst().departmentId(), false);
        if (values.isEmpty()) return new DepartmentResolution(null, false);
        return new DepartmentResolution(null, true);
    }

    private ScheduleResponse indeterminate(UUID employeeId, LocalDate date, UUID departmentId,
                                           UUID exceptionId, String source, String reason) {
        return new ScheduleResponse(employeeId, date, departmentId, exceptionId, source,
                ScheduleDetermination.INDETERMINATE, reason, List.of());
    }

    private record DepartmentResolution(UUID id, boolean ambiguous) { }
}
