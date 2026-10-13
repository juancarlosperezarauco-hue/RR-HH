package com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.dto.AttendanceRequests;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.repository.AttendanceStore;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.Employee;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class AttendanceOvertimeServiceTest {
    private final UUID employeeId = UUID.randomUUID();
    private final UUID recordId = UUID.randomUUID();
    private final UUID overtimeId = UUID.randomUUID();
    private final UUID requesterId = UUID.randomUUID();
    private final UUID reviewerId = UUID.randomUUID();
    private final AttendanceStore store = mock(AttendanceStore.class);
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final AttendanceActor actor = mock(AttendanceActor.class);
    private final TenantPlanEnforcementService plans = mock(TenantPlanEnforcementService.class);
    private final AuditTrailService audit = mock(AuditTrailService.class);
    private AttendanceOvertimeService service;

    @BeforeEach
    void setUp() {
        service = new AttendanceOvertimeService(store, employees, actor, new AttendanceMapper(), plans, audit);
        var record = new AttendanceRecord(recordId, employeeId, null, LocalDate.of(2026, 1, 12),
                null, null, null, null, 0, 0, 0, AttendanceStatus.PENDING,
                null, null, Instant.now(), Instant.now());
        var request = new AttendanceOvertime(overtimeId, recordId, 60, null, "Overtime",
                ReviewStatus.PENDING, null, null, Instant.now(), Instant.now());
        when(store.overtime(overtimeId)).thenReturn(Optional.of(request));
        when(store.lockOvertime(overtimeId)).thenReturn(request);
        when(store.record(recordId)).thenReturn(Optional.of(record));
        when(store.lockOrCreate(employeeId, record.workDate())).thenReturn(record);
        when(employees.findByIdForUpdate(employeeId)).thenReturn(Optional.of(mock(Employee.class)));
        when(actor.id()).thenReturn(reviewerId);
        when(store.overtimeRequestActors(overtimeId)).thenReturn(List.of(requesterId));
        when(store.save(any(AttendanceOvertime.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void requesterCannotApproveTheirOwnOvertime() {
        when(actor.id()).thenReturn(requesterId);
        assertThrows(AccessDeniedException.class, () -> service.approve(overtimeId,
                new AttendanceRequests.OvertimeApproval(45, null)));
        verify(store, never()).save(any(AttendanceOvertime.class));
    }

    @Test
    void missingOrDuplicateRequesterAuditEvidenceBlocksReview() {
        when(store.overtimeRequestActors(overtimeId)).thenReturn(List.of());
        assertThrows(BusinessException.class, () -> service.approve(overtimeId,
                new AttendanceRequests.OvertimeApproval(45, null)));
        when(store.overtimeRequestActors(overtimeId)).thenReturn(List.of(requesterId, reviewerId));
        assertThrows(BusinessException.class, () -> service.approve(overtimeId,
                new AttendanceRequests.OvertimeApproval(45, null)));
        verify(store, never()).save(any(AttendanceOvertime.class));
    }

    @Test
    void approvalUsesReviewerIdentityAndBoundsApprovedMinutes() {
        assertThrows(BusinessException.class, () -> service.approve(overtimeId,
                new AttendanceRequests.OvertimeApproval(61, null)));
        var approved = service.approve(overtimeId, new AttendanceRequests.OvertimeApproval(45, "Reviewed"));
        assertEquals(ReviewStatus.APPROVED, approved.status());
        assertEquals(45, approved.approvedMinutes());
        assertEquals(reviewerId, approved.approvedBy());
    }
}
