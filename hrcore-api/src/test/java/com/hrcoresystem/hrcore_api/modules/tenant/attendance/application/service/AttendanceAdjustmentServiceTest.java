package com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
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

class AttendanceAdjustmentServiceTest {
    private final UUID employeeId = UUID.randomUUID();
    private final UUID recordId = UUID.randomUUID();
    private final UUID adjustmentId = UUID.randomUUID();
    private final UUID requesterId = UUID.randomUUID();
    private final UUID reviewerId = UUID.randomUUID();
    private final AttendanceStore store = mock(AttendanceStore.class);
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final AttendanceCalculationService calculations = mock(AttendanceCalculationService.class);
    private final AttendanceActor actor = mock(AttendanceActor.class);
    private final AttendanceZone zones = mock(AttendanceZone.class);
    private final TenantPlanEnforcementService plans = mock(TenantPlanEnforcementService.class);
    private final AuditTrailService audit = mock(AuditTrailService.class);
    private final ObjectMapper json = new ObjectMapper();
    private AttendanceAdjustmentService service;
    private AttendanceRecord record;

    @BeforeEach
    void setUp() {
        service = new AttendanceAdjustmentService(store, employees, calculations, actor, zones,
                new AttendanceMapper(), plans, audit, json);
        record = new AttendanceRecord(recordId, employeeId, null, LocalDate.of(2026, 1, 12),
                null, null, null, null, 0, 0, 0, AttendanceStatus.PENDING,
                null, null, Instant.now(), Instant.now());
        when(store.record(recordId)).thenReturn(Optional.of(record));
        when(store.lockOrCreate(employeeId, record.workDate())).thenReturn(record);
        when(employees.findByIdForUpdate(employeeId)).thenReturn(Optional.of(mock(Employee.class)));
        when(store.adjustments(recordId)).thenReturn(List.of());
        when(actor.id()).thenReturn(requesterId);
    }

    @Test
    void requesterCannotApproveTheirOwnAdjustment() {
        var pending = new AttendanceAdjustment(adjustmentId, recordId, AdjustmentType.OTHER, "Reason",
                null, json.createObjectNode().put("status", "JUSTIFIED"), ReviewStatus.PENDING,
                requesterId, null, null, null, null, Instant.now(), Instant.now());
        when(store.adjustment(adjustmentId)).thenReturn(Optional.of(pending));
        when(store.lockAdjustment(adjustmentId)).thenReturn(pending);
        assertThrows(AccessDeniedException.class, () -> service.approve(adjustmentId, null));
        verify(store, never()).save(any(AttendanceAdjustment.class));
    }

    @Test
    void requestedValuesCannotModifyEmployeeOrUnrelatedFields() {
        var illegal = json.createObjectNode().put("status", "JUSTIFIED").put("employeeId", employeeId.toString());
        assertThrows(BusinessException.class, () -> service.create(new AttendanceRequests.Adjustment(
                recordId, AdjustmentType.OTHER, "Reason", illegal)));
        verify(store, never()).save(any(AttendanceAdjustment.class));
    }

    @Test
    void approvalAppliesJustificationWithoutChangingOriginalPunches() {
        var pending = pending(AdjustmentType.OTHER, "JUSTIFIED");
        when(actor.id()).thenReturn(reviewerId);
        when(store.adjustment(adjustmentId)).thenReturn(Optional.of(pending));
        when(store.lockAdjustment(adjustmentId)).thenReturn(pending);
        when(store.save(any(AttendanceAdjustment.class))).thenAnswer(i -> i.getArgument(0));
        var justified = new AttendanceRecord(record.id(), employeeId, null, record.workDate(),
                null, null, null, null, 0, 0, 0, AttendanceStatus.JUSTIFIED,
                null, Instant.now(), record.createdAt(), Instant.now());
        when(calculations.recalculateLocked(record)).thenReturn(justified);
        var result = service.approve(adjustmentId, "Reviewed");
        assertEquals(ReviewStatus.APPROVED, result.status());
        assertEquals(reviewerId, result.reviewedBy());
        assertNotNull(result.appliedAt());
        verify(audit).recordTenantEvent(eq("ATTENDANCE_ADJUSTMENT_APPLIED"),
                eq("ATTENDANCE_ADJUSTMENT"), eq(adjustmentId.toString()), any(), any(), any());
        verify(store, never()).associatePunch(any(UUID.class), any(UUID.class));
    }

    @Test
    void statusCorrectionCannotHideIncompletePunches() {
        var pending = pending(AdjustmentType.STATUS_CORRECTION, "LATE");
        when(actor.id()).thenReturn(reviewerId);
        when(store.adjustment(adjustmentId)).thenReturn(Optional.of(pending));
        when(store.lockAdjustment(adjustmentId)).thenReturn(pending);
        when(store.save(any(AttendanceAdjustment.class))).thenAnswer(i -> i.getArgument(0));
        var incomplete = new AttendanceRecord(record.id(), employeeId, null, record.workDate(),
                null, null, null, null, 0, 12, 0, AttendanceStatus.INCOMPLETE,
                json.createObjectNode().put("reason", "OPEN_IN"), Instant.now(), record.createdAt(), Instant.now());
        when(calculations.recalculateLocked(record)).thenReturn(incomplete);
        assertThrows(BusinessException.class, () -> service.approve(adjustmentId, null));
        verify(audit, never()).recordTenantEvent(eq("ATTENDANCE_ADJUSTMENT_APPLIED"),
                any(), any(), any(), any(), any());
    }

    private AttendanceAdjustment pending(AdjustmentType type, String status) {
        return new AttendanceAdjustment(adjustmentId, recordId, type, "Reason",
                json.createObjectNode().putNull("calculatedAt"),
                json.createObjectNode().put("status", status), ReviewStatus.PENDING,
                requesterId, null, null, null, null, Instant.now(), Instant.now());
    }
}
