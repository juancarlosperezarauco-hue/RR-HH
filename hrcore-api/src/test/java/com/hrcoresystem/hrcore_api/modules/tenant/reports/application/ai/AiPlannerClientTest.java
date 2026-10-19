package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiPlannerClientTest {
    private static final String TOKEN = "synthetic-service-token-32-characters-minimum";
    private HttpServer server;

    @AfterEach
    void stop() {
        if (server != null) server.stop(0);
    }

    @Test
    void missingCredentialAndAddressFailClosed() {
        var request = request();
        for (AiPlannerClient client : new AiPlannerClient[] {
                new AiPlannerClient("", TOKEN, new ObjectMapper()),
                new AiPlannerClient("http://127.0.0.1:8000", "", new ObjectMapper()),
                new AiPlannerClient("http://user@127.0.0.1:8000", TOKEN, new ObjectMapper()) }) {
            assertEquals(QueryPlanException.Code.SERVICE_UNAVAILABLE,
                    assertThrows(QueryPlanException.class, () -> client.plan(request)).code());
        }
    }

    @Test
    void authenticatesOnlyAsServiceAndRejectsBadEnvelopes() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<String> token = new AtomicReference<>();
        AtomicReference<String> reply = new AtomicReference<>(
                "{\"status\":\"needs_clarification\",\"question\":\"¿Cuándo?\","
                        + "\"reasonCode\":\"MISSING_DATE_RANGE\"}");
        AtomicInteger status = new AtomicInteger(200);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/v1/plan", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            token.set(exchange.getRequestHeaders().getFirst("X-Internal-Service-Token"));
            byte[] bytes = reply.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status.get(), status.get() == 200 ? bytes.length : -1);
            try (var output = exchange.getResponseBody()) {
                if (status.get() == 200) output.write(bytes);
            }
        });
        server.start();
        var client = new AiPlannerClient("http://127.0.0.1:" + server.getAddress().getPort(),
                TOKEN, new ObjectMapper());
        assertEquals("needs_clarification", client.plan(request()).status());
        assertEquals("¿Cuándo?", client.plan(request()).question());
        assertEquals(TOKEN, token.get());
        assertFalse(body.get().contains("tenantSlug"));
        assertFalse(body.get().contains("Authorization"));
        assertTrue(body.get().contains("employees.roster"));

        for (String invalid : new String[] {
                "{\"status\":\"query_plan\",\"plan\":{},\"sql\":\"SELECT 1\"}",
                "{\"status\":\"query_plan\",\"plan\":null}",
                "{\"status\":\"query_plan\",\"status\":\"unsupported\",\"plan\":{}}",
                "{\"status\":\"needs_clarification\",\"reasonCode\":\"MISSING_DATE_RANGE\"}",
                "{\"status\":\"unsupported\",\"message\":\"x\",\"reasonCode\":\"UNKNOWN\"}",
                "{broken" }) {
            reply.set(invalid);
            assertEquals(AiFlowException.Code.INVALID_PLANNER_RESPONSE,
                    assertThrows(AiFlowException.class, () -> client.plan(request())).code());
        }
        reply.set("{\"status\":\"unsupported\",\"message\":\"x\","
                + "\"reasonCode\":\"UNRECOGNIZED_REQUEST\"}");
        status.set(401);
        assertEquals(QueryPlanException.Code.SERVICE_UNAVAILABLE,
                assertThrows(QueryPlanException.class, () -> client.plan(request())).code());
        status.set(502);
        assertEquals(AiFlowException.Code.INVALID_PLANNER_RESPONSE,
                assertThrows(AiFlowException.class, () -> client.plan(request())).code());
        status.set(504);
        assertEquals(AiFlowException.Code.PLANNER_TIMEOUT,
                assertThrows(AiFlowException.class, () -> client.plan(request())).code());
        status.set(200);
        reply.set("x".repeat(128 * 1024 + 1));
        assertEquals(AiFlowException.Code.INVALID_PLANNER_RESPONSE,
                assertThrows(AiFlowException.class, () -> client.plan(request())).code());
        server.stop(0);
        server = null;
        assertEquals(QueryPlanException.Code.SERVICE_UNAVAILABLE,
                assertThrows(QueryPlanException.class, () -> client.plan(request())).code());
    }

    @Test
    void oversizedConversationContextDropsThePlanAndRetainsOnlyBoundedMetadata() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/v1/plan", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = "{\"status\":\"unsupported\",\"message\":\"No disponible\","
                    .concat("\"reasonCode\":\"UNRECOGNIZED_REQUEST\"}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (var output = exchange.getResponseBody()) { output.write(bytes); }
        });
        server.start();

        ObjectMapper mapper = new ObjectMapper();
        var client = new AiPlannerClient("http://127.0.0.1:" + server.getAddress().getPort(),
                TOKEN, "mock", mapper, 200);
        JsonNode plan = mapper.readTree("""
                {"version":"1","catalogVersion":"1","dataset":"employees.roster",
                 "mode":"detail","asOfDate":"2026-10-01","select":["employee.full_name"],
                 "metrics":[],"filters":[{"field":"department.name","op":"eq","value":"Synthetic Department"}],
                 "groupBy":[],"having":[],"orderBy":[],"page":{"number":0,"size":20},
                 "visualization":{"type":"table"}}
                """);
        var context = new AiPlannerClient.ConversationContext(true, plan, "1", "1",
                java.util.List.of(new AiPlannerClient.MessageMetadata("USER", "TEXT", "COMPLETED")));
        client.plan(request().withConversationContext(context));

        JsonNode sent = mapper.readTree(body.get()).path("conversationContext");
        assertFalse(sent.path("hasUsablePlan").asBoolean());
        assertTrue(sent.path("lastSuccessfulPlan").isNull());
        assertEquals(1, sent.path("recentMessages").size());
        assertFalse(body.get().contains("Synthetic Department"));
        assertFalse(body.get().contains("rows"));
    }

    private static AiPlannerClient.Request request() {
        return new AiPlannerClient.Request("1", "retrasos por departamento", "1", Map.of(
                "employees.roster", new AnalyticAuthorizationPolicy.DatasetCapabilities(
                        true, true, Set.of("employee.full_name"), Set.of("employees.distinct_count"))));
    }
}
