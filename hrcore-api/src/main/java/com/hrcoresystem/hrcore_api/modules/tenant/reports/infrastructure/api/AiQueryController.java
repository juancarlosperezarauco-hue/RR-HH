package com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.api;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.AiFlowException;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.AiQueryOrchestrator;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.AiQueryResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hr/ai")
public class AiQueryController {
    private static final int MAX_BODY_BYTES = 16 * 1024;
    private static final int MAX_QUESTION_CHARACTERS = 2_000;
    private final AiQueryOrchestrator orchestrator;
    private final ObjectMapper mapper;

    public AiQueryController(AiQueryOrchestrator orchestrator, ObjectMapper mapper) {
        this.orchestrator = orchestrator;
        this.mapper = mapper.copy();
        this.mapper.getFactory().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    }

    @PostMapping(value = "/query", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('hr.reports.employees.read','hr.reports.attendance.read')")
    public AiQueryResponse query(@RequestBody(required = false) String body) {
        return orchestrator.query(question(body));
    }

    private String question(String body) {
        if (body == null || body.getBytes(StandardCharsets.UTF_8).length > MAX_BODY_BYTES)
            throw invalid();
        try (JsonParser parser = mapper.getFactory().createParser(body)) {
            JsonNode root = mapper.readTree(parser);
            if (root == null || parser.nextToken() != null || !root.isObject()
                    || root.size() != 1 || !root.has("question")
                    || !root.get("question").isTextual()) throw invalid();
            String question = root.get("question").textValue().trim();
            if (question.isBlank() || question.codePointCount(0, question.length()) > MAX_QUESTION_CHARACTERS)
                throw invalid();
            return question;
        } catch (IOException failure) {
            throw invalid();
        }
    }

    private static AiFlowException invalid() {
        return new AiFlowException(AiFlowException.Code.INVALID_QUESTION);
    }
}
