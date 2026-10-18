package com.hrcoresystem.hrcore_api.modules.tenant.incidents.domain.model;
import java.time.*; import java.util.UUID;
public final class IncidentModels { private IncidentModels() {}
 public enum Type { MEMORANDUM, WARNING, DISCIPLINARY, MISCONDUCT, PERFORMANCE, SAFETY, RECOGNITION, OTHER }
 public enum Severity { LOW, MEDIUM, HIGH, CRITICAL }
 public enum Status { OPEN, UNDER_REVIEW, RESOLVED, CLOSED, CANCELLED }
 public enum ActionType { VERBAL_WARNING, WRITTEN_WARNING, MEMORANDUM, TRAINING, CORRECTIVE_ACTION, DISCIPLINARY_ACTION, FOLLOW_UP, OTHER }
 public enum ActionStatus { PENDING, IN_PROGRESS, COMPLETED, CANCELLED }
 public enum AttachmentType { EVIDENCE, MEMORANDUM, EMPLOYEE_RESPONSE, RESOLUTION, OTHER }
 public record Incident(UUID id, UUID employeeId, UUID employmentPeriodId, String incidentNumber, Type incidentType,
   Severity severity, String title, String description, Instant occurredAt, String location, Status status,
   UUID reportedBy, UUID assignedTo, String employeeResponse, Instant employeeResponseAt, String resolution,
   Instant resolvedAt, Instant closedAt, Instant createdAt, Instant updatedAt) {}
 public record Action(UUID id, UUID incidentId, ActionType actionType, String description, LocalDate actionDate,
   ActionStatus status, UUID responsibleUserId, Instant completedAt, String notes, Instant createdAt, Instant updatedAt) {}
 public record Attachment(UUID id, UUID incidentId, AttachmentType attachmentType, String filename, String storageKey,
   String mimeType, long fileSize, String description, UUID uploadedBy, Instant createdAt) {}
}
