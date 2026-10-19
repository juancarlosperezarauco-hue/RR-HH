package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    private static AiPlannerClient.Request request() {
        return new AiPlannerClient.Request("1", "retrasos por departamento", "1", Map.of(
                "employees.roster", new AnalyticAuthorizationPolicy.DatasetCapabilities(
                        true, true, Set.of("employee.full_name"), Set.of("employees.distinct_count"))));
    }
}
