package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service.TenantTimezoneService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Coordinates one request synchronously so Spring's security and tenant context remain intact. */
@Service
public final class AiQueryOrchestrator {
    public record PlannedQuery(AiQueryResponse response, JsonNode validatedPlan) {}

    private final AnalyticAuthorizationPolicy authorization;
    private final AiPlannerClient planner;
    private final AnalyticQueryService queries;
    private final SemanticCatalogV1 catalog;
    private final TenantTimezoneService timezone;

    @Autowired
    public AiQueryOrchestrator(AnalyticAuthorizationPolicy authorization, AiPlannerClient planner,
                               AnalyticQueryService queries, SemanticCatalogV1 catalog,
                               TenantTimezoneService timezone) {
        this.authorization = authorization;
        this.planner = planner;
        this.queries = queries;
        this.catalog = catalog;
        this.timezone = timezone;
    }

    public AiQueryOrchestrator(AnalyticAuthorizationPolicy authorization, AiPlannerClient planner,
                               AnalyticQueryService queries) {
        this(authorization, planner, queries, new SemanticCatalogV1(), null);
    }

    public AiQueryResponse query(String question) {
        return queryWithPlan(question).response();
    }

    /** Returns a plan only after the existing query service has validated, authorized and executed it. */
    public PlannedQuery queryWithPlan(String question) {
        Map<String, AnalyticAuthorizationPolicy.DatasetCapabilities> capabilities =
                new TreeMap<>(authorization.authorizedCatalog());
        if (capabilities.isEmpty())
            throw new QueryPlanException(QueryPlanException.Code.QUERY_NOT_ALLOWED);

        AiPlannerClient.Reply reply = planner.plan(new AiPlannerClient.Request(
                "1", question, SemanticCatalogV1.VERSION, capabilities, semanticContext(capabilities)));
        return switch (reply.status()) {
            case "query_plan" -> {
                JsonNode plan = reply.plan().deepCopy();
                yield new PlannedQuery(AiQueryResponse.ok(queries.execute(plan.toString())), plan);
            }
            case "needs_clarification" -> new PlannedQuery(AiQueryResponse.clarification(
                    reply.question() == null ? "¿Qué información adicional necesitas proporcionar?"
                            : reply.question(), reply.reasonCode()), null);
            case "unsupported" -> new PlannedQuery(AiQueryResponse.unsupported(safeUnsupported(reply.reasonCode()),
                    reply.reasonCode()), null);
            default -> throw new AiFlowException(AiFlowException.Code.INVALID_PLANNER_RESPONSE);
        };
    }

    private Map<String, Object> semanticContext(
            Map<String, AnalyticAuthorizationPolicy.DatasetCapabilities> capabilities) {
        Map<String, Object> datasets = new TreeMap<>();
        capabilities.forEach((id, allowed) -> {
            SemanticCatalogV1.Dataset dataset = catalog.dataset(id);
            if (dataset == null) return;
            Map<String, Object> dimensions = new TreeMap<>();
            allowed.dimensions().stream().sorted().forEach(dimensionId -> {
                SemanticCatalogV1.Dimension dimension = dataset.dimensions().get(dimensionId);
                if (dimension == null || !dimension.enabled()) return;
                dimensions.put(dimensionId, Map.of(
                        "type", dimension.type().name(), "operators", dimension.operators(),
                        "nullable", dimension.nullable(), "groupKey", dimension.groupKey(),
                        "labelFor", dimension.labelFor() == null ? "" : dimension.labelFor()));
            });
            Map<String, Object> metrics = new TreeMap<>();
            allowed.metrics().stream().sorted().forEach(metricId -> {
                SemanticCatalogV1.Metric metric = dataset.metrics().get(metricId);
                if (metric != null) metrics.put(metricId, Map.of("grain", metric.grain()));
            });
            List<Map<String, String>> relations = dataset.temporalRelations().stream()
                    .map(relation -> Map.of("path", relation.path(), "effectiveAt", relation.effectiveAt(),
                            "cardinality", relation.cardinality(), "rule", relation.rule())).toList();
            datasets.put(id, Map.of("grain", dataset.grain(), "dimensions", dimensions,
                    "metrics", metrics, "temporalRelations", relations));
        });
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("datasets", datasets);
        context.put("limits", Map.of("maxPageSize", 100, "maxDateRangeDays", 366,
                "maxSelect", 12, "maxMetrics", 8, "maxFilters", 12,
                "maxGroupBy", 8, "maxHaving", 6, "maxOrderBy", 4));
        if (timezone != null) {
            try {
                ZoneId zone = timezone.requiredZone();
                context.put("businessDate", LocalDate.now(zone).toString());
                context.put("businessTimezone", zone.getId());
            } catch (BusinessException missingConfiguration) {
                // An unknown business zone must never be replaced with the server zone.
            }
        }
        return context;
    }

    private static String safeUnsupported(String reasonCode) {
        return switch (reasonCode) {
            case "DATASET_NOT_AVAILABLE" -> "La consulta no está disponible en el catálogo actual.";
            case "CAPABILITY_NOT_AVAILABLE" -> "La consulta no está disponible con las capacidades actuales.";
            case "UNRECOGNIZED_REQUEST" -> "La consulta no está disponible en esta demostración.";
            case "PERSONAL_IDENTIFIER_NOT_ALLOWED" -> "No se admiten identificadores personales en preguntas.";
            default -> throw new AiFlowException(AiFlowException.Code.INVALID_PLANNER_RESPONSE);
        };
    }
}
