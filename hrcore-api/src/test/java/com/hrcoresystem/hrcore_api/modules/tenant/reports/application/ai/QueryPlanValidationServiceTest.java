package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QueryPlanValidationServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final QueryPlanValidationService validator =
            new QueryPlanValidationService(mapper, new SemanticCatalogV1());

    static final String ROSTER = """
            {"version":"1","catalogVersion":"1","dataset":"employees.roster","mode":"aggregate",
             "asOfDate":"2026-10-18","select":["department.name"],"metrics":["employees.distinct_count"],
             "filters":[],"groupBy":["department.id"],"having":[],
             "orderBy":[{"field":"employees.distinct_count","direction":"DESC"}],
             "page":{"number":0,"size":100},
             "visualization":{"type":"bar","x":"department.name","y":"employees.distinct_count"}}
            """;
    static final String ATTENDANCE_AGGREGATE = """
            {"version":"1","catalogVersion":"1","dataset":"attendance.daily","mode":"aggregate",
             "select":["employee.full_name","department.name","position.name"],
             "metrics":["attendance.late_days","attendance.late_minutes_total"],
             "dateRange":{"field":"attendance.work_date","from":"2026-09-01","toExclusive":"2026-10-01"},
             "filters":[{"field":"attendance.status","op":"eq","value":"LATE"}],
             "groupBy":["employee.id","department.id","position.id"],
             "having":[{"metric":"attendance.late_days","op":"gt","value":3}],
             "orderBy":[{"field":"attendance.late_days","direction":"DESC"}],
             "page":{"number":0,"size":100},"visualization":{"type":"table"}}
            """;
    static final String ATTENDANCE_DETAIL = """
            {"version":"1","catalogVersion":"1","dataset":"attendance.daily","mode":"detail",
             "select":["employee.full_name","department.name","position.name","attendance.work_date","attendance.first_in_local_time"],
             "metrics":[],"dateRange":{"field":"attendance.work_date","from":"2026-10-01","toExclusive":"2026-10-16"},
             "filters":[{"field":"attendance.first_in_local_time","op":"between","value":["08:00:00","09:00:00"]}],
             "groupBy":[],"having":[],"orderBy":[{"field":"attendance.work_date","direction":"ASC"}],
             "page":{"number":0,"size":100},"visualization":{"type":"table"}}
            """;

    @Test
    void acceptsThreeSpecificationExamples() {
        assertEquals("employees.roster", validator.validate(ROSTER).plan().dataset());
        assertEquals("attendance.daily", validator.validate(ATTENDANCE_AGGREGATE).plan().dataset());
        assertEquals("detail", validator.validate(ATTENDANCE_DETAIL).plan().mode());
    }

    @Test
    void catalogDeclaresTemporalGrainAndDisablesUnreliableHistoricalStatus() {
        SemanticCatalogV1 catalog = new SemanticCatalogV1();
        assertEquals("1", SemanticCatalogV1.VERSION);
        assertEquals("asOfDate", catalog.dataset("employees.roster").temporalRelations().get(0).effectiveAt());
        assertEquals("work_date", catalog.dataset("attendance.daily").temporalRelations().get(1).effectiveAt());
        assertFalse(catalog.dataset("employees.roster").dimensions()
                .get("employee.employment_status_at_date").enabled());
        assertThrows(UnsupportedOperationException.class,
                () -> catalog.datasets().clear());
    }

    @Test
    void rejectsUnknownAndDuplicateJsonPropertiesAtEveryLevel() throws Exception {
        ObjectNode root = (ObjectNode) mapper.readTree(ROSTER);
        root.put("tenantId", "other");
        assertCode(QueryPlanException.Code.INVALID_QUERY_PLAN, root.toString());
        root.remove("tenantId");
        ((ObjectNode) root.get("page")).put("sql", "select 1");
        assertCode(QueryPlanException.Code.INVALID_QUERY_PLAN, root.toString());
        assertCode(QueryPlanException.Code.INVALID_QUERY_PLAN,
                ROSTER.replace("\"version\":\"1\"", "\"version\":\"1\",\"version\":\"1\""));
        assertCode(QueryPlanException.Code.INVALID_QUERY_PLAN, ROSTER + " {} ");
    }

    @Test
    void validatesModeGroupingMetricsAndVisualization() throws Exception {
        ObjectNode root = (ObjectNode) mapper.readTree(ROSTER);
        root.put("mode", "detail");
        assertCode(QueryPlanException.Code.INVALID_QUERY_PLAN, root.toString());
        root = (ObjectNode) mapper.readTree(ROSTER);
        root.withArray("groupBy").removeAll();
        assertCode(QueryPlanException.Code.UNSUPPORTED_QUERY, root.toString());
        root = (ObjectNode) mapper.readTree(ROSTER);
        root.withArray("select").removeAll();
        ((ObjectNode) root.get("visualization")).put("x", "employee.full_name");
        assertCode(QueryPlanException.Code.INVALID_QUERY_PLAN, root.toString());
        root = (ObjectNode) mapper.readTree(ATTENDANCE_AGGREGATE);
        root.withArray("metrics").removeAll();
        assertCode(QueryPlanException.Code.INVALID_QUERY_PLAN, root.toString());
    }

    @Test
    void rejectsDatesLimitsAndTypes() throws Exception {
        ObjectNode root = (ObjectNode) mapper.readTree(ATTENDANCE_DETAIL);
        ((ObjectNode) root.get("dateRange")).put("toExclusive", "2026-10-01");
        assertCode(QueryPlanException.Code.INVALID_QUERY_PLAN, root.toString());
        ((ObjectNode) root.get("dateRange")).put("toExclusive", "2028-01-01");
        assertCode(QueryPlanException.Code.QUERY_TOO_COMPLEX, root.toString());
        ((ObjectNode) root.get("dateRange")).put("from", "2026-02-30");
        assertCode(QueryPlanException.Code.INVALID_QUERY_PLAN, root.toString());
        root = (ObjectNode) mapper.readTree(ROSTER);
        ((ObjectNode) root.get("page")).put("size", 101);
        assertCode(QueryPlanException.Code.QUERY_TOO_COMPLEX, root.toString());
        ((ObjectNode) root.get("page")).put("size", "10");
        assertCode(QueryPlanException.Code.INVALID_QUERY_PLAN, root.toString());
        root.put("catalogVersion", "2");
        assertCode(QueryPlanException.Code.CATALOG_VERSION_MISMATCH, root.toString());
    }

    @Test
    void rejectsWrongDatasetSensitiveAndUnreliableHistoricalField() throws Exception {
        ObjectNode root = (ObjectNode) mapper.readTree(ROSTER);
        root.withArray("select").add("attendance.status");
        assertCode(QueryPlanException.Code.UNSUPPORTED_QUERY, root.toString());
        root = (ObjectNode) mapper.readTree(ROSTER);
        root.withArray("select").add("employee.salary");
        assertCode(QueryPlanException.Code.INVALID_QUERY_PLAN, root.toString());
        root = (ObjectNode) mapper.readTree(ROSTER);
        root.withArray("select").add("employee.employment_status_at_date");
        assertCode(QueryPlanException.Code.UNSUPPORTED_QUERY, root.toString());
    }

    @Test
    void rejectsUnsupportedOperatorsInvalidValuesAndNightCrossing() throws Exception {
        ObjectNode root = (ObjectNode) mapper.readTree(ATTENDANCE_DETAIL);
        ((ObjectNode) root.withArray("filters").get(0)).put("op", "is_null");
        assertCode(QueryPlanException.Code.INVALID_QUERY_PLAN, root.toString());
        root = (ObjectNode) mapper.readTree(ATTENDANCE_DETAIL);
        ((ObjectNode) root.withArray("filters").get(0)).withArray("value").set(0, mapper.getNodeFactory().textNode("23:00:00"));
        assertCode(QueryPlanException.Code.INVALID_QUERY_PLAN, root.toString());
        root = (ObjectNode) mapper.readTree(ATTENDANCE_AGGREGATE);
        ((ObjectNode) root.withArray("filters").get(0)).put("value", "UNKNOWN");
        assertCode(QueryPlanException.Code.INVALID_QUERY_PLAN, root.toString());
        root = (ObjectNode) mapper.readTree(ROSTER);
        root.withArray("filters").addObject().put("field", "department.id").put("op", "gt")
                .put("value", "00000000-0000-0000-0000-000000000001");
        assertCode(QueryPlanException.Code.UNSUPPORTED_QUERY, root.toString());
    }

    private void assertCode(QueryPlanException.Code code, String json) {
        assertEquals(code, assertThrows(QueryPlanException.class, () -> validator.validate(json)).code());
    }
}
