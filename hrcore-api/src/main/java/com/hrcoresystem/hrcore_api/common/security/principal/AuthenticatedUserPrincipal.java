package com.hrcoresystem.hrcore_api.common.security.principal;

import java.util.List;

public record AuthenticatedUserPrincipal(
        String subject,
        String email,
        String displayName,
        String tenantSlug,
        List<String> roles,
        List<String> permissions
) {
}
