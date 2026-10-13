package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeeDocument(
    UUID id,
    UUID employeeFileId,
    UUID contractId,
    DocumentType documentType,
    String title,
    String description,
    String documentNumber,
    LocalDate issuedAt,
    LocalDate expiresAt,
    boolean confidential,
    VerificationStatus verificationStatus,
    UUID verifiedBy,
    Instant verifiedAt,
    FileStatus status,
    UUID createdBy,
    Instant createdAt,
    Instant updatedAt) {}
