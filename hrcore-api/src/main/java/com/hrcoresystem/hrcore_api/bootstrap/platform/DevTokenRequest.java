package com.hrcoresystem.hrcore_api.bootstrap.platform;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record DevTokenRequest(
        @NotBlank
        @Email
        String email,

        @NotBlank
        String tenantSlug,

        List<String> roles
) {
}