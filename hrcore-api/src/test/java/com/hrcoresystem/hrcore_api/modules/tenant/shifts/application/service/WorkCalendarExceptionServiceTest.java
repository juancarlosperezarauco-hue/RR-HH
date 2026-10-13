package com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.repository.DepartmentRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.WorkCalendarExceptionRequest;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.CalendarExceptionType;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.WorkCalendarException;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.WorkCalendarExceptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorkCalendarExceptionServiceTest {
    private final WorkCalendarExceptionRepository repository = mock(WorkCalendarExceptionRepository.class);
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final DepartmentRepository departments = mock(DepartmentRepository.class);
    private final TenantPlanEnforcementService plans = mock(TenantPlanEnforcementService.class);
    private final TenantShiftClock clock = mock(TenantShiftClock.class);
    private final SecurityContextFacade security = mock(SecurityContextFacade.class);
    private final AuditTrailService audit = mock(AuditTrailService.class);
    private final WorkCalendarExceptionService service = new WorkCalendarExceptionService(
            repository, employees, departments, plans, clock, security, audit);
    private final LocalDate today = LocalDate.of(2026, 10, 13);

    @BeforeEach
    void setUp() {
        when(clock.today()).thenReturn(today);
    }

    @Test
    void onlyFutureDatesAndStrictTypeFlagsAreAccepted() {
        assertThrows(BusinessException.class, () -> service.create(
                request(today, CalendarExceptionType.OTHER, true)));
        assertThrows(BusinessException.class, () -> service.create(
                request(today.plusDays(1), CalendarExceptionType.HOLIDAY, true)));
        assertThrows(BusinessException.class, () -> service.create(
                request(today.plusDays(1), CalendarExceptionType.NON_WORKING_DAY, true)));
        assertThrows(BusinessException.class, () -> service.create(
                request(today.plusDays(1), CalendarExceptionType.SPECIAL_WORKING_DAY, false)));
        verify(repository, never()).save(any());
        verifyNoInteractions(audit);
    }

    @Test
    void duplicateExactScopeAndDateIsRejectedAfterLock() {
        LocalDate date = today.plusDays(1);
        when(repository.findByScopeAndDate(null, null, date)).thenReturn(List.of(existing(date)));
        assertThrows(BusinessException.class, () -> service.create(
                request(date, CalendarExceptionType.OTHER, true)));
        verify(repository).lockScopeDate(null, null, date);
        verify(repository, never()).save(any());
        verifyNoInteractions(audit);
    }

    @Test
    void unchangedUpdateDoesNotWriteOrAudit() {
        LocalDate date = today.plusDays(1);
        WorkCalendarException current = existing(date);
        when(repository.findById(current.id())).thenReturn(Optional.of(current));
        when(repository.findByIdFresh(current.id())).thenReturn(Optional.of(current));
        service.update(current.id(), request(date, CalendarExceptionType.OTHER, true));
        verify(repository, never()).save(any());
        verifyNoInteractions(audit);
    }

    @Test
    void concurrentScopeMoveIsDetectedAfterAdvisoryLock() {
        LocalDate date = today.plusDays(1);
        WorkCalendarException observed = existing(date);
        WorkCalendarException moved = new WorkCalendarException(observed.id(), null, null,
                date.plusDays(1), observed.exceptionType(), observed.description(),
                observed.workRequired(), observed.createdBy(), observed.createdAt());
        when(repository.findById(observed.id())).thenReturn(Optional.of(observed));
        when(repository.findByIdFresh(observed.id())).thenReturn(Optional.of(moved));
        assertThrows(BusinessException.class, () -> service.update(observed.id(),
                request(date, CalendarExceptionType.OTHER, true)));
        verify(repository, never()).save(any());
        verifyNoInteractions(audit);
    }

    @Test
    void createWritesAndAuditsOnce() {
        LocalDate date = today.plusDays(1);
        UUID actor = UUID.randomUUID();
        UUID generated = UUID.randomUUID();
        when(security.getCurrentSubject()).thenReturn(actor.toString());
        when(repository.save(any())).thenAnswer(invocation -> {
            WorkCalendarException submitted = invocation.getArgument(0);
            return new WorkCalendarException(generated, submitted.employeeId(), submitted.departmentId(),
                    submitted.exceptionDate(), submitted.exceptionType(), submitted.description(),
                    submitted.workRequired(), submitted.createdBy(), null);
        });
        var result = service.create(request(date, CalendarExceptionType.OTHER, true));
        assertEquals(generated, result.id());
        verify(repository).lockScopeDate(null, null, date);
        verify(audit).recordTenantEvent(eq("WORK_CALENDAR_EXCEPTION_CREATED"),
                eq("WORK_CALENDAR_EXCEPTION"), eq(generated.toString()), any());
    }

    private WorkCalendarExceptionRequest request(LocalDate date, CalendarExceptionType type, boolean required) {
        return new WorkCalendarExceptionRequest(null, null, date, type, null, required);
    }

    private WorkCalendarException existing(LocalDate date) {
        return new WorkCalendarException(UUID.randomUUID(), null, null, date,
                CalendarExceptionType.OTHER, null, true, UUID.randomUUID(), null);
    }
}
