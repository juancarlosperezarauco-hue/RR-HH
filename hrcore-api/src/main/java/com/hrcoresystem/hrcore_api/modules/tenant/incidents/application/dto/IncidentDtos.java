package com.hrcoresystem.hrcore_api.modules.tenant.incidents.application.dto;
import com.hrcoresystem.hrcore_api.modules.tenant.incidents.domain.model.IncidentModels.*; import jakarta.validation.constraints.*; import java.time.*; import java.util.UUID;
public final class IncidentDtos { private IncidentDtos() {}
 public record Create(@NotNull UUID employeeId, @NotNull Type incidentType, @NotNull Severity severity,@NotBlank @Size(max=200) String title,@NotBlank String description,@NotNull Instant occurredAt,@Size(max=200) String location, UUID assignedTo) {}
 public record Update(@NotNull Severity severity,@NotBlank @Size(max=200) String title,@NotBlank String description,@Size(max=200) String location) {}
 public record Assign(@NotNull UUID assignedTo) {}
 public record Resolution(@NotBlank String comment) {}
 public record EmployeeResponse(@NotBlank String response) {}
 public record ActionCreate(@NotNull ActionType actionType,@NotBlank String description,@NotNull LocalDate actionDate,UUID responsibleUserId,String notes) {}
 public record IncidentResponse(UUID id,UUID employeeId,UUID employmentPeriodId,String incidentNumber,Type incidentType,Severity severity,String title,String description,Instant occurredAt,String location,Status status,UUID reportedBy,UUID assignedTo,String employeeResponse,Instant employeeResponseAt,String resolution,Instant resolvedAt,Instant closedAt,Instant createdAt,Instant updatedAt) {}
 public record ActionResponse(UUID id,UUID incidentId,ActionType actionType,String description,LocalDate actionDate,ActionStatus status,UUID responsibleUserId,Instant completedAt,String notes,Instant createdAt,Instant updatedAt) {}
 public record HistoryResponse(UUID id,UUID incidentId,String action,String previousStatus,String newStatus,String comment,UUID performedBy,Instant createdAt) {}
 public record AttachmentResponse(UUID id,UUID incidentId,AttachmentType attachmentType,String filename,String mimeType,long fileSize,String description,UUID uploadedBy,Instant createdAt) {}
}
