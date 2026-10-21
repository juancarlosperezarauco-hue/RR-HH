package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.QueryResultV1;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiConversationTurnResponse(
        UUID conversationId,
        AiConversationMessageResponse request,
        AiConversationMessageResponse response,
        String status,
        UUID reportRunId,
        QueryResultV1 result) {
}
