package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.common.security.principal.AuthenticatedUserPrincipal;
import com.hrcoresystem.hrcore_api.common.tenancy.TenancyProperties;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContext;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.common.tenancy.datasource.TenantAwareDataSource;
import com.hrcoresystem.hrcore_api.common.tenancy.schema.TenantSchemaNamingStrategy;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service.TenantTimezoneService;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Uses only a disposable local ai_reports_test database, never the project's running database. */
@EnabledIfEnvironmentVariable(named = "AI_REPORTS_TEST_JDBC_URL", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AnalyticQueryPostgresIntegrationTest {
    private static final String AUTHORITIES = "hr.reports.attendance.read,attendance.records.list,"
            + "hr.reports.employees.read,employees.list,departments.list,positions.list";
    private String url, user, password, slugA, slugB, schemaA, schemaB;
    private UUID sharedEmployee, sales, support, other;
    private AnnotationConfigApplicationContext context;
    private AnalyticQueryService service;

    @BeforeAll
    void setUp() throws SQLException {
        url = System.getenv("AI_REPORTS_TEST_JDBC_URL");
        user = System.getenv("AI_REPORTS_TEST_DB_USER");
        password = System.getenv("AI_REPORTS_TEST_DB_PASSWORD");
        assertTrue(url.matches("jdbc:postgresql://(localhost|127\\.0\\.0\\.1):[0-9]+/ai_reports_test"),
                "Only a disposable local ai_reports_test database is allowed");
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        slugA = "ai-a-" + suffix;
        slugB = "ai-b-" + suffix;
        TenantSchemaNamingStrategy naming = new TenantSchemaNamingStrategy(new TenancyProperties());
        schemaA = naming.toSchemaName(slugA);
        schemaB = naming.toSchemaName(slugB);
        Flyway.configure().dataSource(url, user, password)
                .locations("classpath:db/migration/public").schemas("public")
                .defaultSchema("public").baselineOnMigrate(true).load().migrate();
        for (String schema : List.of(schemaA, schemaB))
            Flyway.configure().dataSource(url, user, password)
                    .locations("classpath:db/migration/tenant").schemas(schema)
                    .defaultSchema(schema).baselineOnMigrate(true).createSchemas(true).load().migrate();
        seed();
        context = new AnnotationConfigApplicationContext(TestBeans.class);
        service = context.getBean(AnalyticQueryService.class);
        assertTrue(AopUtils.isAopProxy(context.getBean(AnalyticQueryExecutor.class)));
    }

    @AfterAll
    void tearDown() throws SQLException {
        if (context != null) context.close();
        if (schemaA == null || schemaB == null) return;
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            update(connection, "DROP SCHEMA " + schemaA + " CASCADE");
            update(connection, "DROP SCHEMA " + schemaB + " CASCADE");
        }
    }

    @BeforeEach
    void tenantA() { authenticate(slugA, schemaA, AUTHORITIES.split(",")); }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    @Test
    void detailUsesHistoricalAssignmentLocalTimeAndBoundedPages() {
        QueryResultV1 result = service.execute(detail(0, 100));
        assertEquals(4, result.rows().size());
        assertFalse(result.hasMore());
        assertEquals("Ada TenantA", result.rows().get(0).get("employee.full_name"));
        assertEquals("Sales", result.rows().get(0).get("department.name"));
        assertEquals(LocalTime.of(8, 20), result.rows().get(0).get("attendance.first_in_local_time"));
        assertEquals("Support", result.rows().get(3).get("department.name"));
        assertEquals("Lead", result.rows().get(3).get("position.name"));
        assertNull(result.rows().get(2).get("department.name"));
        assertTrue(result.warnings().contains("MISSING_PRIMARY_ASSIGNMENT"));

        QueryResultV1 first = service.execute(detail(0, 1));
        QueryResultV1 second = service.execute(detail(1, 1));
        assertEquals(1, first.rows().size());
        assertTrue(first.hasMore());
        assertEquals(LocalDate.of(2026, 1, 11), second.rows().getFirst().get("attendance.work_date"));
    }

    @Test
    void aggregatesCountLateStatusAndSumOnlyLateMinutesWithoutDoubleCounting() {
        QueryResultV1 result = service.execute(aggregate());
        assertEquals(3, result.rows().size());
        assertEquals("Support", result.rows().get(0).get("department.name"));
        assertEquals(18L, result.rows().get(0).get("attendance.late_minutes_total"));
        assertEquals("Sales", result.rows().get(1).get("department.name"));
        assertEquals(2L, result.rows().get(1).get("attendance.record_count"));
        assertEquals(1L, result.rows().get(1).get("attendance.late_days"));
        assertEquals(12L, result.rows().get(1).get("attendance.late_minutes_total"));
        assertNull(result.rows().get(2).get("department.name"));
        assertEquals(10L, result.rows().get(2).get("attendance.late_minutes_total"));
    }

    @Test
    void localTimeFilterAndRosterAggregateUseTypedParameters() {
        String timed = detail(0, 100).replace("\"filters\":[]",
                "\"filters\":[{\"field\":\"attendance.first_in_local_time\","
                        + "\"op\":\"between\",\"value\":[\"08:00:00\",\"08:30:00\"]}]");
        QueryResultV1 filtered = service.execute(timed);
        assertEquals(3, filtered.rows().size());
        assertEquals(List.of(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 11),
                LocalDate.of(2026, 1, 12)), filtered.rows().stream()
                .map(row -> (LocalDate) row.get("attendance.work_date")).toList());

        String roster = QueryPlanValidationServiceTest.ROSTER.replace("2026-10-18", "2026-02-15");
        QueryResultV1 grouped = service.execute(roster);
        assertEquals(2, grouped.rows().size());
        assertEquals(1L, grouped.rows().stream().filter(row -> "Support".equals(row.get("department.name")))
                .findFirst().orElseThrow().get("employees.distinct_count"));
        assertEquals(1L, grouped.rows().stream().filter(row -> row.get("department.name") == null)
                .findFirst().orElseThrow().get("employees.distinct_count"));

        String ungrouped = aggregate().replace("\"select\":[\"department.name\"]", "\"select\":[]")
                .replace("\"groupBy\":[\"department.id\"]", "\"groupBy\":[]");
        assertTrue(service.execute(ungrouped).warnings().contains("MISSING_PRIMARY_ASSIGNMENT"));
    }

    @Test
    void executorUsesReadOnlyTransactionAndThreeSecondDatabaseTimeout() {
        AnalyticQueryExecutor executor = context.getBean(AnalyticQueryExecutor.class);
        var controls = new QueryPlanSqlCompiler.CompiledQuery(
                "SELECT current_setting('transaction_read_only') AS c0, "
                        + "current_setting('statement_timeout') AS c1, "
                        + "false AS missing_primary_assignment",
                List.of(), List.of(new QueryResultV1.Column("read_only", SemanticCatalogV1.ValueType.TEXT),
                        new QueryResultV1.Column("timeout", SemanticCatalogV1.ValueType.TEXT)), 0, 1);
        Map<String, Object> settings = executor.execute(controls).rows().getFirst();
        assertEquals("on", settings.get("read_only"));
        assertEquals("3s", settings.get("timeout"));

        var slow = new QueryPlanSqlCompiler.CompiledQuery("SELECT 'slow' AS c0 FROM pg_sleep(4)",
                List.of(), List.of(new QueryResultV1.Column("value", SemanticCatalogV1.ValueType.TEXT)), 0, 1);
        QueryPlanException failure = assertThrows(QueryPlanException.class, () -> executor.execute(slow));
        assertEquals(QueryPlanException.Code.QUERY_TIMEOUT, failure.code());
    }

    @Test
    void rosterAndAttendanceFollowPeriodAndAssignmentWhenTenantAndDateChange() {
        assertEquals("Sales", employee(sharedEmployee, "2026-01-15").get("department.name"));
        assertEquals("Support", employee(sharedEmployee, "2026-02-15").get("department.name"));

        authenticate(slugB, schemaB, AUTHORITIES.split(","));
        Map<String, Object> january = employee(sharedEmployee, "2026-01-15");
        Map<String, Object> february = employee(sharedEmployee, "2026-02-15");
        assertEquals("Bora TenantB", january.get("employee.full_name"));
        assertEquals("Other", january.get("department.name"));
        assertEquals("New Team", february.get("department.name"));
        assertNotEquals(january.get("employment_period.id"), february.get("employment_period.id"));
        QueryResultV1 tenantB = service.execute(detail(0, 100));
        assertEquals(2, tenantB.rows().size());
        assertTrue(tenantB.rows().stream().allMatch(row -> "Bora TenantB".equals(row.get("employee.full_name"))));

        authenticate(slugA, schemaA, AUTHORITIES.split(","));
        assertEquals(4, service.execute(detail(0, 100)).rows().size());
        assertEquals("Ada TenantA", employee(sharedEmployee, "2026-02-15").get("employee.full_name"));
    }

    private Map<String, Object> employee(UUID id, String date) {
        String json = """
                {"version":"1","catalogVersion":"1","dataset":"employees.roster","mode":"detail",
                 "asOfDate":"%s","select":["employee.full_name","employment_period.id","department.name","position.name"],
                 "metrics":[],"filters":[{"field":"employee.id","op":"eq","value":"%s"}],
                 "groupBy":[],"having":[],"orderBy":[],"page":{"number":0,"size":100},
                 "visualization":{"type":"table"}}
                """.formatted(date, id);
        return service.execute(json).rows().getFirst();
    }

    private static String detail(int page, int size) {
        return """
                {"version":"1","catalogVersion":"1","dataset":"attendance.daily","mode":"detail",
                 "select":["employee.full_name","department.name","position.name","attendance.work_date",
                           "attendance.first_in_local_time","attendance.status","attendance.late_minutes"],
                 "metrics":[],"dateRange":{"field":"attendance.work_date","from":"2026-01-01","toExclusive":"2026-03-01"},
                 "filters":[],"groupBy":[],"having":[],
                 "orderBy":[{"field":"attendance.work_date","direction":"ASC"}],
                 "page":{"number":%d,"size":%d},"visualization":{"type":"table"}}
                """.formatted(page, size);
    }

    private static String aggregate() {
        return """
                {"version":"1","catalogVersion":"1","dataset":"attendance.daily","mode":"aggregate",
                 "select":["department.name"],
                 "metrics":["attendance.record_count","attendance.late_days","attendance.late_minutes_total"],
                 "dateRange":{"field":"attendance.work_date","from":"2026-01-01","toExclusive":"2026-03-01"},
                 "filters":[],"groupBy":["department.id"],
                 "having":[{"metric":"attendance.late_days","op":"gt","value":0}],
                 "orderBy":[{"field":"attendance.late_minutes_total","direction":"DESC"}],
                 "page":{"number":0,"size":100},"visualization":{"type":"table"}}
                """;
    }

    private void seed() throws SQLException {
        sharedEmployee = UUID.randomUUID();
        sales = UUID.randomUUID();
        support = UUID.randomUUID();
        other = UUID.randomUUID();
        UUID analyst = UUID.randomUUID(), lead = UUID.randomUUID(), newTeam = UUID.randomUUID();
        UUID ben = UUID.randomUUID();
        try (Connection connection = connection(schemaA)) {
            department(connection, sales, "Sales");
            department(connection, support, "Support");
            position(connection, analyst, "Analyst");
            position(connection, lead, "Lead");
            employee(connection, sharedEmployee, "Ada", "TenantA");
            employee(connection, ben, "Ben", "TenantA");
            UUID period = period(connection, sharedEmployee);
            assignment(connection, sharedEmployee, period, sales, analyst, "2026-01-01", "2026-01-31");
            assignment(connection, sharedEmployee, period, support, lead, "2026-02-01", null);
            attendance(connection, sharedEmployee, "2026-01-10", "LATE", 12, "2026-01-10T12:20:00Z");
            attendance(connection, sharedEmployee, "2026-01-11", "PRESENT", 7, "2026-01-11T12:05:00Z");
            attendance(connection, ben, "2026-01-12", "LATE", 10, "2026-01-12T12:10:00Z");
            attendance(connection, sharedEmployee, "2026-02-10", "LATE", 18, "2026-02-10T13:30:00Z");
        }
        try (Connection connection = connection(schemaB)) {
            department(connection, other, "Other");
            department(connection, newTeam, "New Team");
            position(connection, analyst, "Analyst");
            employee(connection, sharedEmployee, "Bora", "TenantB");
            UUID prior = period(connection, sharedEmployee);
            assignment(connection, sharedEmployee, prior, other, analyst, "2026-01-01", "2026-01-31");
            attendance(connection, sharedEmployee, "2026-01-10", "LATE", 99, "2026-01-10T12:40:00Z");
            connection.setAutoCommit(false);
            try {
                update(connection, "UPDATE employment_periods SET status='TERMINATED', end_date=? WHERE id=?",
                        LocalDate.of(2026, 1, 31), prior);
                update(connection, "INSERT INTO employee_terminations(employee_id,employment_period_id,effective_date,reason) "
                                + "VALUES (?,?,?,?)", sharedEmployee, prior, LocalDate.of(2026, 1, 31),
                        "Synthetic termination before rehire");
                update(connection, "INSERT INTO employment_periods(employee_id,start_date,status) VALUES (?,?,'ACTIVE')",
                        sharedEmployee, LocalDate.of(2026, 2, 1));
                connection.commit();
            } catch (SQLException failure) {
                connection.rollback();
                throw failure;
            } finally {
                connection.setAutoCommit(true);
            }
            UUID latest = periodOn(connection, sharedEmployee, LocalDate.of(2026, 2, 1));
            assignment(connection, sharedEmployee, latest, newTeam, analyst, "2026-02-01", null);
            attendance(connection, sharedEmployee, "2026-02-10", "LATE", 88, "2026-02-10T12:50:00Z");
        }
    }

    private Connection connection(String schema) throws SQLException {
        Connection connection = DriverManager.getConnection(url, user, password);
        connection.setSchema(schema);
        return connection;
    }

    private static void department(Connection c, UUID id, String name) throws SQLException {
        update(c, "INSERT INTO departments(id,code,name) VALUES (?,?,?)", id, name.toLowerCase().replace(' ', '_'), name);
    }

    private static void position(Connection c, UUID id, String name) throws SQLException {
        update(c, "INSERT INTO positions(id,code,name) VALUES (?,?,?)", id, name.toLowerCase(), name);
    }

    private static void employee(Connection c, UUID id, String first, String last) throws SQLException {
        update(c, "INSERT INTO employees(id,employee_number,first_name,last_name,document_type,document_number,hire_date) "
                        + "VALUES (?,?,?,?,?,?,?)", id, id.toString(), first, last, "CI", id.toString(),
                LocalDate.of(2026, 1, 1));
    }

    private static UUID period(Connection c, UUID employee) throws SQLException {
        return periodOn(c, employee, LocalDate.of(2026, 1, 1));
    }

    private static UUID periodOn(Connection c, UUID employee, LocalDate date) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(
                "SELECT id FROM employment_periods WHERE employee_id=? AND start_date=?")) {
            p.setObject(1, employee);
            p.setObject(2, date);
            try (ResultSet result = p.executeQuery()) {
                assertTrue(result.next());
                return result.getObject(1, UUID.class);
            }
        }
    }

    private static void assignment(Connection c, UUID employee, UUID period, UUID department,
                                   UUID position, String from, String to) throws SQLException {
        update(c, "INSERT INTO employee_assignments(employee_id,employment_period_id,department_id,position_id,"
                        + "start_date,end_date) VALUES (?,?,?,?,?,?)", employee, period, department, position,
                LocalDate.parse(from), to == null ? null : LocalDate.parse(to));
    }

    private static void attendance(Connection c, UUID employee, String date, String status,
                                   int late, String firstIn) throws SQLException {
        update(c, "INSERT INTO attendance_records(employee_id,work_date,status,late_minutes,actual_first_in_at) "
                        + "VALUES (?,?,?,?,?)", employee, LocalDate.parse(date), status, late,
                Timestamp.from(Instant.parse(firstIn)));
    }

    private static void update(Connection c, String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = c.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            statement.executeUpdate();
        }
    }

    private static void authenticate(String slug, String schema, String... authorities) {
        TenantContextHolder.set(new TenantContext(slug, schema, false));
        var principal = new AuthenticatedUserPrincipal(UUID.randomUUID(), UUID.randomUUID(),
                "fictional@example.invalid", "Fictional", slug, "TENANT", List.of(),
                Arrays.asList(authorities), 1, 1L, 1);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }

    @Configuration
    @EnableTransactionManagement(proxyTargetClass = true)
    static class TestBeans {
        @Bean TenancyProperties tenancyProperties() { return new TenancyProperties(); }
        @Bean TenantSchemaNamingStrategy naming(TenancyProperties properties) {
            return new TenantSchemaNamingStrategy(properties);
        }
        @Bean DataSource dataSource(TenancyProperties properties) {
            DataSource physical = new DriverManagerDataSource(System.getenv("AI_REPORTS_TEST_JDBC_URL"),
                    System.getenv("AI_REPORTS_TEST_DB_USER"), System.getenv("AI_REPORTS_TEST_DB_PASSWORD"));
            return new TenantAwareDataSource(physical, properties);
        }
        @Bean JdbcTemplate jdbc(DataSource source) { return new JdbcTemplate(source); }
        @Bean PlatformTransactionManager transactionManager(DataSource source) {
            return new DataSourceTransactionManager(source);
        }
        @Bean ObjectMapper mapper() { return new ObjectMapper(); }
        @Bean SecurityContextFacade security() { return new SecurityContextFacade(); }
        @Bean TenantTimezoneService timezone() {
            TenantTimezoneService service = mock(TenantTimezoneService.class);
            when(service.requiredZone()).thenReturn(ZoneId.of("America/La_Paz"));
            return service;
        }
        @Bean SemanticCatalogV1 catalog() { return new SemanticCatalogV1(); }
        @Bean QueryPlanValidationService validator(ObjectMapper mapper, SemanticCatalogV1 catalog) {
            return new QueryPlanValidationService(mapper, catalog);
        }
        @Bean AnalyticAuthorizationPolicy authorization(SecurityContextFacade security, SemanticCatalogV1 catalog) {
            return new AnalyticAuthorizationPolicy(security, catalog);
        }
        @Bean QueryPlanSqlCompiler compiler(SemanticCatalogV1 catalog) {
            return new QueryPlanSqlCompiler(catalog);
        }
        @Bean AnalyticQueryExecutor executor(JdbcTemplate jdbc) { return new AnalyticQueryExecutor(jdbc); }
        @Bean AnalyticQueryService queryService(QueryPlanValidationService validator,
                                               AnalyticAuthorizationPolicy authorization,
                                               SecurityContextFacade security,
                                               TenantSchemaNamingStrategy naming,
                                               TenantTimezoneService timezone,
                                               QueryPlanSqlCompiler compiler,
                                               AnalyticQueryExecutor executor) {
            return new AnalyticQueryService(validator, authorization, security, naming, timezone, compiler, executor);
        }
    }
}
