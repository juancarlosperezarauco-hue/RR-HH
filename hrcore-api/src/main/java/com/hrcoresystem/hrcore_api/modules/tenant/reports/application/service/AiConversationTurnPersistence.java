package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.AiPlannerClient;
import com.hrcoresystem.hrcore_api.common.exception.BusinessConflictException;
import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.QueryResultV1;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.persistence.AiConversationEntity;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.persistence.AiConversationJpaRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.persistence.AiMessageEntity;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.persistence.AiMessageJpaRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.persistence.AiReportRunEntity;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.persistence.AiReportRunJpaRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Keeps each persistence phase short; no database transaction spans a planner or query call. */
@Service
public class AiConversationTurnPersistence {
    private static final String USER = "USER";
    private static final String ASSISTANT = "ASSISTANT";
    private static final String PENDING = "PENDING";
    private static final String COMPLETED = "COMPLETED";
    private static final String FAILED = "FAILED";
    private static final String GENERIC_FAILURE = "No se pudo completar la consulta.";

    public record Reservation(AiMessageEntity request, AiMessageEntity response,
                              UUID reportRunId, int conversationVersion, boolean replay,
                              AiPlannerClient.ConversationContext conversationContext) {
        public Reservation(AiMessageEntity request, AiMessageEntity response,
                           UUID reportRunId, int conversationVersion, boolean replay) {
            this(request, response, reportRunId, conversationVersion, replay, null);
        }
    }

    public record RunSnapshot(UUID id, UUID requestMessageId, JsonNode plan,
                              String planVersion, String catalogVersion) {}

    public record StoredRun(UUID id, Instant finishedAt) {}

    private final AiConversationJpaRepository conversations;
    private final AiMessageJpaRepository messages;
    private final AiReportRunJpaRepository reportRuns;
    private final ObjectMapper mapper;
    private final int maxContextMessages;

    public AiConversationTurnPersistence(AiConversationJpaRepository conversations,
                                         AiMessageJpaRepository messages,
                                         AiReportRunJpaRepository reportRuns,
                                         ObjectMapper mapper,
                                         @Value("${app.ai.conversations.context-message-count:4}") int maxContextMessages) {
        if (maxContextMessages < 0 || maxContextMessages > 20)
            throw new IllegalArgumentException("AI conversation context message count must be between 0 and 20");
        this.conversations = conversations;
        this.messages = messages;
        this.reportRuns = reportRuns;
        this.mapper = mapper;
        this.maxContextMessages = maxContextMessages;
    }

    @Transactional
    public Reservation reserve(UUID conversationId, UUID ownerId, UUID clientMessageId,
                               String content, Duration pendingTimeout) {
        AiConversationEntity conversation = requireOwned(conversationId, ownerId);
        Instant now = Instant.now();
        recoverAbandoned(conversationId, now.minus(pendingTimeout));

        Optional<AiMessageEntity> prior = messages.findByConversationIdAndClientMessageId(
                conversationId, clientMessageId);
        if (prior.isPresent()) {
            AiMessageEntity request = prior.get();
            if (!request.getContent().equals(content))
                throw conflict("El identificador ya fue utilizado con otro contenido.");
            return existing(conversationId, request, conversation.getVersion());
        }

        if (messages.existsByConversationIdAndRoleAndState(conversationId, USER, PENDING))
            throw conflict("Ya hay una pregunta en proceso para esta conversación.");

        conversation.setLastMessageAt(now);
        conversations.saveAndFlush(conversation); // @Version serializes reservations across instances.

        AiPlannerClient.ConversationContext context = captureContext(conversationId);

        AiMessageEntity request = newMessage(conversationId, clientMessageId, USER, content,
                "TEXT", PENDING, null);
        messages.saveAndFlush(request);
        return new Reservation(request, null, null, conversation.getVersion(), false, context);
    }

    @Transactional(readOnly = true)
    public Optional<Reservation> findExisting(UUID conversationId, UUID ownerId, UUID clientMessageId,
                                              String content) {
        requireOwned(conversationId, ownerId);
        return messages.findByConversationIdAndClientMessageId(conversationId, clientMessageId)
                .map(request -> {
                    if (!request.getContent().equals(content))
                        throw conflict("El identificador ya fue utilizado con otro contenido.");
                    return existing(conversationId, request, 0);
                });
    }

    @Transactional
    public Reservation complete(UUID conversationId, UUID ownerId, UUID requestId, int reservedVersion,
                                String requestState, String responseType,
                                String responseContent, JsonNode successfulPlan,
                                QueryResultV1 result, long durationMillis) {
        AiConversationEntity conversation = requireOwned(conversationId, ownerId);
        if (conversation.getVersion() != reservedVersion)
            throw conflict("La solicitud pendiente ya no puede completarse.");
        AiMessageEntity request = messages.findByIdAndConversationId(requestId, conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Mensaje no encontrado"));
        if (!PENDING.equals(request.getState()))
            throw conflict("La solicitud ya fue completada o recuperada.");

        Instant finished = Instant.now();
        request.setState(requestState);
        request.setCompletedAt(finished);
        messages.save(request);

        AiMessageEntity response = newMessage(conversationId, null, ASSISTANT, responseContent,
                responseType, COMPLETED, requestId);
        response.setCompletedAt(finished);
        messages.save(response);

        AiReportRunEntity run = null;
        if (successfulPlan != null) {
            if (result == null) throw new IllegalArgumentException("A successful plan needs a query result");
            run = newRun(conversationId, requestId, successfulPlan, result, durationMillis, finished);
            reportRuns.save(run);
        }

        conversation.setLastMessageAt(finished);
        conversations.save(conversation);
        messages.flush();
        if (run != null) reportRuns.flush();
        return new Reservation(request, response, run == null ? null : run.getId(),
                conversation.getVersion(), false);
    }

    @Transactional
    public Reservation fail(UUID conversationId, UUID ownerId, UUID requestId, int reservedVersion) {
        return complete(conversationId, ownerId, requestId, reservedVersion, FAILED,
                "ERROR", GENERIC_FAILURE, null, null, 0);
    }

    @Transactional(readOnly = true)
    public RunSnapshot loadRun(UUID conversationId, UUID ownerId, UUID reportRunId) {
        requireOwned(conversationId, ownerId);
        AiReportRunEntity run = reportRuns.findFirstByConversationIdAndIdAndState(
                        conversationId, reportRunId, "SUCCEEDED")
                .orElseThrow(() -> new ResourceNotFoundException("Reporte no encontrado"));
        return new RunSnapshot(run.getId(), run.getRequestMessageId(), run.getQueryPlan().deepCopy(),
                run.getPlanVersion(), run.getCatalogVersion());
    }

    @Transactional
    public StoredRun saveRefresh(UUID conversationId, UUID ownerId, RunSnapshot source,
                                 QueryResultV1 result, long durationMillis) {
        AiConversationEntity conversation = requireOwned(conversationId, ownerId);
        AiReportRunEntity original = reportRuns.findFirstByConversationIdAndIdAndState(
                        conversationId, source.id(), "SUCCEEDED")
                .orElseThrow(() -> new ResourceNotFoundException("Reporte no encontrado"));
        Instant finished = Instant.now();
        AiReportRunEntity refreshed = newRun(conversationId, original.getRequestMessageId(),
                source.plan(), result, durationMillis, finished);
        reportRuns.saveAndFlush(refreshed);
        conversation.setLastMessageAt(finished);
        conversations.save(conversation);
        return new StoredRun(refreshed.getId(), finished);
    }

    private Reservation existing(UUID conversationId, AiMessageEntity request, int version) {
        AiMessageEntity response = messages.findByConversationIdAndInReplyToMessageId(
                conversationId, request.getId()).orElse(null);
        UUID runId = reportRuns.findFirstByConversationIdAndRequestMessageIdAndStateOrderByFinishedAtDescIdDesc(
                        conversationId, request.getId(), "SUCCEEDED")
                .map(AiReportRunEntity::getId).orElse(null);
        return new Reservation(request, response, runId, version, true);
    }

    private AiPlannerClient.ConversationContext captureContext(UUID conversationId) {
        AiReportRunEntity latest = reportRuns.findFirstByConversationIdAndStateOrderByFinishedAtDescIdDesc(
                conversationId, "SUCCEEDED").orElse(null);
        List<AiPlannerClient.MessageMetadata> recentMessages = maxContextMessages == 0 ? List.of()
                : messages.findAllByConversationIdOrderByCreatedAtDescIdDesc(
                                conversationId, PageRequest.of(0, maxContextMessages))
                        .getContent().stream()
                        .map(message -> new AiPlannerClient.MessageMetadata(
                                message.getRole(), message.getMessageType(), message.getState()))
                        .toList();
        java.util.ArrayList<AiPlannerClient.MessageMetadata> chronological =
                new java.util.ArrayList<>(recentMessages);
        java.util.Collections.reverse(chronological);
        if (latest == null)
            return new AiPlannerClient.ConversationContext(false, null, null, null, chronological);
        return new AiPlannerClient.ConversationContext(true, latest.getQueryPlan(),
                latest.getPlanVersion(), latest.getCatalogVersion(), chronological);
    }

    private void recoverAbandoned(UUID conversationId, Instant cutoff) {
        List<AiMessageEntity> abandoned = messages.findAllByConversationIdAndRoleAndStateAndCreatedAtBefore(
                conversationId, USER, PENDING, cutoff);
        if (abandoned.isEmpty()) return;
        Instant now = Instant.now();
        for (AiMessageEntity request : abandoned) {
            request.setState(FAILED);
            request.setCompletedAt(now);
            AiMessageEntity response = newMessage(conversationId, null, ASSISTANT, GENERIC_FAILURE,
                    "ERROR", COMPLETED, request.getId());
            response.setCompletedAt(now);
            messages.save(response);
        }
        messages.saveAllAndFlush(abandoned);
    }

    private AiReportRunEntity newRun(UUID conversationId, UUID requestId, JsonNode plan,
                                     QueryResultV1 result, long durationMillis, Instant finished) {
        AiReportRunEntity run = new AiReportRunEntity();
        run.setConversationId(conversationId);
        run.setRequestMessageId(requestId);
        run.setQueryPlan(plan.deepCopy());
        run.setPlanVersion(plan.path("version").asText());
        run.setCatalogVersion(plan.path("catalogVersion").asText());
        run.setState("SUCCEEDED");
        run.setFinishedAt(finished);
        run.setResultMetadata(mapper.valueToTree(Map.of(
                "columns", result.columns().stream().map(QueryResultV1.Column::key).toList(),
                "rowsReturned", result.rows().size(),
                "pageNumber", result.pageNumber(),
                "pageSize", result.pageSize(),
                "hasMore", result.hasMore(),
                "warnings", result.warnings(),
                "durationMillis", Math.max(0, durationMillis))));
        return run;
    }

    private static AiMessageEntity newMessage(UUID conversationId, UUID clientMessageId,
                                              String role, String content, String type,
                                              String state, UUID inReplyTo) {
        AiMessageEntity message = new AiMessageEntity();
        message.setConversationId(conversationId);
        message.setClientMessageId(clientMessageId);
        message.setRole(role);
        message.setContent(content);
        message.setMessageType(type);
        message.setState(state);
        message.setInReplyToMessageId(inReplyTo);
        return message;
    }

    private AiConversationEntity requireOwned(UUID conversationId, UUID ownerId) {
        return conversations.findByIdAndOwnerUserId(conversationId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversación no encontrada"));
    }

    private static BusinessConflictException conflict(String message) {
        return new BusinessConflictException(message);
    }
}
