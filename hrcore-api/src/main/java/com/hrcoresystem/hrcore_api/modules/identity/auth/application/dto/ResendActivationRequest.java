package com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResendActivationRequest(
        @NotBlank
        @Email
        @Size(max = 150)
        String email
) {
}
