package com.hrcoresystem.hrcore_api.modules.tenant.attendance.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Runs only against an explicitly supplied disposable local PostgreSQL database. */
@EnabledIfEnvironmentVariable(named = "ATTENDANCE_TEST_JDBC_URL", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AttendancePostgresIntegrationTest {
    private String url, user, password, schemaA, schemaB;
    private UUID employee, requester;

    @BeforeAll
    void setUp() throws SQLException {
        url = System.getenv("ATTENDANCE_TEST_JDBC_URL");
        user = System.getenv("ATTENDANCE_TEST_DB_USER");
        password = System.getenv("ATTENDANCE_TEST_DB_PASSWORD");
        assertTrue(url.matches("jdbc:postgresql://(localhost|127\\.0\\.0\\.1):[0-9]+/hu17_test"),
                "Integration tests require a disposable local hu17_test database");
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        schemaA = "hu17_a_" + suffix;
        schemaB = "hu17_b_" + suffix;
        Flyway.configure().dataSource(url, user, password)
                .locations("classpath:db/migration/public").schemas("public")
                .defaultSchema("public").baselineOnMigrate(true).load().migrate();
        for (String schema : new String[] { schemaA, schemaB })
            Flyway.configure().dataSource(url, user, password)
                    .locations("classpath:db/migration/tenant").schemas(schema)
                    .defaultSchema(schema).baselineOnMigrate(true).createSchemas(true).load().migrate();
        requester = UUID.randomUUID();
        employee = UUID.randomUUID();
        try (Connection c = connection(schemaA)) {
            update(c, "insert into tenant_users(id,email,password_hash) values(?,?,?)",
                    requester, "requester-" + suffix + "@example.test", "test");
            update(c, "insert into employees(id,tenant_user_id,employee_number,first_name,last_name," +
                            "document_type,document_number,hire_date) values(?,?,?,?,?,?,?,?)",
                    employee, requester, "E-" + suffix, "Test", "Employee", "CI", "D-" + suffix,
                    LocalDate.of(2025, 1, 1));
        }
    }

    @AfterAll
    void tearDown() throws SQLException {
        if (schemaA == null || schemaB == null) return;
        try (Connection c = DriverManager.getConnection(url, user, password)) {
            update(c, "drop schema " + schemaA + " cascade");
            update(c, "drop schema " + schemaB + " cascade");
        }
    }

    @Test
    void uniqueChecksAndJsonbUseV15() throws SQLException {
        UUID record = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 1, 12);
        try (Connection c = connection(schemaA)) {
            update(c, "insert into attendance_records(id,employee_id,work_date,calculation_details) " +
                    "values(?,?,?,?::jsonb)", record, employee, date, "{\"reason\":\"TEST\"}");
            assertEquals("TEST", scalar(c, "select calculation_details->>'reason' from attendance_records where id=?", record));
            assertThrows(SQLException.class, () -> update(c,
                    "insert into attendance_records(employee_id,work_date) values(?,?)", employee, date));
            assertThrows(SQLException.class, () -> update(c,
                    "insert into attendance_punches(employee_id,punch_type,punched_at,source) " +
                    "values(?,'UNKNOWN',now(),'WEB')", employee));
            assertThrows(SQLException.class, () -> update(c,
                    "insert into attendance_overtime(attendance_record_id,requested_minutes) values(?,0)", record));
            update(c, "insert into attendance_adjustments(attendance_record_id,adjustment_type,reason," +
                            "previous_values,requested_values,requested_by) " +
                            "values(?,'OTHER','test',?::jsonb,?::jsonb,?)",
                    record, "{\"status\":\"PENDING\"}", "{\"status\":\"JUSTIFIED\"}", requester);
            assertEquals("JUSTIFIED", scalar(c,
                    "select requested_values->>'status' from attendance_adjustments where attendance_record_id=?", record));
        }
    }

    @Test
    void paginationRollbackAuditIdentityAndTenantIsolation() throws SQLException {
        UUID record = UUID.randomUUID(), overtime = UUID.randomUUID();
        try (Connection c = connection(schemaA)) {
            for (int day = 1; day <= 5; day++)
                update(c, "insert into attendance_records(employee_id,work_date) values(?,?)",
                        employee, LocalDate.of(2026, 2, day));
            assertEquals("2026-02-03", scalar(c, "select work_date::text from attendance_records " +
                    "where employee_id=? and work_date between '2026-02-01' and '2026-02-05' " +
                    "order by work_date limit 1 offset 2", employee));
            c.setAutoCommit(false);
            try {
                update(c, "insert into attendance_records(id,employee_id,work_date) values(?,?,?)",
                        record, employee, LocalDate.of(2026, 3, 1));
                update(c, "insert into attendance_punches(employee_id,punch_type,punched_at,source) " +
                        "values(?,'INVALID',now(),'WEB')", employee);
                fail("The invalid punch must fail the transaction");
            } catch (SQLException expected) {
                c.rollback();
            } finally { c.setAutoCommit(true); }
            assertEquals("0", scalar(c, "select count(*)::text from attendance_records where id=?", record));
            update(c, "insert into attendance_records(id,employee_id,work_date) values(?,?,?)",
                    record, employee, LocalDate.of(2026, 3, 1));
            update(c, "insert into attendance_overtime(id,attendance_record_id,requested_minutes) values(?,?,?)",
                    overtime, record, 60);
            assertEquals("0", scalar(c, requesterCountSql(), overtime.toString()));
            for (int i = 1; i <= 2; i++) {
                update(c, "insert into tenant_audit_events(actor_id,event_type,resource_type,resource_id) " +
                        "values(?,'ATTENDANCE_OVERTIME_REQUESTED','ATTENDANCE_OVERTIME',?)",
                        requester, overtime.toString());
                assertEquals(Integer.toString(i), scalar(c, requesterCountSql(), overtime.toString()));
            }
        }
        try (Connection other = connection(schemaB)) {
            assertEquals("0", scalar(other, "select count(*)::text from attendance_records where id=?", record));
            assertThrows(SQLException.class, () -> update(other,
                    "insert into attendance_records(employee_id,work_date) values(?,?)",
                    employee, LocalDate.of(2026, 3, 1)));
            assertEquals("0", scalar(other, requesterCountSql(), overtime.toString()));
        }
    }

    @Test
    void employeeLockSerializesConcurrentRecordCreation() throws Exception {
        LocalDate date = LocalDate.of(2026, 4, 1);
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> createWithLock(date, ready, start));
            var second = executor.submit(() -> createWithLock(date, ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            assertEquals(1, first.get(10, TimeUnit.SECONDS) + second.get(10, TimeUnit.SECONDS));
        }
        try (Connection c = connection(schemaA)) {
            assertEquals("1", scalar(c, "select count(*)::text from attendance_records " +
                    "where employee_id=? and work_date=?", employee, date));
        }
    }

    private int createWithLock(LocalDate date, CountDownLatch ready, CountDownLatch start) throws Exception {
        try (Connection c = connection(schemaA)) {
            c.setAutoCommit(false);
            ready.countDown();
            start.await();
            try {
                scalar(c, "select id::text from employees where id=? for update", employee);
                if ("0".equals(scalar(c, "select count(*)::text from attendance_records " +
                        "where employee_id=? and work_date=?", employee, date))) {
                    update(c, "insert into attendance_records(employee_id,work_date) values(?,?)", employee, date);
                    c.commit();
                    return 1;
                }
                c.commit();
                return 0;
            } catch (Exception ex) { c.rollback(); throw ex; }
        }
    }

    private String requesterCountSql() {
        return "select count(*)::text from tenant_audit_events where event_type=" +
                "'ATTENDANCE_OVERTIME_REQUESTED' and resource_type='ATTENDANCE_OVERTIME' " +
                "and resource_id=? and outcome='SUCCESS'";
    }
    private Connection connection(String schema) throws SQLException {
        Connection c = DriverManager.getConnection(url, user, password);
        c.setSchema(schema);
        return c;
    }
    private void update(Connection c, String sql, Object... values) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) p.setObject(i + 1, values[i]);
            p.executeUpdate();
        }
    }
    private String scalar(Connection c, String sql, Object... values) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) p.setObject(i + 1, values[i]);
            try (ResultSet r = p.executeQuery()) { return r.next() ? r.getString(1) : null; }
        }
    }
}
