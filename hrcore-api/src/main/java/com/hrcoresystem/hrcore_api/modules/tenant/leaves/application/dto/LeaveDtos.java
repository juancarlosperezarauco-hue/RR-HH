package com.hrcoresystem.hrcore_api.modules.tenant.leaves.application.dto;

import com.hrcoresystem.hrcore_api.modules.tenant.leaves.domain.model.LeaveModels.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

public final class LeaveDtos {
    private LeaveDtos() { }

    public record TypeInput(@NotBlank @Size(max = 50) String code,
            @NotBlank @Size(max = 150) String name, String description,
            @NotNull Boolean paid, @NotNull Boolean requiresApproval,
            @NotNull Boolean requiresAttachment, @NotNull Boolean deductsBalance) { }
    public record TypeResponse(UUID id, String code, String name, String description, boolean paid,
            boolean requiresApproval, boolean requiresAttachment, boolean deductsBalance,
            TypeStatus status, Instant createdAt, Instant updatedAt) { }

    public record BalanceInput(@NotNull UUID employeeId, @NotNull UUID leaveTypeId,
            @NotNull Integer balanceYear, @NotNull BigDecimal openingDays) { }
    public record BalanceAdjustment(@NotNull BigDecimal deltaDays, @NotBlank String reason) { }
    public record BalanceResponse(UUID id, UUID employeeId, UUID leaveTypeId, int balanceYear,
            BigDecimal openingDays, BigDecimal accruedDays, BigDecimal usedDays,
            BigDecimal adjustedDays, BigDecimal pendingDays, BigDecimal availableDays,
            Instant updatedAt) { }

    public record RequestInput(@NotNull UUID employeeId, @NotNull UUID leaveTypeId,
            @NotNull LocalDate startDate, @NotNull LocalDate endDate,
            LocalTime startTime, LocalTime endTime, @NotBlank String reason) { }
    public record RequestResponse(UUID id, UUID employeeId, UUID leaveTypeId,
            LocalDate startDate, LocalDate endDate, LocalTime startTime, LocalTime endTime,
            BigDecimal requestedDays, String reason, RequestStatus status, UUID requestedBy,
            Instant submittedAt, UUID reviewedBy, Instant reviewedAt, String reviewComment,
            Instant createdAt, Instant updatedAt) { }
    public record ReviewInput(String comment) { }
    public record HistoryResponse(UUID id, UUID leaveRequestId, RequestStatus previousStatus,
            RequestStatus newStatus, String comment, UUID performedBy, Instant createdAt) { }
    public record AttachmentResponse(UUID id, UUID leaveRequestId, String filename, String mimeType,
            long fileSize, UUID uploadedBy, Instant createdAt) { }
}
