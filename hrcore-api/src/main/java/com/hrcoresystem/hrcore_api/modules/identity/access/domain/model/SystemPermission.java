package com.hrcoresystem.hrcore_api.modules.identity.access.domain.model;

import java.time.Instant;
import java.util.UUID;

public record SystemPermission(
        UUID id,
        String code,
        String module,
        String description,
        boolean active,
        Instant createdAt
) {
}