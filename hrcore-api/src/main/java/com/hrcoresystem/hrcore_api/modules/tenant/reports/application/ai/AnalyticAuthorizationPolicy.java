package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai;

import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContext;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

import static com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.QueryPlanException.Code.QUERY_NOT_ALLOWED;

/** Computes the union of required authorities from a fully validated plan. */
@Service
public final class AnalyticAuthorizationPolicy {
    public enum Action { QUERY, EXPORT }

    public record Decision(Set<String> requiredAuthorities) {
        public Decision { requiredAuthorities = Set.copyOf(requiredAuthorities); }
    }

    public record DatasetCapabilities(boolean aggregate, boolean detail,
                                      Set<String> dimensions, Set<String> metrics) {
        public DatasetCapabilities {
            dimensions = Set.copyOf(dimensions);
            metrics = Set.copyOf(metrics);
        }
    }

    private final SecurityContextFacade security;
    private final SemanticCatalogV1 catalog;

    public AnalyticAuthorizationPolicy(SecurityContextFacade security, SemanticCatalogV1 catalog) {
        this.security = security;
        this.catalog = catalog;
    }

    public Decision authorize(QueryPlanValidationService.ValidatedPlan validated, Action action) {
        requireTenantPrincipal();
        if (validated == null || action == null) throw new QueryPlanException(QUERY_NOT_ALLOWED);
        QueryPlanV1 plan = validated.plan();
        SemanticCatalogV1.Dataset dataset = catalog.dataset(plan.dataset());
        if (dataset == null) throw new QueryPlanException(QUERY_NOT_ALLOWED);

        Set<String> required = new HashSet<>();
        required.add(dataset.reportPermission());
        if ("detail".equals(plan.mode())) required.add(dataset.detailPermission());
        if (action == Action.EXPORT) required.add("hr.reports.export");

        for (String id : plan.select()) addDimension(required, dataset, id);
        for (String id : plan.groupBy()) addDimension(required, dataset, id);
        for (QueryPlanV1.Filter filter : plan.filters()) addDimension(required, dataset, filter.field());
        for (String id : plan.metrics()) addMetric(required, dataset, id);
        for (QueryPlanV1.Having having : plan.having()) addMetric(required, dataset, having.metric());
        for (QueryPlanV1.Order order : plan.orderBy()) {
            if (dataset.dimensions().containsKey(order.field())) addDimension(required, dataset, order.field());
            else addMetric(required, dataset, order.field());
        }
        if (plan.visualization().x() != null) addDimension(required, dataset, plan.visualization().x());
        if (plan.visualization().y() != null) addMetric(required, dataset, plan.visualization().y());

        if (!hasAll(required)) throw new QueryPlanException(QUERY_NOT_ALLOWED);
        return new Decision(required);
    }

    /** A safe capability view for a future orchestrator; no data or SQL is exposed. */
    public Map<String, DatasetCapabilities> authorizedCatalog() {
        requireTenantPrincipal();
        Map<String, DatasetCapabilities> result = new LinkedHashMap<>();
        for (SemanticCatalogV1.Dataset dataset : catalog.datasets().values()) {
            if (!security.hasAuthority(dataset.reportPermission())) continue;
            Set<String> dimensions = new HashSet<>();
            Set<String> metrics = new HashSet<>();
            for (SemanticCatalogV1.Dimension dimension : dataset.dimensions().values()) {
                if (!dimension.enabled()) continue;
                Set<String> required = new HashSet<>();
                addDimension(required, dataset, dimension.id());
                if (hasAll(required)) dimensions.add(dimension.id());
            }
            for (SemanticCatalogV1.Metric metric : dataset.metrics().values()) {
                if (hasAll(metric.permissions())) metrics.add(metric.id());
            }
            result.put(dataset.id(), new DatasetCapabilities(true,
                    security.hasAuthority(dataset.detailPermission()), dimensions, metrics));
        }
        return Map.copyOf(result);
    }

    private void addDimension(Set<String> required, SemanticCatalogV1.Dataset dataset, String id) {
        SemanticCatalogV1.Dimension dimension = dataset.dimensions().get(id);
        if (dimension == null || !dimension.enabled()) throw new QueryPlanException(QUERY_NOT_ALLOWED);
        required.addAll(dimension.permissions());
        if (dimension.nominal() && "attendance.daily".equals(dataset.id()))
            required.add("attendance.records.list");
    }

    private void addMetric(Set<String> required, SemanticCatalogV1.Dataset dataset, String id) {
        SemanticCatalogV1.Metric metric = dataset.metrics().get(id);
        if (metric == null) throw new QueryPlanException(QUERY_NOT_ALLOWED);
        required.addAll(metric.permissions());
    }

    private boolean hasAll(Set<String> required) {
        return required.stream().allMatch(security::hasAuthority);
    }

    private void requireTenantPrincipal() {
        TenantContext context = TenantContextHolder.get();
        if (!security.isTenantAuthenticated() || context == null || context.publicRequest()
                || !context.tenantSlug().equals(security.getCurrentTenantSlug()))
            throw new QueryPlanException(QUERY_NOT_ALLOWED);
    }
}
