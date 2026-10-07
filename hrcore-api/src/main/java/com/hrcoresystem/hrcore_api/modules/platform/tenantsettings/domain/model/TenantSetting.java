package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model;

import java.time.Instant;
import java.util.UUID;

public record TenantSetting(
        UUID id,
        String settingKey,
        String settingValue,
        Instant createdAt,
        Instant updatedAt
) {
}