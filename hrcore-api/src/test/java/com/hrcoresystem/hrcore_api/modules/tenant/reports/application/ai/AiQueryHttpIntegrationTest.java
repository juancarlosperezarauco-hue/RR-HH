package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrcoresystem.hrcore_api.bootstrap.platform.PlatformBootstrapRunner;
import com.hrcoresystem.hrcore_api.common.exception.BusinessConflictException;
import com.hrcoresystem.hrcore_api.common.security.jwt.AccessTokenPayload;
import com.hrcoresystem.hrcore_api.common.security.jwt.JwtTokenService;
import com.hrcoresystem.hrcore_api.common.tenancy.TenancyProperties;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContext;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.common.tenancy.schema.TenantSchemaNamingStrategy;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.service.AiConversationTurnPersistence;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import software.amazon.awssdk.services.s3.S3Client;

import static org.junit.jupiter.api.Assertions.*;

/** Full HTTP route with the real filters, local FastAPI, and two synthetic PostgreSQL schemas. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "AI_REPORTS_TEST_JDBC_URL", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AiQueryHttpIntegrationTest {
    private static final String TOKEN = "synthetic-service-token-32-characters-minimum";
    private static final String ALL = "hr.reports.employees.read,employees.list,departments.list,"
            + "hr.reports.attendance.read,attendance.records.list";
    private static final String URL = System.getenv("AI_REPORTS_TEST_JDBC_URL");
    private static final String USER = System.getenv("AI_REPORTS_TEST_DB_USER");
    private static final String PASSWORD = System.getenv("AI_REPORTS_TEST_DB_PASSWORD");
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> URL);
        registry.add("spring.datasource.username", () -> USER);
        registry.add("spring.datasource.password", () -> PASSWORD);
        registry.add("spring.flyway.enabled", () -> false);
        registry.add("app.security.jwt.secret", () -> "synthetic-jwt-secret-at-least-32-characters-long");
        registry.add("app.bootstrap.superadmin.email", () -> "ai-phase3@example.invalid");
        registry.add("app.bootstrap.superadmin.password", () -> "synthetic-password-123");
        registry.add("app.ai.planner.url", () -> System.getenv("AI_PLANNER_TEST_URL"));
        registry.add("app.ai.planner.service-token", () -> TOKEN);
        registry.add("app.auth.face-recognition.enabled", () -> false);
        registry.add("hrcore.attendance.processing-ms", () -> 3_600_000_000L);
    }

    @MockitoBean private PlatformBootstrapRunner bootstrap;
    @MockitoBean private S3Client documentS3Client;
    @Autowired private JwtTokenService tokens;
    @Autowired private AiConversationTurnPersistence turnPersistence;
    @LocalServerPort private int port;
    private final ObjectMapper mapper = new ObjectMapper();
    private String slugA, slugB, schemaA, schemaB;
    private UUID tenantA, tenantB, userA, userB, userA2;
    private long catalogVersion;

    @BeforeAll
    void seed() throws SQLException {
        assertTrue(URL.matches("jdbc:postgresql://(localhost|127\\.0\\.0\\.1):[0-9]+/ai_reports_test"));
        assertTrue(System.getenv("AI_PLANNER_TEST_URL").startsWith("http://127.0.0.1:"));
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        slugA = "ai3-a-" + suffix;
        slugB = "ai3-b-" + suffix;
        TenantSchemaNamingStrategy naming = new TenantSchemaNamingStrategy(new TenancyProperties());
        schemaA = naming.toSchemaName(slugA);
        schemaB = naming.toSchemaName(slugB);
        tenantA = UUID.randomUUID();
        tenantB = UUID.randomUUID();
        Flyway.configure().dataSource(URL, USER, PASSWORD)
                .locations("classpath:db/migration/public").schemas("public")
                .defaultSchema("public").load().migrate();
        try (Connection publicDb = connection("public")) {
            catalogVersion = scalarLong(publicDb,
                    "SELECT catalog_version FROM authorization_catalog_state WHERE id=1");
        }
        setupTenant(tenantA, slugA, schemaA, "Ada", "Sales", 12);
        setupTenant(tenantB, slugB, schemaB, "Bora", "Support", 99);
        userA2 = UUID.randomUUID();
        try (Connection tenant = connection(schemaA)) {
            execute(tenant, "INSERT INTO tenant_users(id,email,password_hash,first_name,last_name) "
                    + "VALUES (?,?,?,?,?)", userA2, "second-user@example.invalid", "synthetic-hash",
                    "Second", "User");
        }
    }

    @AfterAll
    void cleanup() throws SQLException {
        if (schemaA == null) return;
        try (Connection publicDb = connection("public")) {
            execute(publicDb, "DELETE FROM platform_tenants WHERE id IN (?,?)", tenantA, tenantB);
            execute(publicDb, "DROP SCHEMA IF EXISTS " + schemaA + " CASCADE");
            execute(publicDb, "DROP SCHEMA IF EXISTS " + schemaB + " CASCADE");
        }
    }

    @Test
    void realDetailAndAggregateStayInsideEachTenant() throws Exception {
        String a = session(schemaA, slugA, userA, ALL);
        String b = session(schemaB, slugB, userB, ALL);
        Reply detailA = ask(slugA, a, "listar empleados al 2026-01-15");
        Reply detailB = ask(slugB, b, "listar empleados al 2026-01-15");
        assertEquals(200, detailA.status());
        assertEquals("ok", detailA.body().get("status").asText());
        assertEquals("Ada Tenant", detailA.body().at("/result/rows/0/employee.full_name").asText());
        assertEquals("Bora Tenant", detailB.body().at("/result/rows/0/employee.full_name").asText());
        assertEquals("Sales", detailA.body().at("/result/rows/0/department.name").asText());
        assertEquals("Support", detailB.body().at("/result/rows/0/department.name").asText());

        String question = "retrasos por departamento desde 2026-01-01 hasta antes de 2026-02-01";
        Reply aggregateA = ask(slugA, a, question);
        Reply aggregateB = ask(slugB, b, question);
        assertEquals(200, aggregateA.status());
        assertEquals(200, aggregateB.status());
        assertEquals(12, aggregateA.body().at("/result/rows/0/attendance.late_minutes_total").asInt());
        assertEquals(99, aggregateB.body().at("/result/rows/0/attendance.late_minutes_total").asInt());
        assertEquals(1, aggregateA.body().at("/result/rows/0/attendance.late_days").asInt());
    }

    @Test
    void clarificationAndUnsupportedDoNotReturnRows() throws Exception {
        String jwt = session(schemaA, slugA, userA, ALL);
        Reply clarification = ask(slugA, jwt, "retrasos por departamento");
        Reply unsupported = ask(slugA, jwt, "reporte de nómina");
        assertEquals(200, clarification.status());
        assertEquals("needs_clarification", clarification.body().get("status").asText());
        assertFalse(clarification.body().has("result"));
        assertEquals("unsupported", unsupported.body().get("status").asText());
        assertFalse(unsupported.body().has("result"));
    }

    @Test
    void realJwtSessionAndTenantFiltersRejectInvalidAccess() throws Exception {
        String jwt = session(schemaA, slugA, userA, ALL);
        assertEquals(401, ask(slugA, "invalid.jwt", "listar empleados al 2026-01-15").status());
        assertEquals(401, ask(slugB, jwt, "listar empleados al 2026-01-15").status());
        String noReport = session(schemaA, slugA, userA, "employees.list,departments.list");
        assertEquals(403, ask(slugA, noReport, "listar empleados al 2026-01-15").status());
        try (Connection tenant = connection(schemaA)) {
            UUID sessionId = tokens.parseAccessToken(jwt).sessionId();
            execute(tenant, "UPDATE tenant_auth_sessions SET revoked_at=NOW() WHERE id=?", sessionId);
        }
        assertEquals(401, ask(slugA, jwt, "listar empleados al 2026-01-15").status());
        String newlyIssued = session(schemaA, slugA, userA, ALL);
        try (Connection tenant = connection(schemaA)) {
            execute(tenant, "UPDATE tenant_authorization_state SET authorization_version="
                    + "authorization_version+1 WHERE id=1");
        }
        assertEquals(401, ask(slugA, newlyIssued, "listar empleados al 2026-01-15").status());
    }

    @Test
    void httpBodyAndDatasetCapabilitiesAreConstrained() throws Exception {
        String jwt = session(schemaB, slugB, userB, ALL);
        assertEquals(400, send(slugB, jwt, "{\"question\":\"listar empleados al 2026-01-15\","
                + "\"sql\":\"SELECT 1\"}").status());
        assertEquals(400, send(slugB, jwt, "{\"question\":\"a\",\"question\":\"b\"}").status());
        assertEquals(400, send(slugB, jwt, "{\"question\":\"" + "x".repeat(17_000) + "\"}").status());
        String attendanceOnly = session(schemaB, slugB, userB,
                "hr.reports.attendance.read,attendance.records.list");
        Reply crossModule = ask(slugB, attendanceOnly, "listar empleados al 2026-01-15");
        assertEquals(200, crossModule.status());
        assertEquals("unsupported", crossModule.body().get("status").asText());
        assertEquals("CAPABILITY_NOT_AVAILABLE", crossModule.body().get("reasonCode").asText());
        assertFalse(crossModule.body().has("result"));
    }

    @Test
    void conversationsAreTenantAndOwnerScopedAndHistoryStartsEmpty() throws Exception {
        String owner = session(schemaA, slugA, userA, ALL);
        String secondUser = session(schemaA, slugA, userA2, ALL);
        String tenantBUser = session(schemaB, slugB, userB, ALL);

        Reply created = conversation(slugA, owner, "POST", "/api/hr/ai/conversations", null);
        assertEquals(201, created.status());
        UUID conversationId = UUID.fromString(created.body().at("/data/id").asText());
        assertEquals("Nuevo informe", created.body().at("/data/title").asText());
        Reply secondCreated = conversation(slugA, owner, "POST", "/api/hr/ai/conversations", null);
        assertEquals(201, secondCreated.status());

        Reply ownerList = conversation(slugA, owner, "GET", "/api/hr/ai/conversations?page=0&size=1", null);
        Reply ownerListSecondPage = conversation(slugA, owner, "GET",
                "/api/hr/ai/conversations?page=1&size=1", null);
        Reply otherUserList = conversation(slugA, secondUser, "GET",
                "/api/hr/ai/conversations?page=0&size=1", null);
        assertEquals(2, ownerList.body().at("/data/totalElements").asInt(), ownerList.body().toPrettyString());
        assertEquals(1, ownerList.body().at("/data/content").size());
        assertEquals(1, ownerListSecondPage.body().at("/data/content").size());
        assertNotEquals(ownerList.body().at("/data/content/0/id").asText(),
                ownerListSecondPage.body().at("/data/content/0/id").asText());
        assertEquals(0, otherUserList.body().at("/data/totalElements").asInt(),
                otherUserList.body().toPrettyString());

        assertEquals(200, conversation(slugA, owner, "GET",
                "/api/hr/ai/conversations/" + conversationId, null).status());
        Reply history = conversation(slugA, owner, "GET",
                "/api/hr/ai/conversations/" + conversationId + "/messages?page=0&size=10", null);
        assertEquals(200, history.status());
        assertEquals(0, history.body().at("/data/totalElements").asInt());
        assertTrue(history.body().at("/data/content").isEmpty());

        assertEquals(404, conversation(slugA, secondUser, "GET",
                "/api/hr/ai/conversations/" + conversationId, null).status());
        assertEquals(404, conversation(slugA, secondUser, "DELETE",
                "/api/hr/ai/conversations/" + conversationId, null).status());
        assertEquals(404, conversation(slugB, tenantBUser, "GET",
                "/api/hr/ai/conversations/" + conversationId, null).status());
        assertEquals(403, conversation(slugA,
                session(schemaA, slugA, userA, "employees.list,departments.list"),
                "GET", "/api/hr/ai/conversations", null).status());

        String ownerWithoutReportPermission = session(schemaA, slugA, userA, "employees.list");
        assertEquals(200, conversation(slugA, ownerWithoutReportPermission, "DELETE",
                "/api/hr/ai/conversations/" + conversationId, null).status());
        assertEquals(404, conversation(slugA, owner, "GET",
                "/api/hr/ai/conversations/" + conversationId, null).status());
    }

    @Test
    void conversationCreationRejectsClientSuppliedOwnerOrTenant() throws Exception {
        String jwt = session(schemaA, slugA, userA, ALL);
        Reply rejected = conversation(slugA, jwt, "POST", "/api/hr/ai/conversations",
                "{\"owner_user_id\":\"" + UUID.randomUUID() + "\",\"tenant\":\"other\"}");
        assertEquals(400, rejected.status());
    }

    @Test
    void postgresSerializesConcurrentReservationsAndRejectsStaleCompletion() throws Exception {
        String owner = session(schemaA, slugA, userA, ALL);
        UUID conversationId = UUID.fromString(conversation(slugA, owner, "POST",
                "/api/hr/ai/conversations", null).body().at("/data/id").asText());
        CountDownLatch start = new CountDownLatch(1);
        CompletableFuture<ReservationAttempt> first = CompletableFuture.supplyAsync(() -> {
            await(start);
            return reserveAttempt(conversationId, UUID.randomUUID(), "turno sintetico uno");
        });
        CompletableFuture<ReservationAttempt> second = CompletableFuture.supplyAsync(() -> {
            await(start);
            return reserveAttempt(conversationId, UUID.randomUUID(), "turno sintetico dos");
        });
        start.countDown();
        List<ReservationAttempt> attempts = List.of(first.join(), second.join());
        List<ReservationAttempt> winners = attempts.stream().filter(attempt -> attempt.reservation() != null).toList();
        assertEquals(1, winners.size());
        ReservationAttempt rejected = attempts.stream().filter(attempt -> attempt.failure() != null)
                .findFirst().orElseThrow();
        assertTrue(rejected.failure() instanceof BusinessConflictException
                || rejected.failure() instanceof org.springframework.orm.ObjectOptimisticLockingFailureException,
                rejected.failure().toString());

        AiConversationTurnPersistence.Reservation reservation = winners.getFirst().reservation();
        try (Connection tenant = connection(schemaA)) {
            // Simulate another transaction changing the conversation while the planner is in flight.
            execute(tenant, "UPDATE ai_conversations SET version=version+1 WHERE id=?", conversationId);
        }
        TenantContextHolder.set(new TenantContext(slugA, schemaA, false));
        try {
            assertThrows(BusinessConflictException.class, () -> turnPersistence.complete(conversationId, userA,
                    reservation.request().getId(), reservation.conversationVersion(), "COMPLETED", "REPORT",
                    "Reporte sintetico", null, null, 1));
        } finally {
            TenantContextHolder.clear();
        }
        try (Connection tenant = connection(schemaA)) {
            assertEquals(1, scalarLong(tenant, "SELECT COUNT(*) FROM ai_messages "
                    + "WHERE conversation_id=? AND role='USER' AND state='PENDING'", conversationId));
            assertEquals(0, scalarLong(tenant, "SELECT COUNT(*) FROM ai_report_runs WHERE conversation_id=?",
                    conversationId));
        }
    }

    @Test
    void conversationTurnIsPersistedIdempotentlyAndRefreshReadsCurrentTenantData() throws Exception {
        String owner = session(schemaA, slugA, userA, ALL);
        String sameTenantOtherUser = session(schemaA, slugA, userA2, ALL);
        String tenantBUser = session(schemaB, slugB, userB, ALL);
        UUID conversationId = UUID.fromString(conversation(slugA, owner, "POST",
                "/api/hr/ai/conversations", null).body().at("/data/id").asText());
        UUID clientMessageId = UUID.randomUUID();
        String requestBody = mapper.writeValueAsString(java.util.Map.of(
                "clientMessageId", clientMessageId, "message", "listar empleados al 2026-01-15"));
        String messagesPath = "/api/hr/ai/conversations/" + conversationId + "/messages";
        assertEquals(400, conversation(slugA, owner, "POST", messagesPath,
                mapper.writeValueAsString(java.util.Map.of("clientMessageId", UUID.randomUUID(),
                        "message", "pregunta", "owner_user_id", userA.toString()))).status());
        assertEquals(400, conversation(slugA, owner, "POST", messagesPath,
                "{\"clientMessageId\":\"" + UUID.randomUUID()
                        + "\",\"clientMessageId\":\"" + UUID.randomUUID()
                        + "\",\"message\":\"pregunta\"}").status());
        assertEquals(400, conversation(slugA, owner, "POST", messagesPath,
                mapper.writeValueAsString(java.util.Map.of("clientMessageId", UUID.randomUUID(),
                        "message", "x".repeat(4001)))).status());

        Reply first = conversation(slugA, owner, "POST", messagesPath, requestBody);
        assertEquals(200, first.status(), first.body().toPrettyString());
        assertEquals("ok", first.body().at("/data/status").asText());
        assertEquals("Ada Tenant", first.body().at("/data/result/rows/0/employee.full_name").asText());
        assertTrue(first.body().at("/data/reportRunId").isTextual());
        UUID firstRunId = UUID.fromString(first.body().at("/data/reportRunId").asText());
        Reply savedHistory = conversation(slugA, owner, "GET", messagesPath + "?page=0&size=10", null);
        assertEquals(2, savedHistory.body().at("/data/totalElements").asInt());
        assertEquals("USER", savedHistory.body().at("/data/content/0/role").asText());
        assertFalse(savedHistory.body().toPrettyString().contains("Ada Tenant"));

        Reply retry = conversation(slugA, owner, "POST", messagesPath, requestBody);
        assertEquals(200, retry.status(), retry.body().toPrettyString());
        assertEquals("ok", retry.body().at("/data/status").asText());
        assertEquals(firstRunId.toString(), retry.body().at("/data/reportRunId").asText());
        assertFalse(retry.body().at("/data").has("result"), "replay must not expose cached rows");
        assertEquals(409, conversation(slugA, owner, "POST", messagesPath,
                mapper.writeValueAsString(java.util.Map.of("clientMessageId", clientMessageId,
                        "message", "reutilizar id con otro contenido"))).status());
        assertEquals(404, conversation(slugA, sameTenantOtherUser, "POST", messagesPath, requestBody).status());
        assertEquals(404, conversation(slugB, tenantBUser, "POST", messagesPath, requestBody).status());

        Reply unsupported = conversation(slugA, owner, "POST", messagesPath,
                mapper.writeValueAsString(java.util.Map.of("clientMessageId", UUID.randomUUID(),
                        "message", "reporte de nómina")));
        assertEquals(200, unsupported.status(), unsupported.body().toPrettyString());
        assertEquals("unsupported", unsupported.body().at("/data/status").asText());
        assertFalse(unsupported.body().at("/data").has("result"));

        try (Connection tenant = connection(schemaA)) {
            UUID employeeId = scalarUuid(tenant, "SELECT id FROM employees LIMIT 1");
            execute(tenant, "UPDATE employees SET first_name='Ada Updated' WHERE id=?", employeeId);
            assertEquals(4, scalarLong(tenant, "SELECT COUNT(*) FROM ai_messages WHERE conversation_id=?",
                    conversationId));
            assertEquals(1, scalarLong(tenant, "SELECT COUNT(*) FROM ai_report_runs WHERE conversation_id=?",
                    conversationId));
            assertFalse(scalarText(tenant, "SELECT result_metadata::text FROM ai_report_runs WHERE id=?",
                    firstRunId).contains("Ada"));
        }

        Reply refreshed = conversation(slugA, owner, "POST", "/api/hr/ai/conversations/" + conversationId
                + "/reports/" + firstRunId + "/refresh", null);
        assertEquals(200, refreshed.status(), refreshed.body().toPrettyString());
        assertEquals("Ada Updated Tenant", refreshed.body().at("/data/result/rows/0/employee.full_name").asText());
        assertNotEquals(firstRunId.toString(), refreshed.body().at("/data/reportRunId").asText());
        try (Connection tenant = connection(schemaA)) {
            assertEquals(2, scalarLong(tenant, "SELECT COUNT(*) FROM ai_report_runs WHERE conversation_id=?",
                    conversationId));
        }

        String revoked = session(schemaA, slugA, userA, "hr.reports.employees.read,departments.list");
        Reply deniedRefresh = conversation(slugA, revoked, "POST", "/api/hr/ai/conversations/" + conversationId
                + "/reports/" + firstRunId + "/refresh", null);
        assertEquals(403, deniedRefresh.status(), deniedRefresh.body().toPrettyString());

        try (Connection tenant = connection(schemaA)) {
            execute(tenant, "UPDATE ai_report_runs SET catalog_version='obsolete' WHERE id=?", firstRunId);
        }
        Reply obsoleteCatalog = conversation(slugA, owner, "POST", "/api/hr/ai/conversations/" + conversationId
                + "/reports/" + firstRunId + "/refresh", null);
        assertEquals(409, obsoleteCatalog.status(), obsoleteCatalog.body().toPrettyString());

        assertEquals(200, conversation(slugA, owner, "DELETE",
                "/api/hr/ai/conversations/" + conversationId, null).status());
        try (Connection tenant = connection(schemaA)) {
            assertEquals(0, scalarLong(tenant, "SELECT COUNT(*) FROM ai_messages WHERE conversation_id=?",
                    conversationId));
            assertEquals(0, scalarLong(tenant, "SELECT COUNT(*) FROM ai_report_runs WHERE conversation_id=?",
                    conversationId));
        }

        UUID staleConversationId = UUID.fromString(conversation(slugA, owner, "POST",
                "/api/hr/ai/conversations", null).body().at("/data/id").asText());
        UUID staleMessageId = UUID.randomUUID();
        try (Connection tenant = connection(schemaA)) {
            execute(tenant, "INSERT INTO ai_messages(id,conversation_id,client_message_id,role,content,"
                            + "message_type,state,created_at) VALUES (?,?,?,'USER','stale synthetic message',"
                            + "'TEXT','PENDING',NOW()-INTERVAL '5 minutes')",
                    UUID.randomUUID(), staleConversationId, staleMessageId);
        }
        String staleRecoveryBody = mapper.writeValueAsString(java.util.Map.of(
                "clientMessageId", UUID.randomUUID(), "message", "listar empleados al 2026-01-15"));
        Reply afterRecovery = conversation(slugA, owner, "POST", "/api/hr/ai/conversations/"
                + staleConversationId + "/messages", staleRecoveryBody);
        assertEquals(200, afterRecovery.status(), afterRecovery.body().toPrettyString());
        try (Connection tenant = connection(schemaA)) {
            assertEquals("FAILED", scalarText(tenant,
                    "SELECT state FROM ai_messages WHERE conversation_id=? AND client_message_id=?",
                    staleConversationId, staleMessageId));
            assertEquals(4, scalarLong(tenant, "SELECT COUNT(*) FROM ai_messages WHERE conversation_id=?",
                    staleConversationId));
        }

        for (int attempt = 0; attempt < 5; attempt++) {
            UUID concurrentConversationId = UUID.fromString(conversation(slugA, owner, "POST",
                    "/api/hr/ai/conversations", null).body().at("/data/id").asText());
            UUID concurrentClientId = UUID.randomUUID();
            String concurrentBody = mapper.writeValueAsString(java.util.Map.of(
                    "clientMessageId", concurrentClientId, "message", "listar empleados al 2026-01-15"));
            String concurrentPath = "/api/hr/ai/conversations/" + concurrentConversationId + "/messages";
            CountDownLatch start = new CountDownLatch(1);
            CompletableFuture<Reply> firstCall = CompletableFuture.supplyAsync(() -> {
                await(start);
                try { return conversation(slugA, owner, "POST", concurrentPath, concurrentBody); }
                catch (Exception failure) { throw new RuntimeException(failure); }
            });
            CompletableFuture<Reply> secondCall = CompletableFuture.supplyAsync(() -> {
                await(start);
                try { return conversation(slugA, owner, "POST", concurrentPath, concurrentBody); }
                catch (Exception failure) { throw new RuntimeException(failure); }
            });
            start.countDown();
            Reply firstReply = firstCall.join();
            Reply secondReply = secondCall.join();
            assertTrue(List.of(200, 202).contains(firstReply.status()), firstReply.body().toPrettyString());
            assertTrue(List.of(200, 202).contains(secondReply.status()), secondReply.body().toPrettyString());
            try (Connection tenant = connection(schemaA)) {
                assertEquals(2, scalarLong(tenant, "SELECT COUNT(*) FROM ai_messages WHERE conversation_id=?",
                        concurrentConversationId));
                assertEquals(1, scalarLong(tenant, "SELECT COUNT(*) FROM ai_report_runs WHERE conversation_id=?",
                        concurrentConversationId));
            }
        }
    }

    private void setupTenant(UUID tenantId, String slug, String schema, String first,
                             String department, int late) throws SQLException {
        Flyway.configure().dataSource(URL, USER, PASSWORD)
                .locations("classpath:db/migration/tenant").schemas(schema)
                .defaultSchema(schema).createSchemas(true).load().migrate();
        try (Connection publicDb = connection("public")) {
            execute(publicDb, "INSERT INTO platform_tenants(id,name,slug,schema_name) VALUES (?,?,?,?)",
                    tenantId, first + " test tenant", slug, schema);
        }
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID positionId = UUID.randomUUID();
        try (Connection tenant = connection(schema)) {
            tenant.setAutoCommit(false);
            execute(tenant, "INSERT INTO tenant_users(id,email,password_hash,first_name,last_name) VALUES (?,?,?,?,?)",
                    userId, first.toLowerCase() + "@example.invalid", "synthetic-hash", first, "Tenant");
            execute(tenant, "UPDATE tenant_authorization_state SET owner_user_id=? WHERE id=1", userId);
            execute(tenant, "INSERT INTO tenant_user_roles(user_id,role_id) "
                    + "SELECT ?,id FROM tenant_roles WHERE code='OWNER_ADMIN'", userId);
            execute(tenant, "INSERT INTO departments(id,code,name) VALUES (?,?,?)",
                    departmentId, department.toLowerCase(), department);
            execute(tenant, "INSERT INTO positions(id,code,name) VALUES (?,?,?)",
                    positionId, "analyst", "Analyst");
            execute(tenant, "INSERT INTO employees(id,employee_number,first_name,last_name,"
                            + "document_type,document_number,hire_date) VALUES (?,?,?,?,?,?,?)",
                    employeeId, employeeId.toString(), first, "Tenant", "CI", employeeId.toString(),
                    LocalDate.of(2026, 1, 1));
            UUID period = scalarUuid(tenant,
                    "SELECT id FROM employment_periods WHERE employee_id=?", employeeId);
            execute(tenant, "INSERT INTO employee_assignments(employee_id,employment_period_id,"
                            + "department_id,position_id,start_date) VALUES (?,?,?,?,?)",
                    employeeId, period, departmentId, positionId, LocalDate.of(2026, 1, 1));
            execute(tenant, "INSERT INTO attendance_records(employee_id,work_date,status,late_minutes) "
                    + "VALUES (?,?,?,?)", employeeId, LocalDate.of(2026, 1, 10), "LATE", late);
            tenant.commit();
        }
        if (schema.equals(schemaA)) userA = userId; else userB = userId;
    }

    private String session(String schema, String slug, UUID userId, String permissions) throws SQLException {
        UUID sessionId = UUID.randomUUID();
        long authorizationVersion;
        try (Connection tenant = connection(schema)) {
            authorizationVersion = scalarLong(tenant,
                    "SELECT authorization_version FROM tenant_authorization_state WHERE id=1");
            execute(tenant, "INSERT INTO tenant_auth_sessions(id,user_id,token_family_id,"
                            + "refresh_token_hash,expires_at) VALUES (?,?,?,?,?)",
                    sessionId, userId, UUID.randomUUID(),
                    UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""),
                    java.sql.Timestamp.from(Instant.now().plusSeconds(3600)));
        }
        return tokens.generateAccessToken(new AccessTokenPayload(userId, sessionId, slug, "TENANT",
                "fictional@example.invalid", "Fictional", List.of("OWNER_ADMIN"),
                List.of(permissions.split(",")), 1L, authorizationVersion, catalogVersion,
                Instant.now(), Instant.now().plusSeconds(900), UUID.randomUUID()));
    }

    private Reply ask(String slug, String jwt, String question) throws Exception {
        String json = mapper.writeValueAsString(java.util.Map.of("question", question));
        return send(slug, jwt, json);
    }

    private Reply send(String slug, String jwt, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/hr/ai/query"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + jwt)
                .header("X-Tenant-Slug", slug)
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), mapper.readTree(response.body()));
    }

    private Reply conversation(String slug, String jwt, String method, String path, String body)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("Authorization", "Bearer " + jwt)
                .header("X-Tenant-Slug", slug);
        if (body != null) builder.header("Content-Type", "application/json");
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body);
        HttpResponse<String> response = HTTP.send(builder.method(method, publisher).build(),
                HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), response.body().isBlank()
                ? mapper.createObjectNode() : mapper.readTree(response.body()));
    }

    private record Reply(int status, JsonNode body) {}

    private static Connection connection(String schema) throws SQLException {
        Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
        connection.setSchema(schema);
        return connection;
    }

    private static void execute(Connection connection, String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            statement.executeUpdate();
        }
    }

    private static UUID scalarUuid(Connection connection, String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                return result.getObject(1, UUID.class);
            }
        }
    }

    private static long scalarLong(Connection connection, String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                return result.getLong(1);
            }
        }
    }

    private static String scalarText(Connection connection, String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                return result.getString(1);
            }
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, java.util.concurrent.TimeUnit.SECONDS))
                throw new IllegalStateException("Timed out starting concurrent synthetic requests");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted synthetic request", interrupted);
        }
    }

    private ReservationAttempt reserveAttempt(UUID conversationId, UUID clientMessageId, String message) {
        TenantContextHolder.set(new TenantContext(slugA, schemaA, false));
        try {
            return new ReservationAttempt(turnPersistence.reserve(conversationId, userA, clientMessageId,
                    message, Duration.ofMinutes(2)), null);
        } catch (RuntimeException failure) {
            return new ReservationAttempt(null, failure);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private record ReservationAttempt(AiConversationTurnPersistence.Reservation reservation,
                                      RuntimeException failure) {}
}
