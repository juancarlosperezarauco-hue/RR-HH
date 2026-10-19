package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.common.security.principal.AuthenticatedUserPrincipal;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContext;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.AiFlowException;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.AiQueryOrchestrator;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.AiQueryResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.AnalyticAuthorizationPolicy;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.AnalyticQueryService;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.QueryPlanException;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.QueryResultV1;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.SemanticCatalogV1;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.dto.AiConversationMessageRequest;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.persistence.AiMessageEntity;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiConversationTurnServiceTest {
    private static final String TENANT = "synthetic-tenant";
    private static final String FULL_READ = "hr.reports.employees.read,employees.list";
    private final SecurityContextFacade security = new SecurityContextFacade();
    private final AnalyticAuthorizationPolicy authorization =
            new AnalyticAuthorizationPolicy(security, new SemanticCatalogV1());
    private final ObjectMapper mapper = new ObjectMapper();
    @Mock private AiConversationTurnPersistence persistence;
    @Mock private AiQueryOrchestrator orchestrator;
    @Mock private AnalyticQueryService queries;
    private AiConversationTurnService service;
    private UUID ownerId;
    private UUID conversationId;
    private UUID clientMessageId;
    private JsonNode plan;
    private QueryResultV1 result;

    @BeforeEach
    void setUp() throws Exception {
        ownerId = UUID.randomUUID();
        conversationId = UUID.randomUUID();
        clientMessageId = UUID.randomUUID();
        service = new AiConversationTurnService(persistence, orchestrator, queries, authorization,
                security, 4000, Duration.ofMinutes(2));
        plan = mapper.readTree("""
                {"version":"1","catalogVersion":"1","dataset":"employees.roster",
                 "mode":"detail","asOfDate":"2026-10-01","select":["employee.full_name"]}
                """);
        result = new QueryResultV1(List.of(new QueryResultV1.Column("employee.full_name",
                SemanticCatalogV1.ValueType.TEXT)), List.of(Map.of("employee.full_name", "Synthetic Person")),
                0, 20, false, List.of());
        authenticate(FULL_READ.split(","));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    @Test
    void savesSuccessfulPlanAndReturnsResultOnlyForTheCurrentRequest() throws Exception {
        String content = "listar empleados";
        AiMessageEntity request = message(UUID.randomUUID(), "USER", content, "TEXT", "PENDING", null);
        AiMessageEntity response = message(UUID.randomUUID(), "ASSISTANT", "Reporte ejecutado correctamente.",
                "REPORT", "COMPLETED", request.getId());
        UUID reportRunId = UUID.randomUUID();
        var context = new com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.AiPlannerClient.ConversationContext(
                true, plan, "1", "1", List.of(new com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.AiPlannerClient.MessageMetadata(
                        "USER", "TEXT", "COMPLETED")));
        when(persistence.reserve(conversationId, ownerId, clientMessageId, content, Duration.ofMinutes(2)))
                .thenReturn(new AiConversationTurnPersistence.Reservation(request, null, null, 5, false, context));
        when(orchestrator.queryWithPlan(content, context)).thenReturn(
                new AiQueryOrchestrator.PlannedQuery(AiQueryResponse.ok(result), plan));
        when(persistence.complete(eq(conversationId), eq(ownerId), eq(request.getId()), eq(5),
                eq("COMPLETED"), eq("REPORT"), eq("Reporte ejecutado correctamente."),
                eq(plan), eq(result), anyLong()))
                .thenReturn(new AiConversationTurnPersistence.Reservation(request, response, reportRunId, 6, false));

        var actual = service.submit(conversationId,
                new AiConversationMessageRequest(clientMessageId, content));

        assertEquals("ok", actual.status());
        assertEquals(reportRunId, actual.reportRunId());
        assertEquals(result, actual.result());
        verify(orchestrator).queryWithPlan(content, context);
        verify(persistence).complete(eq(conversationId), eq(ownerId), eq(request.getId()), eq(5),
                eq("COMPLETED"), eq("REPORT"), anyString(), eq(plan), eq(result), anyLong());
    }

    @Test
    void repeatedRequestReplaysStoredOutcomeWithoutPlannerOrDatabaseQuery() {
        String content = "listar empleados";
        AiMessageEntity request = message(UUID.randomUUID(), "USER", content, "TEXT", "COMPLETED", null);
        AiMessageEntity response = message(UUID.randomUUID(), "ASSISTANT", "Reporte ejecutado correctamente.",
                "REPORT", "COMPLETED", request.getId());
        UUID existingRun = UUID.randomUUID();
        when(persistence.reserve(conversationId, ownerId, clientMessageId, content, Duration.ofMinutes(2)))
                .thenReturn(new AiConversationTurnPersistence.Reservation(request, response, existingRun, 8, true));

        var actual = service.submit(conversationId,
                new AiConversationMessageRequest(clientMessageId, content));

        assertEquals("ok", actual.status());
        assertEquals(existingRun, actual.reportRunId());
        assertNull(actual.result());
        verifyNoInteractions(orchestrator, queries);
    }

    @Test
    void clarificationAndUnsupportedDoNotPersistAPlan() throws Exception {
        String content = "reporte no disponible";
        AiMessageEntity request = message(UUID.randomUUID(), "USER", content, "TEXT", "PENDING", null);
        AiMessageEntity response = message(UUID.randomUUID(), "ASSISTANT", "No se admite.",
                "UNSUPPORTED", "COMPLETED", request.getId());
        when(persistence.reserve(any(), any(), any(), anyString(), any()))
                .thenReturn(new AiConversationTurnPersistence.Reservation(request, null, null, 2, false));
        when(orchestrator.queryWithPlan(content, null)).thenReturn(new AiQueryOrchestrator.PlannedQuery(
                AiQueryResponse.unsupported("No se admite.", "UNRECOGNIZED_REQUEST"), null));
        when(persistence.complete(any(), any(), any(), anyInt(), eq("COMPLETED"), eq("UNSUPPORTED"),
                eq("No se admite."), isNull(), isNull(), anyLong()))
                .thenReturn(new AiConversationTurnPersistence.Reservation(request, response, null, 3, false));

        var actual = service.submit(conversationId,
                new AiConversationMessageRequest(clientMessageId, content));

        assertEquals("unsupported", actual.status());
        assertNull(actual.result());
        verify(persistence).complete(any(), any(), any(), anyInt(), eq("COMPLETED"), eq("UNSUPPORTED"),
                eq("No se admite."), isNull(), isNull(), anyLong());
    }

    @Test
    void plannerFailureMarksPendingTurnFailedAndDoesNotStoreAPlan() {
        AiMessageEntity request = message(UUID.randomUUID(), "USER", "listar empleados", "TEXT", "PENDING", null);
        when(persistence.reserve(any(), any(), any(), anyString(), any()))
                .thenReturn(new AiConversationTurnPersistence.Reservation(request, null, null, 11, false));
        when(orchestrator.queryWithPlan("listar empleados", null))
                .thenThrow(new QueryPlanException(QueryPlanException.Code.SERVICE_UNAVAILABLE));
        AiMessageEntity error = message(UUID.randomUUID(), "ASSISTANT", "No se pudo completar la consulta.",
                "ERROR", "COMPLETED", request.getId());
        when(persistence.fail(conversationId, ownerId, request.getId(), 11))
                .thenReturn(new AiConversationTurnPersistence.Reservation(request, error, null, 12, false));

        assertThrows(QueryPlanException.class, () -> service.submit(conversationId,
                new AiConversationMessageRequest(clientMessageId, "listar empleados")));

        verify(persistence).fail(conversationId, ownerId, request.getId(), 11);
        verify(persistence, never()).complete(any(), any(), any(), anyInt(), anyString(), anyString(),
                anyString(), any(), any(), anyLong());
    }

    @Test
    void refreshReexecutesStoredPlanWithoutCallingPlanner() throws Exception {
        UUID sourceRun = UUID.randomUUID();
        UUID refreshedRun = UUID.randomUUID();
        when(persistence.loadRun(conversationId, ownerId, sourceRun))
                .thenReturn(new AiConversationTurnPersistence.RunSnapshot(sourceRun, UUID.randomUUID(),
                        plan, "1", "1"));
        when(queries.execute(plan.toString())).thenReturn(result);
        when(persistence.saveRefresh(eq(conversationId), eq(ownerId), any(), eq(result), anyLong()))
                .thenReturn(new AiConversationTurnPersistence.StoredRun(refreshedRun, Instant.now()));

        var actual = service.refresh(conversationId, sourceRun);

        assertEquals(refreshedRun, actual.reportRunId());
        assertEquals(sourceRun, actual.refreshedFromRunId());
        assertEquals(result, actual.result());
        verifyNoInteractions(orchestrator);
        verify(queries).execute(plan.toString());
    }

    @Test
    void revokedAnalyticAccessBlocksConversationOperationsBeforePersistence() {
        authenticate("employees.list");

        assertThrows(AccessDeniedException.class, () -> service.submit(conversationId,
                new AiConversationMessageRequest(clientMessageId, "listar empleados")));

        verifyNoInteractions(persistence, orchestrator, queries);
    }

    private AiMessageEntity message(UUID id, String role, String content, String type,
                                    String state, UUID replyTo) {
        AiMessageEntity result = new AiMessageEntity();
        result.setId(id);
        result.setConversationId(conversationId);
        result.setRole(role);
        result.setContent(content);
        result.setMessageType(type);
        result.setState(state);
        result.setInReplyToMessageId(replyTo);
        result.setCreatedAt(Instant.now());
        result.setCompletedAt("PENDING".equals(state) ? null : Instant.now());
        return result;
    }

    private void authenticate(String... permissions) {
        TenantContextHolder.set(new TenantContext(TENANT, "synthetic_tenant", false));
        UUID id = ownerId == null ? UUID.randomUUID() : ownerId;
        ownerId = id;
        var principal = new AuthenticatedUserPrincipal(id, UUID.randomUUID(),
                "synthetic@example.invalid", "Synthetic", TENANT, "TENANT", List.of(),
                List.of(permissions), 1, 1L, 1);
        var authorities = java.util.Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }
}
