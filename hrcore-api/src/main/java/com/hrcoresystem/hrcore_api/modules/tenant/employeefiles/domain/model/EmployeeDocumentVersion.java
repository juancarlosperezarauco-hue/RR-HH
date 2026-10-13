package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.model;

import java.time.Instant;
import java.util.UUID;

public record EmployeeDocumentVersion(
    UUID id,
    UUID documentId,
    int versionNumber,
    String originalFilename,
    String storageKey,
    String mimeType,
    long fileSize,
    String checksumSha256,
    UUID uploadedBy,
    Instant createdAt) {}
