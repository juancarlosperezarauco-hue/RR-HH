package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.common.security.principal.AuthenticatedUserPrincipal;
import com.hrcoresystem.hrcore_api.common.tenancy.TenancyProperties;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContext;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.common.tenancy.schema.TenantSchemaNamingStrategy;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service.TenantTimezoneService;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AnalyticQueryServiceTest {
    private final SemanticCatalogV1 catalog = new SemanticCatalogV1();
    private final SecurityContextFacade security = new SecurityContextFacade();
    private final TenantTimezoneService timezone = mock(TenantTimezoneService.class);
    private final AnalyticQueryExecutor executor = mock(AnalyticQueryExecutor.class);
    private final AnalyticQueryService service = new AnalyticQueryService(
            new QueryPlanValidationService(new ObjectMapper(), catalog),
            new AnalyticAuthorizationPolicy(security, catalog), security,
            new TenantSchemaNamingStrategy(new TenancyProperties()), timezone,
            new QueryPlanSqlCompiler(catalog), executor);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    @Test
    void rechecksEffectiveAuthoritiesBeforeEveryExecution() {
        authenticate("hr.reports.attendance.read", "attendance.records.list",
                "hr.reports.employees.read", "employees.list", "departments.list", "positions.list");
        when(timezone.requiredZone()).thenReturn(ZoneId.of("America/La_Paz"));
        var expected = new QueryResultV1(List.of(), List.of(), 0, 100, false, List.of());
        when(executor.execute(any())).thenReturn(expected);

        assertSame(expected, service.execute(QueryPlanValidationServiceTest.ATTENDANCE_DETAIL));
        verify(executor).execute(any());

        authenticate("hr.reports.attendance.read", "attendance.records.list", "employees.list");
        assertEquals(QueryPlanException.Code.QUERY_NOT_ALLOWED,
                assertThrows(QueryPlanException.class,
                        () -> service.execute(QueryPlanValidationServiceTest.ATTENDANCE_DETAIL)).code());
        verifyNoMoreInteractions(executor);
    }

    @Test
    void refusesSchemaMismatchAndUnpublishedPlanBeforeDatabaseAccess() {
        authenticate("hr.reports.attendance.read", "attendance.records.list");
        TenantContextHolder.set(new TenantContext("tenant-a", "tenant_b", false));
        String simple = QueryPlanValidationServiceTest.ATTENDANCE_DETAIL
                .replace("\"employee.full_name\",\"department.name\",\"position.name\",", "")
                .replace("\"field\":\"attendance.first_in_local_time\",\"op\":\"between\",\"value\":[\"08:00:00\",\"09:00:00\"]",
                        "\"field\":\"attendance.status\",\"op\":\"eq\",\"value\":\"LATE\"");
        assertEquals(QueryPlanException.Code.QUERY_NOT_ALLOWED,
                assertThrows(QueryPlanException.class, () -> service.execute(simple)).code());
        assertEquals(QueryPlanException.Code.INVALID_QUERY_PLAN,
                assertThrows(QueryPlanException.class, () -> service.execute(simple.replace(
                        "\"dataset\":\"attendance.daily\"", "\"dataset\":\"payroll.private\""))).code());
        verifyNoInteractions(executor, timezone);
    }

    private void authenticate(String... authorities) {
        TenantContextHolder.set(new TenantContext("tenant-a", "tenant_tenant_a", false));
        var principal = new AuthenticatedUserPrincipal(UUID.randomUUID(), UUID.randomUUID(),
                "fictitious@example.invalid", "Fictitious", "tenant-a", "TENANT",
                List.of(), Arrays.asList(authorities), 1, 1L, 1);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }

    @Test void exportsRevalidateCurrentPermissionsAndCannotBypassTenantChecks() {
        authenticate("hr.reports.attendance.read", "attendance.records.list", "hr.reports.employees.read",
                "employees.list", "departments.list", "positions.list");
        var sink = mock(AnalyticExportSink.class);
        assertEquals(QueryPlanException.Code.QUERY_NOT_ALLOWED, assertThrows(QueryPlanException.class,
                () -> service.export(QueryPlanValidationServiceTest.ATTENDANCE_DETAIL, 10000, 30, sink)).code());
        verifyNoInteractions(executor);
        authenticate("hr.reports.export", "hr.reports.attendance.read", "attendance.records.list",
                "hr.reports.employees.read", "employees.list", "departments.list", "positions.list");
        when(timezone.requiredZone()).thenReturn(ZoneId.of("America/La_Paz"));
        service.export(QueryPlanValidationServiceTest.ATTENDANCE_DETAIL, 10000, 30, sink);
        verify(executor).export(argThat(q -> q.pageNumber() == 0 && q.pageSize() == 10000
                && q.parameters().get(q.parameters().size() - 2).equals(10001)), eq(30), same(sink));
        authenticate("hr.reports.export", "hr.reports.attendance.read", "attendance.records.list");
        assertThrows(QueryPlanException.class,
                () -> service.export(QueryPlanValidationServiceTest.ATTENDANCE_DETAIL, 10000, 30, sink));
        verifyNoMoreInteractions(executor);
    }
}
