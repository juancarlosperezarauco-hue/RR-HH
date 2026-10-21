package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai;

import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContext;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.common.tenancy.schema.TenantSchemaNamingStrategy;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service.TenantTimezoneService;
import java.time.ZoneId;
import org.springframework.stereotype.Service;

/** The internal entry point. Every execution validates and authorizes the complete plan. */
@Service
public final class AnalyticQueryService {
    private final QueryPlanValidationService validator;
    private final AnalyticAuthorizationPolicy authorization;
    private final SecurityContextFacade security;
    private final TenantSchemaNamingStrategy schemaNaming;
    private final TenantTimezoneService timezone;
    private final QueryPlanSqlCompiler compiler;
    private final AnalyticQueryExecutor executor;

    public AnalyticQueryService(QueryPlanValidationService validator,
                                AnalyticAuthorizationPolicy authorization,
                                SecurityContextFacade security,
                                TenantSchemaNamingStrategy schemaNaming,
                                TenantTimezoneService timezone,
                                QueryPlanSqlCompiler compiler,
                                AnalyticQueryExecutor executor) {
        this.validator = validator;
        this.authorization = authorization;
        this.security = security;
        this.schemaNaming = schemaNaming;
        this.timezone = timezone;
        this.compiler = compiler;
        this.executor = executor;
    }

    public QueryResultV1 execute(String queryPlanJson) {
        var validated = validator.validate(queryPlanJson);
        authorization.authorize(validated, AnalyticAuthorizationPolicy.Action.QUERY);
        checkTenant();
        ZoneId zone = "attendance.daily".equals(validated.plan().dataset())
                ? timezone.requiredZone() : null;
        return executor.execute(compiler.compile(validated, zone));
    }

    public void export(String queryPlanJson, int maxRows, int timeoutSeconds, AnalyticExportSink sink) {
        if (maxRows < 1 || maxRows > 100_000 || timeoutSeconds < 1 || timeoutSeconds > 120)
            throw new IllegalArgumentException("Invalid export bounds");
        var validated = validator.validate(queryPlanJson);
        authorization.authorize(validated, AnalyticAuthorizationPolicy.Action.EXPORT);
        checkTenant();
        ZoneId zone = "attendance.daily".equals(validated.plan().dataset()) ? timezone.requiredZone() : null;
        executor.export(compiler.compileExport(validated, zone, maxRows), timeoutSeconds, sink);
    }

    private void checkTenant() {
        TenantContext tenant = TenantContextHolder.get();
        String expectedSchema;
        try {
            expectedSchema = schemaNaming.toSchemaName(security.getCurrentTenantSlug());
        } catch (RuntimeException invalidTenant) {
            throw new QueryPlanException(QueryPlanException.Code.QUERY_NOT_ALLOWED);
        }
        if (tenant == null || tenant.publicRequest() || !expectedSchema.equals(tenant.schemaName()))
            throw new QueryPlanException(QueryPlanException.Code.QUERY_NOT_ALLOWED);

    }
}
