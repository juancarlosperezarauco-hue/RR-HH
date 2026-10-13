package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.application.dto;

import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.model.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateDocumentRequest(
    UUID contractId,
    @NotNull DocumentType documentType,
    @NotBlank @Size(max = 200) String title,
    String description,
    @Size(max = 100) String documentNumber,
    LocalDate issuedAt,
    LocalDate expiresAt,
    @NotNull Boolean confidential,
    @NotNull FileStatus status) {}
