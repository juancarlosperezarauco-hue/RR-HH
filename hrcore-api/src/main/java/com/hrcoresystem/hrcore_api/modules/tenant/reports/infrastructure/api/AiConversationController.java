package com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.api;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.AiFlowException;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.dto.AiConversationMessageRequest;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.dto.AiConversationMessageResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.dto.AiConversationResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.dto.AiConversationTurnResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.dto.AiReportRefreshResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.service.AiConversationService;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.service.AiConversationTurnService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.UUID;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ContentDisposition;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.export.AiReportExportService;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.export.ExportFormat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hr/ai/conversations")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyAuthority('hr.reports.employees.read','hr.reports.attendance.read')")
public class AiConversationController {
    private static final int MAX_MESSAGE_BODY_BYTES = 16 * 1024;
    private final AiConversationService conversations;
    private final AiConversationTurnService turns;
    private final ObjectMapper mapper;
    private final AiReportExportService exports;

    public AiConversationController(AiConversationService conversations, AiConversationTurnService turns,
                                    ObjectMapper mapper, AiReportExportService exports) {
        this.conversations = conversations;
        this.turns = turns;
        this.exports = exports;
        this.mapper = mapper.copy();
        this.mapper.getFactory().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AiConversationResponse> create(@RequestBody(required = false) String body) {
        if (body != null && !body.isBlank())
            throw new BusinessException("La creación de conversaciones no admite datos de entrada");
        return ApiResponse.success("Conversación creada correctamente", conversations.create());
    }

    @GetMapping
    public ApiResponse<Page<AiConversationResponse>> list(
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success("Conversaciones recuperadas correctamente", conversations.list(pageable));
    }

    @GetMapping("/{conversationId}")
    public ApiResponse<AiConversationResponse> get(@PathVariable UUID conversationId) {
        return ApiResponse.success("Conversación recuperada correctamente", conversations.get(conversationId));
    }

    @GetMapping("/{conversationId}/messages")
    public ApiResponse<Page<AiConversationMessageResponse>> messages(
            @PathVariable UUID conversationId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ApiResponse.success("Historial recuperado correctamente",
                conversations.messages(conversationId, pageable));
    }

    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<ApiResponse<AiConversationTurnResponse>> sendMessage(
            @PathVariable UUID conversationId,
            @RequestBody(required = false) String body) {
        AiConversationTurnResponse result = turns.submit(conversationId, messageRequest(body));
        HttpStatus status = "pending".equals(result.status()) ? HttpStatus.ACCEPTED : HttpStatus.OK;
        return ResponseEntity.status(status)
                .body(ApiResponse.success("Turno de conversación procesado", result));
    }

    private AiConversationMessageRequest messageRequest(String body) {
        if (body == null || body.getBytes(StandardCharsets.UTF_8).length > MAX_MESSAGE_BODY_BYTES)
            throw invalidMessage();
        try (JsonParser parser = mapper.getFactory().createParser(body)) {
            JsonNode root = mapper.readTree(parser);
            if (root == null || parser.nextToken() != null || !root.isObject() || root.size() != 2
                    || !root.has("clientMessageId") || !root.get("clientMessageId").isTextual()
                    || !root.has("message") || !root.get("message").isTextual())
                throw invalidMessage();
            String idValue = root.get("clientMessageId").textValue();
            UUID clientMessageId = UUID.fromString(idValue);
            if (!clientMessageId.toString().equalsIgnoreCase(idValue)) throw invalidMessage();
            return new AiConversationMessageRequest(clientMessageId, root.get("message").textValue());
        } catch (IOException | IllegalArgumentException invalid) {
            throw invalidMessage();
        }
    }

    private static AiFlowException invalidMessage() {
        return new AiFlowException(AiFlowException.Code.INVALID_QUESTION);
    }

    @PostMapping("/{conversationId}/reports/{reportRunId}/refresh")
    public ApiResponse<AiReportRefreshResponse> refresh(
            @PathVariable UUID conversationId, @PathVariable UUID reportRunId,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return ApiResponse.success("Reporte actualizado correctamente",
                turns.refresh(conversationId, reportRunId, page, size));
    }

    @DeleteMapping("/{conversationId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> delete(@PathVariable UUID conversationId) {
        conversations.delete(conversationId);
        return ApiResponse.success("Conversación eliminada correctamente", null);
    }

    @PostMapping("/{conversationId}/reports/{reportRunId}/export")
    @PreAuthorize("hasAuthority('hr.reports.export')")
    public void export(@PathVariable UUID conversationId, @PathVariable UUID reportRunId,
                       @RequestParam String format, HttpServletRequest request,
                       HttpServletResponse response) throws IOException {
        if (!request.getParameterMap().keySet().equals(java.util.Set.of("format"))
                || request.getParameterValues("format").length != 1 || request.getInputStream().read() != -1)
            throw new BusinessException("La exportación solo admite el formato");
        try (var download = exports.generate(conversationId, reportRunId, ExportFormat.parse(format))) {
            response.setContentType(download.mimeType());
            response.setContentLengthLong(download.size());
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("Content-Disposition", ContentDisposition.attachment()
                    .filename(download.filename(), StandardCharsets.UTF_8).build().toString());
            Files.copy(download.path(), response.getOutputStream());
        }
    }
}
