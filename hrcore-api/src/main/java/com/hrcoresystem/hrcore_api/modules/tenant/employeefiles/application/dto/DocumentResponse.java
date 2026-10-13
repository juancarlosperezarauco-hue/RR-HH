package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.application.dto;

import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.model.*;
import java.time.*;
import java.util.UUID;

public record DocumentResponse(
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
    Instant createdAt,
    Instant updatedAt) {}
