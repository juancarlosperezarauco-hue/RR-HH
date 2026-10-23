package com.hrcoresystem.hrcore_api.modules.identity.users.application.dto;

import java.time.Instant;
import java.util.UUID;
import java.util.List;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.dto.AssignedRoleResponse;

public record TenantUserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String displayName,
        boolean active,
        String status,
        Instant createdAt,
        Instant updatedAt,
        List<AssignedRoleResponse> roles,
        UUID employeeId,
        String employeeNumber,
        String employeeFullName,
        boolean owner
) {
    public TenantUserResponse(
            UUID id,
            String email,
            String firstName,
            String lastName,
            boolean active,
            String status,
            Instant createdAt,
            Instant updatedAt,
            List<AssignedRoleResponse> roles
    ) {
        this(
                id,
                email,
                firstName,
                lastName,
                ((firstName != null ? firstName : "") + (lastName != null && !lastName.isBlank() ? " " + lastName : "")).trim(),
                active,
                status,
                createdAt,
                updatedAt,
                roles,
                null,
                null,
                null,
                false
        );
    }
}
