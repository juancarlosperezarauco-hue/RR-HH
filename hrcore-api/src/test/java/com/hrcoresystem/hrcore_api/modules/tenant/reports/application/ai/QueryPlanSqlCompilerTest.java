package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QueryPlanSqlCompilerTest {
    private final SemanticCatalogV1 catalog = new SemanticCatalogV1();
    private final QueryPlanValidationService validator = new QueryPlanValidationService(new ObjectMapper(), catalog);
    private final QueryPlanSqlCompiler compiler = new QueryPlanSqlCompiler(catalog);

    @Test
    void everyEnabledCatalogIdentifierHasAServerOwnedSqlBinding() {
        for (var dataset : catalog.datasets().values()) {
            Set<String> enabled = dataset.dimensions().values().stream()
                    .filter(SemanticCatalogV1.Dimension::enabled)
                    .map(SemanticCatalogV1.Dimension::id)
                    .collect(Collectors.toSet());
            assertEquals(enabled, compiler.supportedDimensions(dataset.id()).keySet());
            assertEquals(dataset.metrics().keySet(), compiler.supportedMetrics(dataset.id()).keySet());
            String sample = "attendance.daily".equals(dataset.id())
                    ? QueryPlanValidationServiceTest.ATTENDANCE_AGGREGATE.replace(
                            "\"attendance.late_days\",\"attendance.late_minutes_total\"",
                            "\"attendance.record_count\",\"attendance.late_days\",\"attendance.late_minutes_total\"")
                    : QueryPlanValidationServiceTest.ROSTER;
            String sql = compiler.compile(validator.validate(sample),
                    ZoneId.of("America/La_Paz")).sql();
            for (String alias : compiler.supportedDimensions(dataset.id()).values())
                assertTrue(sql.contains(" AS " + alias), alias);
            for (String expression : compiler.supportedMetrics(dataset.id()).values())
                assertTrue(sql.contains(expression), expression);
        }
    }

    @Test
    void attendanceDetailBindsDatesHoursAndTimezoneAndKeepsOneRecordGrain() {
        var query = compiler.compile(validator.validate(QueryPlanValidationServiceTest.ATTENDANCE_DETAIL),
                ZoneId.of("America/La_Paz"));

        assertTrue(query.sql().contains("ar.employment_period_id"));
        assertTrue(query.sql().contains("ea.employment_period_id = ar.employment_period_id"));
        assertTrue(query.sql().contains("ea.start_date <= ar.work_date"));
        assertTrue(query.sql().contains("timezone(CAST(? AS text), ar.actual_first_in_at)::time"));
        assertTrue(query.sql().contains("LIMIT ? OFFSET ?"));
        assertFalse(query.sql().contains("America/La_Paz"));
        assertEquals("America/La_Paz", query.parameters().get(0));
        assertEquals(LocalDate.of(2026, 10, 1), query.parameters().get(1));
        assertEquals(LocalTime.of(8, 0), query.parameters().get(3));
        assertEquals(101, query.parameters().get(query.parameters().size() - 2));
        assertEquals(0, query.parameters().getLast());
    }

    @Test
    void aggregateUsesPublishedMetricsAndFunctionalLabels() {
        var attendance = compiler.compile(validator.validate(QueryPlanValidationServiceTest.ATTENDANCE_AGGREGATE),
                ZoneId.of("America/La_Paz"));
        assertTrue(attendance.sql().contains("COUNT(DISTINCT CASE WHEN base.attendance_status = 'LATE'"));
        assertTrue(attendance.sql().contains("SUM(CASE WHEN base.attendance_status = 'LATE'"));
        assertTrue(attendance.sql().contains("MIN(base.department_name)"));
        assertTrue(attendance.sql().contains("GROUP BY base.employee_id, base.department_id, base.position_id"));
        assertTrue(attendance.sql().contains(" HAVING "));

        var roster = compiler.compile(validator.validate(QueryPlanValidationServiceTest.ROSTER), null);
        assertTrue(roster.sql().contains("COUNT(DISTINCT base.employee_id)"));
        assertTrue(roster.sql().contains("ea.employment_period_id = ep.id"));
        assertEquals(4, roster.parameters().stream().filter(LocalDate.class::isInstance).count());
    }

    @Test
    void filterTextNeverBecomesSql() {
        String attack = "fake' OR 1=1 --";
        String plan = QueryPlanValidationServiceTest.ATTENDANCE_DETAIL.replace(
                "\"field\":\"attendance.first_in_local_time\",\"op\":\"between\",\"value\":[\"08:00:00\",\"09:00:00\"]",
                "\"field\":\"employee.full_name\",\"op\":\"eq\",\"value\":\"" + attack + "\"");
        var query = compiler.compile(validator.validate(plan), ZoneId.of("America/La_Paz"));
        assertFalse(query.sql().contains(attack));
        assertTrue(query.parameters().contains(attack));
    }
}
