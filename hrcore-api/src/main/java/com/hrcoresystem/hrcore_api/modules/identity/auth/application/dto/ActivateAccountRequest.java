package com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto;

import jakarta.validation.constraints.NotBlank;

public record ActivateAccountRequest(
        @NotBlank
        String token
) {
}
