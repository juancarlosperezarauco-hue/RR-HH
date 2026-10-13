package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.application.dto;

import java.time.Instant;
import java.util.UUID;

public record VersionResponse(
    UUID id,
    UUID documentId,
    int versionNumber,
    String originalFilename,
    String mimeType,
    long fileSize,
    String checksumSha256,
    Instant createdAt) {}
