package com.hrcoresystem.hrcore_api.modules.identity.users.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateTenantUserRequest(
        @NotBlank
        @Email
        @Size(max = 150)
        String email,

        @Size(max = 100)
        String firstName,

        @Size(max = 100)
        String lastName,

        @Size(max = 200)
        String displayName,

        UUID employeeId
) {
    public UpdateTenantUserRequest(
            String email,
            String firstName,
            String lastName
    ) {
        this(email, firstName, lastName, null, null);
    }
}
