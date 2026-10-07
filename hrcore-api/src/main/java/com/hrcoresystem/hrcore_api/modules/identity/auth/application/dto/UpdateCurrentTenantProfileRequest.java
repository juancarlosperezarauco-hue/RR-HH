package com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto;


import jakarta.validation.constraints.Size;

public record UpdateCurrentTenantProfileRequest(
        @Size(max = 100)
        String firstName,

        @Size(max = 100)
        String lastName
) {
}
