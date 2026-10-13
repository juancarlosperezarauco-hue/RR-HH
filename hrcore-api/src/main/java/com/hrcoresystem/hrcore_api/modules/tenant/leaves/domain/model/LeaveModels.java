package com.hrcoresystem.hrcore_api.modules.tenant.leaves.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public final class LeaveModels {
    private LeaveModels() { }

    public enum TypeStatus { ACTIVE, INACTIVE }
    public enum RequestStatus { DRAFT, PENDING, APPROVED, REJECTED, CANCELLED }

    public record LeaveType(UUID id, String code, String name, String description, boolean paid,
            boolean requiresApproval, boolean requiresAttachment, boolean deductsBalance,
            TypeStatus status, Instant createdAt, Instant updatedAt) { }

    public record LeaveBalance(UUID id, UUID employeeId, UUID leaveTypeId, int balanceYear,
            BigDecimal openingDays, BigDecimal accruedDays, BigDecimal usedDays,
            BigDecimal adjustedDays, Instant updatedAt) {
        public BigDecimal available() {
            return openingDays.add(accruedDays).add(adjustedDays).subtract(usedDays);
        }
    }

    public record LeaveRequest(UUID id, UUID employeeId, UUID leaveTypeId, LocalDate startDate,
            LocalDate endDate, LocalTime startTime, LocalTime endTime, BigDecimal requestedDays,
            String reason, RequestStatus status, UUID requestedBy, Instant submittedAt,
            UUID reviewedBy, Instant reviewedAt, String reviewComment, Instant createdAt,
            Instant updatedAt) { }

    public record LeaveRequestHistory(UUID id, UUID leaveRequestId, RequestStatus previousStatus,
            RequestStatus newStatus, String comment, UUID performedBy, Instant createdAt) { }

    public record LeaveAttachment(UUID id, UUID leaveRequestId, String filename, String storageKey,
            String mimeType, long fileSize, UUID uploadedBy, Instant createdAt) { }
}
