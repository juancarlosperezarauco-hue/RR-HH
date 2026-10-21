package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai;

import com.fasterxml.jackson.annotation.JsonInclude;

/** The public response has exactly one of the three documented shapes. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiQueryResponse(String status, QueryResultV1 result, String question,
                              String message, String reasonCode) {
    public static AiQueryResponse ok(QueryResultV1 result) {
        return new AiQueryResponse("ok", result, null, null, null);
    }

    public static AiQueryResponse clarification(String question, String reasonCode) {
        return new AiQueryResponse("needs_clarification", null, question, null, reasonCode);
    }

    public static AiQueryResponse unsupported(String message, String reasonCode) {
        return new AiQueryResponse("unsupported", null, null, message, reasonCode);
    }
}
