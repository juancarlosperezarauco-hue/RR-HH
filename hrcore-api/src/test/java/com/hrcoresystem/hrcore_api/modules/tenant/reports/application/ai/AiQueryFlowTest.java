package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service.TenantTimezoneService;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.api.AiQueryController;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.api.AiQueryExceptionHandler;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiQueryFlowTest {
    private final AnalyticAuthorizationPolicy authorization = mock(AnalyticAuthorizationPolicy.class);
    private final AiPlannerClient planner = mock(AiPlannerClient.class);
    private final AnalyticQueryService queries = mock(AnalyticQueryService.class);
    private final AiQueryOrchestrator orchestrator = new AiQueryOrchestrator(authorization, planner, queries);
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void controllerAcceptsOnlyOneBoundedQuestion() {
        AiQueryController controller = new AiQueryController(orchestrator, mapper);
        when(authorization.authorizedCatalog()).thenReturn(Map.of("employees.roster",
                new AnalyticAuthorizationPolicy.DatasetCapabilities(true, true,
                        java.util.Set.of("employee.full_name"), java.util.Set.of("employees.distinct_count"))));
        when(planner.plan(any())).thenReturn(new AiPlannerClient.Reply("unsupported", null,
                "UNRECOGNIZED_REQUEST"));
        assertEquals("unsupported", controller.query("{\"question\":\"  ejemplo  \"}").status());
        verify(planner).plan(argThat(request -> "ejemplo".equals(request.question())
                && request.capabilities().containsKey("employees.roster")
                && request.semanticContext().containsKey("datasets")
                && !request.semanticContext().toString().contains("permissions")));

        for (String invalid : List.of("{}", "[]", "{broken", "{\"question\":\"\"}",
                "{\"question\":\"a\",\"sql\":\"SELECT 1\"}",
                "{\"question\":\"a\",\"question\":\"b\"}",
                "{\"question\":\"" + "x".repeat(2001) + "\"}")) {
            AiFlowException failure = assertThrows(AiFlowException.class, () -> controller.query(invalid));
            assertEquals(AiFlowException.Code.INVALID_QUESTION, failure.code());
        }
        assertEquals(AiFlowException.Code.INVALID_QUESTION,
                assertThrows(AiFlowException.class,
                        () -> controller.query("{\"question\":\"" + "x".repeat(17_000) + "\"}"))
                        .code());
    }

    @Test
    void clarificationAndUnsupportedNeverExecuteQuery() {
        when(authorization.authorizedCatalog()).thenReturn(Map.of("attendance.daily",
                new AnalyticAuthorizationPolicy.DatasetCapabilities(true, false,
                        java.util.Set.of("department.id"), java.util.Set.of("attendance.late_days"))));
        when(planner.plan(any())).thenReturn(new AiPlannerClient.Reply(
                        "needs_clarification", null, "MISSING_DATE_RANGE"),
                new AiPlannerClient.Reply("unsupported", null, "DATASET_NOT_AVAILABLE"),
                new AiPlannerClient.Reply("unsupported", null, "PERSONAL_IDENTIFIER_NOT_ALLOWED"));
        assertEquals("needs_clarification", orchestrator.query("retrasos").status());
        assertEquals("unsupported", orchestrator.query("nómina").status());
        assertEquals("PERSONAL_IDENTIFIER_NOT_ALLOWED", orchestrator.query("identificador").reasonCode());
        verifyNoInteractions(queries);
    }

    @Test
    void semanticProjectionContainsOnlyAllowedFields() {
        when(authorization.authorizedCatalog()).thenReturn(Map.of("attendance.daily",
                new AnalyticAuthorizationPolicy.DatasetCapabilities(true, false,
                        java.util.Set.of("attendance.work_date"), java.util.Set.of("attendance.late_days"))));
        when(planner.plan(any())).thenReturn(new AiPlannerClient.Reply(
                "needs_clarification", null, "MISSING_INFORMATION", "¿Qué año?"));
        assertEquals("¿Qué año?", orchestrator.query("septiembre").question());
        verify(planner).plan(argThat(request -> {
            String metadata = request.semanticContext().toString();
            return metadata.contains("attendance.work_date") && metadata.contains("attendance.late_days")
                    && !metadata.contains("employee.full_name") && !metadata.contains("hr.reports")
                    && !metadata.contains("tenant") && !metadata.contains("schema");
        }));
        verifyNoInteractions(queries);
    }

    @Test
    void configuredBusinessClockAndOperatorsReachPlanner() {
        TenantTimezoneService zone = mock(TenantTimezoneService.class);
        when(zone.requiredZone()).thenReturn(ZoneId.of("America/La_Paz"));
        AiQueryOrchestrator withZone = new AiQueryOrchestrator(
                authorization, planner, queries, new SemanticCatalogV1(), zone);
        when(authorization.authorizedCatalog()).thenReturn(Map.of("attendance.daily",
                new AnalyticAuthorizationPolicy.DatasetCapabilities(true, true,
                        java.util.Set.of("attendance.work_date"), java.util.Set.of("attendance.late_days"))));
        when(planner.plan(any())).thenReturn(new AiPlannerClient.Reply(
                "unsupported", null, "UNRECOGNIZED_REQUEST"));
        withZone.query("ejemplo");
        verify(planner).plan(argThat(request -> {
            Map<String, Object> context = request.semanticContext();
            return "America/La_Paz".equals(context.get("businessTimezone"))
                    && LocalDate.now(ZoneId.of("America/La_Paz")).toString().equals(context.get("businessDate"))
                    && context.toString().contains("between")
                    && context.toString().contains("temporalRelations")
                    && context.toString().contains("maxPageSize=100");
        }));
    }

    @Test
    void planUsesPhaseTwoServiceAndRetainsItsResult() throws Exception {
        when(authorization.authorizedCatalog()).thenReturn(Map.of("employees.roster",
                new AnalyticAuthorizationPolicy.DatasetCapabilities(true, true,
                        java.util.Set.of("employee.full_name"), java.util.Set.of("employees.distinct_count"))));
        when(planner.plan(any())).thenReturn(new AiPlannerClient.Reply("query_plan",
                mapper.readTree(QueryPlanValidationServiceTest.ROSTER), null));
        QueryResultV1 result = new QueryResultV1(List.of(), List.of(), 0, 20, false, List.of());
        when(queries.execute(any())).thenReturn(result);
        assertSame(result, orchestrator.query("listar empleados").result());
        verify(queries).execute(argThat(json -> json.contains("\"dataset\":\"employees.roster\"")));
    }

    @Test
    void conversationalContextRequiresCompatibleAuthorizedPlanAndDropsPersonalFilters() throws Exception {
        when(authorization.authorizedCatalog()).thenReturn(Map.of("employees.roster",
                new AnalyticAuthorizationPolicy.DatasetCapabilities(true, true,
                        java.util.Set.of("employee.full_name", "department.id", "department.name"),
                        java.util.Set.of("employees.distinct_count"))));
        when(planner.plan(any())).thenReturn(new AiPlannerClient.Reply(
                "unsupported", null, "UNRECOGNIZED_REQUEST"));
        var safePlan = mapper.readTree(QueryPlanValidationServiceTest.ROSTER);
        var metadata = List.of(new AiPlannerClient.MessageMetadata("USER", "TEXT", "COMPLETED"));
        var safeContext = new AiPlannerClient.ConversationContext(true, safePlan, "1", "1", metadata);

        orchestrator.queryWithPlan("ordénalos por departamento", safeContext);

        verify(planner).plan(argThat(request -> request.conversationContext() != null
                && request.conversationContext().hasUsablePlan()
                && request.conversationContext().lastSuccessfulPlan().equals(safePlan)
                && request.conversationContext().recentMessages().equals(metadata)));
        verify(authorization).authorize(any(), eq(AnalyticAuthorizationPolicy.Action.QUERY));

        var personalPlan = safePlan.deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) personalPlan).set("filters", mapper.createArrayNode()
                .add(mapper.createObjectNode().put("field", "employee.full_name")
                        .put("op", "eq").put("value", "Synthetic Person")));
        var personalContext = new AiPlannerClient.ConversationContext(true, personalPlan,
                "1", "1", metadata);
        orchestrator.queryWithPlan("ordénalos", personalContext);

        verify(planner).plan(argThat(request -> request.conversationContext() != null
                && !request.conversationContext().hasUsablePlan()
                && request.conversationContext().lastSuccessfulPlan() == null));
    }

    @Test
    void staleCatalogAndRevokedPermissionsRemoveThePriorPlanBeforePlanning() throws Exception {
        when(authorization.authorizedCatalog()).thenReturn(Map.of("employees.roster",
                new AnalyticAuthorizationPolicy.DatasetCapabilities(true, true,
                        java.util.Set.of("employee.full_name"), java.util.Set.of("employees.distinct_count"))));
        when(planner.plan(any())).thenReturn(new AiPlannerClient.Reply(
                "unsupported", null, "UNRECOGNIZED_REQUEST"));
        var safePlan = mapper.readTree(QueryPlanValidationServiceTest.ROSTER);
        var metadata = List.of(new AiPlannerClient.MessageMetadata("USER", "TEXT", "COMPLETED"));

        orchestrator.queryWithPlan("ordénalos", new AiPlannerClient.ConversationContext(
                true, safePlan, "1", "0", metadata));
        verify(planner).plan(argThat(request -> request.conversationContext() != null
                && !request.conversationContext().hasUsablePlan()));

        reset(planner);
        when(planner.plan(any())).thenReturn(new AiPlannerClient.Reply(
                "unsupported", null, "UNRECOGNIZED_REQUEST"));
        doThrow(new QueryPlanException(QueryPlanException.Code.QUERY_NOT_ALLOWED))
                .when(authorization).authorize(any(), eq(AnalyticAuthorizationPolicy.Action.QUERY));
        orchestrator.queryWithPlan("ordénalos", new AiPlannerClient.ConversationContext(
                true, safePlan, "1", "1", metadata));
        verify(planner).plan(argThat(request -> request.conversationContext() != null
                && !request.conversationContext().hasUsablePlan()
                && request.conversationContext().recentMessages().equals(metadata)));
    }

    @Test
    void endpointSpecificErrorsHaveStableCodesAndStatuses() {
        AiQueryExceptionHandler handler = new AiQueryExceptionHandler();
        assertEquals(400, handler.flow(new AiFlowException(AiFlowException.Code.INVALID_QUESTION))
                .getStatusCode().value());
        assertEquals(502, handler.flow(new AiFlowException(AiFlowException.Code.INVALID_PLANNER_RESPONSE))
                .getStatusCode().value());
        assertEquals(504, handler.flow(new AiFlowException(AiFlowException.Code.PLANNER_TIMEOUT))
                .getStatusCode().value());
        assertEquals(403, handler.query(new QueryPlanException(QueryPlanException.Code.QUERY_NOT_ALLOWED))
                .getStatusCode().value());
        assertEquals(503, handler.query(new QueryPlanException(QueryPlanException.Code.SERVICE_UNAVAILABLE))
                .getStatusCode().value());
        assertEquals(504, handler.query(new QueryPlanException(QueryPlanException.Code.QUERY_TIMEOUT))
                .getStatusCode().value());
    }
}
