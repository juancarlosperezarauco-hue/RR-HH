package com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto;

import java.util.UUID;
import java.util.List;

public record AuthenticatedTenantUserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        boolean active,
        String status,
        String tenantSlug,
        String scope,
        boolean owner,
        List<String> roles,
        List<AuthenticatedRoleResponse> roleDetails,
        List<String> permissions,
        long securityVersion,
        long authorizationVersion,
        long catalogVersion
) {
}
