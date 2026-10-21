package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.common.security.principal.AuthenticatedUserPrincipal;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContext;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;

class AnalyticAuthorizationPolicyTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final SemanticCatalogV1 catalog = new SemanticCatalogV1();
    private final QueryPlanValidationService validator = new QueryPlanValidationService(mapper, catalog);
    private final AnalyticAuthorizationPolicy policy =
            new AnalyticAuthorizationPolicy(new SecurityContextFacade(), catalog);

    private static final String ANONYMOUS_AGGREGATE = """
            {"version":"1","catalogVersion":"1","dataset":"attendance.daily","mode":"aggregate",
             "select":[],"metrics":["attendance.record_count"],
             "dateRange":{"field":"attendance.work_date","from":"2026-09-01","toExclusive":"2026-10-01"},
             "filters":[],"groupBy":[],"having":[],"orderBy":[],
             "page":{"number":0,"size":100},"visualization":{"type":"table"}}
            """;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    @Test
    void specificationExamplesAreAuthorizedWithTheUnionOfRealPermissions() {
        authenticate("hr.reports.employees.read", "employees.list", "departments.list",
                "positions.list", "hr.reports.attendance.read", "attendance.records.list");
        for (String json : List.of(QueryPlanValidationServiceTest.ROSTER,
                QueryPlanValidationServiceTest.ATTENDANCE_AGGREGATE,
                QueryPlanValidationServiceTest.ATTENDANCE_DETAIL)) {
            var validated = validator.validate(json);
            assertDoesNotThrow(() -> policy.authorize(validated, AnalyticAuthorizationPolicy.Action.QUERY));
        }
    }

    @Test
    void aggregateNeedsReportPermissionButDetailAlsoNeedsRecordsList() throws Exception {
        authenticate("hr.reports.attendance.read");
        var aggregate = validator.validate(ANONYMOUS_AGGREGATE);
        assertDoesNotThrow(() -> policy.authorize(aggregate, AnalyticAuthorizationPolicy.Action.QUERY));

        ObjectNode detail = root();
        detail.put("mode", "detail");
        detail.withArray("select").add("attendance.work_date");
        detail.withArray("metrics").removeAll();
        var validated = validator.validate(detail.toString());
        assertDenied(validated);
        authenticate("hr.reports.attendance.read", "attendance.records.list");
        assertDoesNotThrow(() -> policy.authorize(validated, AnalyticAuthorizationPolicy.Action.QUERY));
    }

    @Test
    void employeeIdentityInFilterAndHiddenGroupRequiresAllNominalAuthorities() throws Exception {
        authenticate("hr.reports.attendance.read", "attendance.records.list");
        ObjectNode filtered = root();
        filtered.withArray("filters").addObject().put("field", "employee.full_name")
                .put("op", "eq").put("value", "Persona Ficticia");
        assertDenied(validator.validate(filtered.toString()));

        ObjectNode grouped = root();
        grouped.withArray("groupBy").add("employee.id");
        var hiddenGroup = validator.validate(grouped.toString());
        assertDenied(hiddenGroup);

        authenticate("hr.reports.attendance.read", "attendance.records.list",
                "hr.reports.employees.read");
        assertDenied(hiddenGroup);
        authenticate("hr.reports.attendance.read", "attendance.records.list",
                "employees.list");
        assertDenied(hiddenGroup);

        authenticate("hr.reports.attendance.read", "attendance.records.list",
                "hr.reports.employees.read", "employees.list");
        var decision = policy.authorize(hiddenGroup, AnalyticAuthorizationPolicy.Action.QUERY);
        assertTrue(decision.requiredAuthorities().containsAll(List.of("hr.reports.attendance.read",
                "attendance.records.list", "hr.reports.employees.read", "employees.list")));
        assertDoesNotThrow(() -> policy.authorize(validator.validate(filtered.toString()),
                AnalyticAuthorizationPolicy.Action.QUERY));
    }

    @Test
    void unselectedIdentityUsedOnlyForOrderingStillNeedsNominalRights() throws Exception {
        ObjectNode detail = root();
        detail.put("mode", "detail");
        detail.withArray("select").add("attendance.work_date");
        detail.withArray("metrics").removeAll();
        detail.withArray("orderBy").addObject().put("field", "employee.full_name").put("direction", "ASC");
        var validated = validator.validate(detail.toString());
        authenticate("hr.reports.attendance.read", "attendance.records.list");
        assertDenied(validated);
        authenticate("hr.reports.attendance.read", "attendance.records.list",
                "hr.reports.employees.read", "employees.list");
        assertDoesNotThrow(() -> policy.authorize(validated, AnalyticAuthorizationPolicy.Action.QUERY));
    }

    @Test
    void departmentPositionOrderingAndChartAreAuthorizedIndependently() throws Exception {
        ObjectNode chart = root();
        chart.withArray("select").add("department.name");
        chart.withArray("groupBy").add("department.id");
        ObjectNode visualization = (ObjectNode) chart.get("visualization");
        visualization.put("type", "bar").put("x", "department.name").put("y", "attendance.record_count");
        var bar = validator.validate(chart.toString());

        authenticate("hr.reports.attendance.read");
        assertDenied(bar);
        authenticate("hr.reports.attendance.read", "departments.list");
        assertDoesNotThrow(() -> policy.authorize(bar, AnalyticAuthorizationPolicy.Action.QUERY));

        chart.withArray("orderBy").addObject().put("field", "position.name").put("direction", "ASC");
        chart.withArray("groupBy").add("position.id");
        var ordered = validator.validate(chart.toString());
        assertDenied(ordered);
        authenticate("hr.reports.attendance.read", "departments.list", "positions.list");
        assertDoesNotThrow(() -> policy.authorize(ordered, AnalyticAuthorizationPolicy.Action.QUERY));
    }

    @Test
    void exportRequiresExportAndReadAndRevocationIsRechecked() {
        var validated = validator.validate(ANONYMOUS_AGGREGATE);
        authenticate("hr.reports.export");
        assertDenied(validated, AnalyticAuthorizationPolicy.Action.EXPORT);
        authenticate("hr.reports.attendance.read");
        assertDenied(validated, AnalyticAuthorizationPolicy.Action.EXPORT);
        authenticate("hr.reports.attendance.read", "hr.reports.export");
        assertDoesNotThrow(() -> policy.authorize(validated, AnalyticAuthorizationPolicy.Action.EXPORT));
        authenticate("hr.reports.export");
        assertDenied(validated, AnalyticAuthorizationPolicy.Action.EXPORT);
    }

    @Test
    void teamOnlyAndWrongTenantCannotAuthorizeOrReadCatalog() {
        var validated = validator.validate(ANONYMOUS_AGGREGATE);
        authenticate("employees.team.read", "attendance.team.read");
        assertDenied(validated);
        assertTrue(policy.authorizedCatalog().isEmpty());
        authenticate("hr.reports.attendance.read");
        assertFalse(policy.authorizedCatalog().get("attendance.daily").dimensions().contains("employee.full_name"));
        TenantContextHolder.set(new TenantContext("other-tenant", "other_schema", false));
        assertDenied(validated);
        assertEquals(QueryPlanException.Code.QUERY_NOT_ALLOWED,
                assertThrows(QueryPlanException.class, policy::authorizedCatalog).code());
    }

    private ObjectNode root() throws Exception { return (ObjectNode) mapper.readTree(ANONYMOUS_AGGREGATE); }

    private void authenticate(String... authorities) {
        TenantContextHolder.set(new TenantContext("tenant-a", "tenant_a", false));
        var principal = new AuthenticatedUserPrincipal(UUID.randomUUID(), UUID.randomUUID(),
                "fictional@example.invalid", "Fictional", "tenant-a", "TENANT",
                List.of(), Arrays.asList(authorities), 1, 1L, 1);
        var granted = Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, granted));
    }

    private void assertDenied(QueryPlanValidationService.ValidatedPlan plan) {
        assertDenied(plan, AnalyticAuthorizationPolicy.Action.QUERY);
    }

    private void assertDenied(QueryPlanValidationService.ValidatedPlan plan,
                              AnalyticAuthorizationPolicy.Action action) {
        assertEquals(QueryPlanException.Code.QUERY_NOT_ALLOWED,
                assertThrows(QueryPlanException.class, () -> policy.authorize(plan, action)).code());
    }
}
