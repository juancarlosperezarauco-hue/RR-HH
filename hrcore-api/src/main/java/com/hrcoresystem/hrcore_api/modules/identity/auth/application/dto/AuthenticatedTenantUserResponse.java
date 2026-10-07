package com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto;

import java.util.List;
import java.util.UUID;

public record AuthenticatedTenantUserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        boolean active,
        String status,
        String tenantSlug,
        List<String> roles
) {
}