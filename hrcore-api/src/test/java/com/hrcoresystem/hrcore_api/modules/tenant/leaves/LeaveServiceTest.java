package com.hrcoresystem.hrcore_api.modules.tenant.leaves;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.repository.TenantSettingRepository;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSetting;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.Employee;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.leaves.application.service.*;
import com.hrcoresystem.hrcore_api.modules.tenant.leaves.application.dto.LeaveDtos.*;
import com.hrcoresystem.hrcore_api.modules.tenant.leaves.domain.model.LeaveModels.*;
import com.hrcoresystem.hrcore_api.modules.tenant.leaves.domain.repository.LeaveStore;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LeaveServiceTest {
    private final UUID employeeId = UUID.randomUUID(), typeId = UUID.randomUUID(),
            requestId = UUID.randomUUID(), reviewerId = UUID.randomUUID(), requesterId = UUID.randomUUID();
    private final LeaveStore store = mock(LeaveStore.class);
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final LeaveDayCalculator calculator = mock(LeaveDayCalculator.class);
    private final LeaveAttachmentService attachments = mock(LeaveAttachmentService.class);
    private final TenantSettingRepository settings = mock(TenantSettingRepository.class);
    private final TenantPlanEnforcementService plans = mock(TenantPlanEnforcementService.class);
    private final SecurityContextFacade security = mock(SecurityContextFacade.class);
    private final AuditTrailService audit = mock(AuditTrailService.class);
    private final LeaveService service = new LeaveService(store, employees, calculator, attachments, settings,
            plans, security, audit);
    private LeaveBalance balance;

    @BeforeEach void setUp() {
        balance = new LeaveBalance(UUID.randomUUID(), employeeId, typeId, 2026,
                new BigDecimal("5.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null);
        when(employees.findByIdForUpdate(employeeId)).thenReturn(Optional.of(mock(Employee.class)));
        when(store.lockedBalance(employeeId, typeId, 2026)).thenReturn(Optional.of(balance));
        when(store.pendingDays(employeeId, typeId, 2026)).thenReturn(BigDecimal.ZERO);
        when(store.conflictingCandidates(eq(employeeId), any(), any(), eq(requestId))).thenReturn(List.of());
        when(store.save(any(LeaveRequest.class))).thenAnswer(i -> i.getArgument(0));
        when(store.save(any(LeaveBalance.class))).thenAnswer(i -> i.getArgument(0));
        when(store.save(any(LeaveRequestHistory.class))).thenAnswer(i -> i.getArgument(0));
        when(security.getCurrentSubject()).thenReturn(reviewerId.toString());
        when(calculator.calculate(eq(employeeId), any(), any(), isNull(), isNull()))
                .thenReturn(new BigDecimal("4.00"));
    }

    @Test void pendingDoesNotConsumeBalance() {
        LeaveRequest draft = request(RequestStatus.DRAFT, requesterId);
        when(store.request(requestId)).thenReturn(Optional.of(draft));
        when(store.lockedRequest(requestId)).thenReturn(Optional.of(draft));
        when(store.lockedType(typeId)).thenReturn(Optional.of(type(true)));
        assertEquals(RequestStatus.PENDING, service.submit(requestId).status());
        verify(store, never()).save(any(LeaveBalance.class));
        verify(store).save(argThat((LeaveRequestHistory h) -> h.previousStatus() == RequestStatus.DRAFT
                && h.newStatus() == RequestStatus.PENDING));
    }

    @Test void automaticApprovalConsumesExactlyOnceAndHasNoHumanReviewer() {
        LeaveRequest draft = request(RequestStatus.DRAFT, requesterId);
        when(store.request(requestId)).thenReturn(Optional.of(draft));
        when(store.lockedRequest(requestId)).thenReturn(Optional.of(draft));
        when(store.lockedType(typeId)).thenReturn(Optional.of(type(false)));
        var approved = service.submit(requestId);
        assertEquals(RequestStatus.APPROVED, approved.status());
        assertNull(approved.reviewedBy());
        verify(store).save(argThat((LeaveBalance b) -> b.usedDays().compareTo(new BigDecimal("4.00")) == 0));
        verify(store, times(2)).save(any(LeaveRequestHistory.class));
    }

    @Test void requesterCannotManuallyApprove() {
        LeaveRequest pending = request(RequestStatus.PENDING, reviewerId);
        when(store.request(requestId)).thenReturn(Optional.of(pending));
        when(store.lockedRequest(requestId)).thenReturn(Optional.of(pending));
        when(store.type(typeId)).thenReturn(Optional.of(type(true)));
        assertThrows(BusinessException.class, () -> service.approve(requestId, null));
        verify(store, never()).save(any(LeaveBalance.class));
    }

    @Test void approvalRechecksRealBalance() {
        LeaveRequest pending = request(RequestStatus.PENDING, requesterId);
        when(store.request(requestId)).thenReturn(Optional.of(pending));
        when(store.lockedRequest(requestId)).thenReturn(Optional.of(pending));
        when(store.type(typeId)).thenReturn(Optional.of(type(true)));
        balance = new LeaveBalance(balance.id(), employeeId, typeId, 2026,
                new BigDecimal("3.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null);
        when(store.lockedBalance(employeeId, typeId, 2026)).thenReturn(Optional.of(balance));
        assertThrows(BusinessException.class, () -> service.approve(requestId, null));
        verify(store, never()).save(any(LeaveBalance.class));
    }

    @Test void approvedCancellationRefundsOriginalDaysOnceBeforeStart() {
        LocalDate future = LocalDate.now(ZoneId.of("America/La_Paz")).plusDays(2);
        LeaveRequest approved = new LeaveRequest(requestId, employeeId, typeId, future,
                future.plusDays(3), null, null, new BigDecimal("4.00"), "Vacation",
                RequestStatus.APPROVED, requesterId, null, reviewerId, null, null, null, null);
        when(store.request(requestId)).thenReturn(Optional.of(approved));
        when(store.lockedRequest(requestId)).thenReturn(Optional.of(approved));
        when(store.type(typeId)).thenReturn(Optional.of(type(true)));
        when(settings.findByKey("company.timezone")).thenReturn(Optional.of(
                new TenantSetting(UUID.randomUUID(), "company.timezone", "America/La_Paz", null, null)));
        LeaveBalance used = new LeaveBalance(balance.id(), employeeId, typeId, future.getYear(),
                new BigDecimal("5.00"), BigDecimal.ZERO, new BigDecimal("4.00"), BigDecimal.ZERO, null);
        when(store.lockedBalance(employeeId, typeId, future.getYear())).thenReturn(Optional.of(used));
        assertEquals(RequestStatus.CANCELLED, service.cancel(requestId, "Cancelled").status());
        verify(store).save(argThat((LeaveBalance b) -> b.usedDays().compareTo(BigDecimal.ZERO) == 0));
        assertThrows(BusinessException.class, () -> {
            when(store.lockedRequest(requestId)).thenReturn(Optional.of(new LeaveRequest(requestId,
                    employeeId, typeId, future, future.plusDays(3), null, null,
                    new BigDecimal("4.00"), "Vacation", RequestStatus.CANCELLED,
                    requesterId, null, reviewerId, null, null, null, null)));
            service.cancel(requestId, null);
        });
    }

    @Test void conflictingPendingRequestBlocksApproval() {
        LeaveRequest pending = request(RequestStatus.PENDING, requesterId);
        when(store.request(requestId)).thenReturn(Optional.of(pending));
        when(store.lockedRequest(requestId)).thenReturn(Optional.of(pending));
        when(store.type(typeId)).thenReturn(Optional.of(type(true)));
        when(store.conflictingCandidates(eq(employeeId), any(), any(), eq(requestId)))
                .thenReturn(List.of(new LeaveRequest(UUID.randomUUID(), employeeId, typeId,
                        pending.startDate(), pending.startDate(), null, null, BigDecimal.ONE,
                        "Other", RequestStatus.PENDING, requesterId, null, null, null, null, null, null)));
        assertThrows(BusinessException.class, () -> service.approve(requestId, null));
        verify(store, never()).save(any(LeaveBalance.class));
    }

    @Test void usedTypeCannotChangeItsBalancePolicy() {
        when(store.lockedType(typeId)).thenReturn(Optional.of(type(true)));
        when(store.typeHasRelevantRequests(typeId)).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.updateType(typeId,
                new TypeInput("VAC", "Vacation", null, true, true, false, false)));
        verify(store, never()).save(any(LeaveType.class));
    }

    @Test void duplicateBalanceAndNegativeAdjustmentAreRejected() {
        when(store.type(typeId)).thenReturn(Optional.of(type(true)));
        when(store.balanceExists(employeeId, typeId, 2026)).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.createBalance(
                new BalanceInput(employeeId, typeId, 2026, new BigDecimal("5.00"))));
        when(store.balance(balance.id())).thenReturn(Optional.of(balance));
        when(store.pendingDays(employeeId, typeId, 2026)).thenReturn(new BigDecimal("4.00"));
        assertThrows(BusinessException.class, () -> service.adjustBalance(balance.id(),
                new BalanceAdjustment(new BigDecimal("-2.00"), "Correction")));
        verify(store, never()).save(any(LeaveBalance.class));
    }

    @Test void rejectionRequiresReasonAndDoesNotConsumeBalance() {
        LeaveRequest pending = request(RequestStatus.PENDING, requesterId);
        when(store.request(requestId)).thenReturn(Optional.of(pending));
        when(store.lockedRequest(requestId)).thenReturn(Optional.of(pending));
        assertThrows(BusinessException.class, () -> service.reject(requestId, " "));
        assertEquals(RequestStatus.REJECTED, service.reject(requestId, "Insufficient documentation").status());
        verify(store, never()).save(any(LeaveBalance.class));
    }

    @Test void balanceDeductingDraftCannotCrossCalendarYears() {
        when(store.lockedType(typeId)).thenReturn(Optional.of(type(true)));
        assertThrows(BusinessException.class, () -> service.createRequest(new RequestInput(employeeId,
                typeId, LocalDate.of(2026, 12, 31), LocalDate.of(2027, 1, 2), null, null, "Vacation")));
        verify(calculator, never()).calculate(any(), any(), any(), any(), any());
    }

    @Test void requiredStoredAttachmentBlocksApprovalIfMissing() {
        LeaveRequest pending = request(RequestStatus.PENDING, requesterId);
        when(store.request(requestId)).thenReturn(Optional.of(pending));
        when(store.lockedRequest(requestId)).thenReturn(Optional.of(pending));
        when(store.type(typeId)).thenReturn(Optional.of(new LeaveType(typeId, "VAC", "Vacation",
                null, true, true, true, true, TypeStatus.ACTIVE, null, null)));
        doThrow(new BusinessException("Attachment unavailable")).when(attachments)
                .requireStoredAttachment(requestId);
        assertThrows(BusinessException.class, () -> service.approve(requestId, null));
        verify(store, never()).save(any(LeaveBalance.class));
    }

    @Test void availableBalanceSubtractsPendingCommitments() {
        when(store.balance(balance.id())).thenReturn(Optional.of(balance));
        when(store.pendingDays(employeeId, typeId, 2026)).thenReturn(new BigDecimal("4.00"));
        var response = service.balance(balance.id());
        assertEquals(new BigDecimal("1.00"), response.availableDays());
        assertEquals(new BigDecimal("4.00"), response.pendingDays());
        assertEquals(BigDecimal.ZERO, response.usedDays());
    }

    @Test void fullNightLeaveConflictsWithNextMorningPartialLeave() {
        LocalDate date = LocalDate.of(2026, 11, 3);
        LeaveRequest partial = new LeaveRequest(requestId, employeeId, typeId, date, date,
                LocalTime.of(2, 0), LocalTime.of(4, 0), new BigDecimal("0.25"), "Partial",
                RequestStatus.PENDING, requesterId, null, null, null, null, null, null);
        LeaveRequest previousFull = new LeaveRequest(UUID.randomUUID(), employeeId, typeId,
                date.minusDays(1), date.minusDays(1), null, null, BigDecimal.ONE, "Night",
                RequestStatus.APPROVED, requesterId, null, reviewerId, null, null, null, null);
        when(store.request(requestId)).thenReturn(Optional.of(partial));
        when(store.lockedRequest(requestId)).thenReturn(Optional.of(partial));
        when(store.type(typeId)).thenReturn(Optional.of(type(true)));
        when(calculator.calculate(employeeId, date, date, partial.startTime(), partial.endTime()))
                .thenReturn(new BigDecimal("0.25"));
        when(store.conflictingCandidates(eq(employeeId), any(), any(), eq(requestId)))
                .thenReturn(List.of(previousFull));
        when(calculator.nightContinuationOverlaps(employeeId, previousFull.endDate(), date,
                partial.startTime(), partial.endTime())).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.approve(requestId, null));
        verify(store, never()).save(any(LeaveBalance.class));
    }

    @Test void twoPartialsTouchingOnlyDuringUnpaidBreakDoNotConflict() {
        LocalDate date = LocalDate.of(2026, 11, 3);
        LeaveRequest first = new LeaveRequest(requestId, employeeId, typeId, date, date,
                LocalTime.of(9, 0), LocalTime.of(12, 30), new BigDecimal("0.44"), "Morning",
                RequestStatus.PENDING, requesterId, null, null, null, null, null, null);
        LeaveRequest second = new LeaveRequest(UUID.randomUUID(), employeeId, typeId, date, date,
                LocalTime.NOON, LocalTime.of(14, 0), new BigDecimal("0.13"), "Afternoon",
                RequestStatus.PENDING, requesterId, null, null, null, null, null, null);
        when(store.request(requestId)).thenReturn(Optional.of(first));
        when(store.lockedRequest(requestId)).thenReturn(Optional.of(first));
        when(store.type(typeId)).thenReturn(Optional.of(type(true)));
        when(store.conflictingCandidates(eq(employeeId), any(), any(), eq(requestId)))
                .thenReturn(List.of(second));
        when(calculator.calculate(employeeId, date, date, first.startTime(), first.endTime()))
                .thenReturn(first.requestedDays());
        when(calculator.hasWorkingMinutes(employeeId, date, LocalTime.NOON, LocalTime.of(12, 30)))
                .thenReturn(false);
        assertEquals(RequestStatus.APPROVED, service.approve(requestId, null).status());
        verify(store).save(argThat((LeaveBalance b) -> b.usedDays().compareTo(new BigDecimal("0.44")) == 0));
    }

    @Test void duplicateTypeCodeCannotBeCreated() {
        when(store.typeCodeExists("VAC", null)).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.createType(
                new TypeInput("VAC", "Vacation", null, true, true, false, true)));
        verify(store, never()).save(any(LeaveType.class));
    }

    @Test void draftIsCreatedWithBackendCalculatedDaysAndInitialHistory() {
        when(store.lockedType(typeId)).thenReturn(Optional.of(type(true)));
        var result = service.createRequest(new RequestInput(employeeId, typeId,
                LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 5), null, null, "Vacation"));
        assertEquals(RequestStatus.DRAFT, result.status());
        assertEquals(new BigDecimal("4.00"), result.requestedDays());
        assertEquals(reviewerId, result.requestedBy());
        verify(store).save(argThat((LeaveRequestHistory h) -> h.previousStatus() == null
                && h.newStatus() == RequestStatus.DRAFT));
    }

    private LeaveType type(boolean requiresApproval) {
        return new LeaveType(typeId, "VAC", "Vacation", null, true, requiresApproval,
                false, true, TypeStatus.ACTIVE, null, null);
    }
    private LeaveRequest request(RequestStatus status, UUID requestedBy) {
        return new LeaveRequest(requestId, employeeId, typeId, LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 5), null, null, new BigDecimal("4.00"), "Vacation",
                status, requestedBy, null, null, null, null, null, null);
    }
}
