package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.api.AiQueryController;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.api.AiQueryExceptionHandler;
import java.util.List;
import java.util.Map;
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
                && request.capabilities().containsKey("employees.roster")));

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
                new AiPlannerClient.Reply("unsupported", null, "DATASET_NOT_AVAILABLE"));
        assertEquals("needs_clarification", orchestrator.query("retrasos").status());
        assertEquals("unsupported", orchestrator.query("nómina").status());
        verifyNoInteractions(queries);
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
    void endpointSpecificErrorsHaveStableCodesAndStatuses() {
        AiQueryExceptionHandler handler = new AiQueryExceptionHandler();
        assertEquals(400, handler.flow(new AiFlowException(AiFlowException.Code.INVALID_QUESTION))
                .getStatusCode().value());
        assertEquals(502, handler.flow(new AiFlowException(AiFlowException.Code.INVALID_PLANNER_RESPONSE))
                .getStatusCode().value());
        assertEquals(403, handler.query(new QueryPlanException(QueryPlanException.Code.QUERY_NOT_ALLOWED))
                .getStatusCode().value());
        assertEquals(503, handler.query(new QueryPlanException(QueryPlanException.Code.SERVICE_UNAVAILABLE))
                .getStatusCode().value());
        assertEquals(504, handler.query(new QueryPlanException(QueryPlanException.Code.QUERY_TIMEOUT))
                .getStatusCode().value());
    }
}
