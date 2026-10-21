package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AiConversationMessageRequest(
        @NotNull UUID clientMessageId,
        @NotBlank @Size(max = 4000) String message) {
}
