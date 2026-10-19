package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.hrcoresystem.hrcore_api.common.exception.BusinessConflictException;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.AiFlowException;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.AiQueryOrchestrator;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.AiQueryResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.AnalyticAuthorizationPolicy;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.AnalyticQueryService;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.QueryPlanException;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.QueryResultV1;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.SemanticCatalogV1;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.dto.AiConversationMessageRequest;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.dto.AiConversationMessageResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.dto.AiConversationTurnResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.dto.AiReportRefreshResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.persistence.AiMessageEntity;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/** Coordinates conversational turns while persistence and query execution use separate transactions. */
@Service
public class AiConversationTurnService {
    private static final String REPORT_MESSAGE = "Reporte ejecutado correctamente.";
    private static final String ERROR_MESSAGE = "No se pudo completar la consulta.";

    private final AiConversationTurnPersistence persistence;
    private final AiQueryOrchestrator orchestrator;
    private final AnalyticQueryService queries;
    private final AnalyticAuthorizationPolicy authorization;
    private final SecurityContextFacade security;
    private final int maxMessageLength;
    private final Duration pendingTimeout;

    public AiConversationTurnService(AiConversationTurnPersistence persistence,
                                     AiQueryOrchestrator orchestrator,
                                     AnalyticQueryService queries,
                                     AnalyticAuthorizationPolicy authorization,
                                     SecurityContextFacade security,
                                     @Value("${app.ai.conversations.message-max-length:4000}") int maxMessageLength,
                                     @Value("${app.ai.conversations.pending-timeout:PT2M}") Duration pendingTimeout) {
        if (maxMessageLength < 1 || maxMessageLength > 4000)
            throw new IllegalArgumentException("AI conversation message length must be between 1 and 4000");
        if (pendingTimeout == null || pendingTimeout.isNegative() || pendingTimeout.isZero())
            throw new IllegalArgumentException("AI conversation pending timeout must be positive");
        this.persistence = persistence;
        this.orchestrator = orchestrator;
        this.queries = queries;
        this.authorization = authorization;
        this.security = security;
        this.maxMessageLength = maxMessageLength;
        this.pendingTimeout = pendingTimeout;
    }

    public AiConversationTurnResponse submit(UUID conversationId, AiConversationMessageRequest request) {
        UUID ownerId = requireAuthorizedOwner();
        validateMessage(request.message());

        AiConversationTurnPersistence.Reservation reservation;
        try {
            reservation = persistence.reserve(conversationId, ownerId, request.clientMessageId(),
                    request.message(), pendingTimeout);
        } catch (ObjectOptimisticLockingFailureException concurrentReservation) {
            reservation = persistence.findExisting(conversationId, ownerId, request.clientMessageId(),
                            request.message())
                    .orElseThrow(() -> new BusinessConflictException(
                            "Ya hay una pregunta en proceso para esta conversación.", concurrentReservation));
        }

        if (reservation.replay()) return response(conversationId, reservation, null);

        long started = System.nanoTime();
        AiQueryOrchestrator.PlannedQuery planned;
        try {
            planned = orchestrator.queryWithPlan(request.message(), reservation.conversationContext());
        } catch (RuntimeException failure) {
            try {
                persistence.fail(conversationId, ownerId, reservation.request().getId(),
                        reservation.conversationVersion());
            } catch (RuntimeException persistenceFailure) {
                failure.addSuppressed(persistenceFailure);
            }
            throw failure;
        }

        AiQueryResponse outcome = planned.response();
        String messageType = switch (outcome.status()) {
            case "ok" -> "REPORT";
            case "needs_clarification" -> "CLARIFICATION";
            case "unsupported" -> "UNSUPPORTED";
            default -> throw new AiFlowException(AiFlowException.Code.INVALID_PLANNER_RESPONSE);
        };
        String responseText = switch (outcome.status()) {
            case "ok" -> REPORT_MESSAGE;
            case "needs_clarification" -> outcome.question();
            case "unsupported" -> outcome.message();
            default -> ERROR_MESSAGE;
        };
        long durationMillis = Duration.ofNanos(System.nanoTime() - started).toMillis();
        AiConversationTurnPersistence.Reservation completed = persistence.complete(
                conversationId, ownerId, reservation.request().getId(), reservation.conversationVersion(),
                "COMPLETED", messageType, responseText, planned.validatedPlan(),
                outcome.result(), durationMillis);
        return response(conversationId, completed, outcome.result());
    }

    public AiReportRefreshResponse refresh(UUID conversationId, UUID reportRunId) {
        UUID ownerId = requireAuthorizedOwner();
        AiConversationTurnPersistence.RunSnapshot source = persistence.loadRun(conversationId, ownerId, reportRunId);
        JsonNode plan = source.plan();
        if (!"1".equals(source.planVersion())
                || !SemanticCatalogV1.VERSION.equals(source.catalogVersion())
                || !source.planVersion().equals(plan.path("version").asText())
                || !source.catalogVersion().equals(plan.path("catalogVersion").asText()))
            throw new BusinessConflictException("El reporte no es compatible con el catálogo actual.");

        long started = System.nanoTime();
        QueryResultV1 result = queries.execute(plan.toString());
        long durationMillis = Duration.ofNanos(System.nanoTime() - started).toMillis();
        AiConversationTurnPersistence.StoredRun refreshed = persistence.saveRefresh(
                conversationId, ownerId, source, result, durationMillis);
        return new AiReportRefreshResponse(refreshed.id(), source.id(), refreshed.finishedAt(), result);
    }

    private AiConversationTurnResponse response(UUID conversationId,
                                                AiConversationTurnPersistence.Reservation reservation,
                                                QueryResultV1 transientResult) {
        AiMessageEntity request = reservation.request();
        AiMessageEntity response = reservation.response();
        String status;
        if (response == null) {
            status = "pending";
        } else {
            status = switch (response.getMessageType()) {
                case "REPORT" -> "ok";
                case "CLARIFICATION" -> "needs_clarification";
                case "UNSUPPORTED" -> "unsupported";
                default -> "error";
            };
        }
        return new AiConversationTurnResponse(conversationId, toResponse(request),
                response == null ? null : toResponse(response), status,
                reservation.reportRunId(), transientResult);
    }

    private UUID requireAuthorizedOwner() {
        if (authorization.authorizedCatalog().isEmpty())
            throw new AccessDeniedException("Se requiere acceso a reportes analíticos");
        return security.requireCurrentUserId();
    }

    private void validateMessage(String message) {
        if (message == null || message.isBlank()
                || message.codePointCount(0, message.length()) > maxMessageLength)
            throw new AiFlowException(AiFlowException.Code.INVALID_QUESTION);
    }

    private static AiConversationMessageResponse toResponse(AiMessageEntity entity) {
        return new AiConversationMessageResponse(entity.getId(), entity.getRole(), entity.getContent(),
                entity.getMessageType(), entity.getState(), entity.getCreatedAt(), entity.getCompletedAt());
    }
}
