package com.hrcoresystem.hrcore_api.modules.identity.access.domain.model;

import java.time.Instant;
import java.util.UUID;

public record SystemPermission(
    UUID id,
    String code,
    String module,
    String description,
    PermissionScope scope,
    boolean customRoleAssignable,
    boolean active,
    Instant createdAt) {

    public SystemPermission(
            String code,
            String module,
            String description,
            PermissionScope scope,
            boolean customRoleAssignable,
            boolean active
    ) {
        this(UUID.randomUUID(), code, module, description, scope, customRoleAssignable, active, Instant.now());
    }
}
