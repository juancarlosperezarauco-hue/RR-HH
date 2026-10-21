package com.hrcoresystem.hrcore_api.modules.identity.users.application.dto;

import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateTenantUserRequest(
        @NotBlank
        @Email
        @Size(max = 150)
        String email,

        @Size(min = 8, max = 100)
        String password,

        @Size(max = 100)
        String firstName,

        @Size(max = 100)
        String lastName,

        @Size(max = 200)
        String displayName,

        TenantUserStatus status,

        UUID employeeId,

        List<UUID> roleIds
) {
    public CreateTenantUserRequest(
            String email,
            String password,
            String firstName,
            String lastName,
            List<UUID> roleIds
    ) {
        this(email, password, firstName, lastName, null, null, null, roleIds);
    }
}
