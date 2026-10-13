package com.hrcoresystem.hrcore_api.modules.tenant.leaves;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Requires an explicitly supplied, disposable localhost database named hu18_test. */
@EnabledIfEnvironmentVariable(named = "LEAVES_TEST_JDBC_URL", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LeavePostgresIntegrationTest {
    private String url, user, password, schemaA, schemaB;
    private UUID employee, type, balance, requestA, requestB;

    @BeforeAll void setUp() throws SQLException {
        url = System.getenv("LEAVES_TEST_JDBC_URL");
        user = System.getenv("LEAVES_TEST_DB_USER");
        password = System.getenv("LEAVES_TEST_DB_PASSWORD");
        assertTrue(url.matches("jdbc:postgresql://(localhost|127\\.0\\.0\\.1):[0-9]+/hu18_test"),
                "Tests require a disposable localhost hu18_test database");
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        schemaA = "hu18_a_" + suffix; schemaB = "hu18_b_" + suffix;
        Flyway.configure().dataSource(url, user, password).locations("classpath:db/migration/public")
                .schemas("public").defaultSchema("public").baselineOnMigrate(true).load().migrate();
        for (String schema : new String[] {schemaA, schemaB})
            Flyway.configure().dataSource(url, user, password).locations("classpath:db/migration/tenant")
                    .schemas(schema).defaultSchema(schema).baselineOnMigrate(true)
                    .createSchemas(true).load().migrate();
        employee = UUID.randomUUID(); type = UUID.randomUUID(); balance = UUID.randomUUID();
        requestA = UUID.randomUUID(); requestB = UUID.randomUUID(); UUID userId = UUID.randomUUID();
        try (Connection c = connection(schemaA)) {
            update(c, "insert into tenant_users(id,email,password_hash) values(?,?,?)",
                    userId, "hr-" + suffix + "@example.test", "test");
            update(c, "insert into employees(id,tenant_user_id,employee_number,first_name,last_name," +
                    "document_type,document_number,hire_date) values(?,?,?,?,?,?,?,?)",
                    employee, userId, "E-" + suffix, "Test", "Employee", "CI", "D-" + suffix,
                    LocalDate.of(2025, 1, 1));
            update(c, "insert into leave_types(id,code,name,deducts_balance) values(?,?,?,true)",
                    type, "VAC-" + suffix, "Vacation");
            update(c, "insert into leave_balances(id,employee_id,leave_type_id,balance_year,opening_days) " +
                    "values(?,?,?,?,5.00)", balance, employee, type, 2026);
            update(c, "insert into leave_requests(id,employee_id,leave_type_id,start_date,end_date," +
                    "requested_days,reason,status,requested_by) values(?,?,?,?,?,4.00,'Test','PENDING',?)",
                    requestA, employee, type, LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 5), userId);
            update(c, "insert into leave_requests(id,employee_id,leave_type_id,start_date,end_date," +
                    "requested_days,reason,status,requested_by) values(?,?,?,?,?,4.00,'Test','PENDING',?)",
                    requestB, employee, type, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 4), userId);
        }
    }

    @AfterAll void tearDown() throws SQLException {
        if (schemaA == null || schemaB == null) return;
        try (Connection c = DriverManager.getConnection(url, user, password)) {
            update(c, "drop schema if exists " + schemaA + " cascade");
            update(c, "drop schema if exists " + schemaB + " cascade");
        }
    }

    @Test void v16ConstraintsAndTenantSchemasAreEnforced() throws SQLException {
        try (Connection a = connection(schemaA); Connection b = connection(schemaB)) {
            assertEquals(1L, count(a, "select count(*) from leave_requests where id=?", requestA));
            assertEquals(0L, count(b, "select count(*) from leave_requests where id=?", requestA));
            assertThrows(SQLException.class, () -> update(a,
                    "insert into leave_balances(employee_id,leave_type_id,balance_year) values(?,?,?)",
                    employee, type, 2026));
            assertThrows(SQLException.class, () -> update(a,
                    "insert into leave_requests(employee_id,leave_type_id,start_date,end_date," +
                    "start_time,end_time,requested_days,reason) values(?,?,?,?,?,?,1.00,'Bad')",
                    employee, type, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 1),
                    Time.valueOf("15:00:00"), Time.valueOf("14:00:00")));
        }
    }

    @Test void lockedBalancePreventsTwoFourDayApprovalsFromSpendingFiveDays() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> first = pool.submit(() -> { start.await(); return tryApprove(requestA); });
            Future<Boolean> second = pool.submit(() -> { start.await(); return tryApprove(requestB); });
            start.countDown();
            assertNotEquals(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
            try (Connection c = connection(schemaA)) {
                assertEquals(new BigDecimal("4.00"), decimal(c,
                        "select used_days from leave_balances where id=?", balance));
                assertEquals(1L, count(c,
                        "select count(*) from leave_requests where status='APPROVED' and id in (?,?)",
                        requestA, requestB));
            }
        } finally { pool.shutdownNow(); }
    }

    private boolean tryApprove(UUID requestId) throws SQLException {
        try (Connection c = connection(schemaA)) {
            c.setAutoCommit(false);
            try {
                lockEmployee(c);
                BigDecimal available = decimal(c,
                        "select opening_days+accrued_days+adjusted_days-used_days " +
                        "from leave_balances where id=? for update", balance);
                if (available.compareTo(new BigDecimal("4.00")) < 0) { c.rollback(); return false; }
                update(c, "update leave_balances set used_days=used_days+4.00 where id=?", balance);
                update(c, "update leave_requests set status='APPROVED' where id=? and status='PENDING'", requestId);
                c.commit(); return true;
            } catch (SQLException ex) { c.rollback(); throw ex; }
        }
    }

    private Connection connection(String schema) throws SQLException {
        Connection c = DriverManager.getConnection(url, user, password);
        try (Statement s = c.createStatement()) { s.execute("set search_path to " + schema); }
        return c;
    }
    private void lockEmployee(Connection c) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("select id from employees where id=? for update")) {
            p.setObject(1, employee);
            try (ResultSet r = p.executeQuery()) { assertTrue(r.next()); }
        }
    }
    private void update(Connection c, String sql, Object... values) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) p.setObject(i + 1, values[i]);
            p.executeUpdate();
        }
    }
    private long count(Connection c, String sql, Object... values) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) p.setObject(i + 1, values[i]);
            try (ResultSet r = p.executeQuery()) { r.next(); return r.getLong(1); }
        }
    }
    private BigDecimal decimal(Connection c, String sql, Object... values) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) p.setObject(i + 1, values[i]);
            try (ResultSet r = p.executeQuery()) { r.next(); return r.getBigDecimal(1); }
        }
    }
}
