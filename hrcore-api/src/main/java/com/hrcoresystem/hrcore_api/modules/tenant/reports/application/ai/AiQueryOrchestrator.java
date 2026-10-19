package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai;

import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;

/** Coordinates one request synchronously so Spring's security and tenant context remain intact. */
@Service
public final class AiQueryOrchestrator {
    private final AnalyticAuthorizationPolicy authorization;
    private final AiPlannerClient planner;
    private final AnalyticQueryService queries;

    public AiQueryOrchestrator(AnalyticAuthorizationPolicy authorization, AiPlannerClient planner,
                               AnalyticQueryService queries) {
        this.authorization = authorization;
        this.planner = planner;
        this.queries = queries;
    }

    public AiQueryResponse query(String question) {
        Map<String, AnalyticAuthorizationPolicy.DatasetCapabilities> capabilities =
                new TreeMap<>(authorization.authorizedCatalog());
        if (capabilities.isEmpty())
            throw new QueryPlanException(QueryPlanException.Code.QUERY_NOT_ALLOWED);

        AiPlannerClient.Reply reply = planner.plan(new AiPlannerClient.Request(
                "1", question, SemanticCatalogV1.VERSION, capabilities));
        return switch (reply.status()) {
            case "query_plan" -> AiQueryResponse.ok(queries.execute(reply.plan().toString()));
            case "needs_clarification" -> AiQueryResponse.clarification(
                    "¿Qué rango de fechas necesitas?", reply.reasonCode());
            case "unsupported" -> AiQueryResponse.unsupported(safeUnsupported(reply.reasonCode()),
                    reply.reasonCode());
            default -> throw new AiFlowException(AiFlowException.Code.INVALID_PLANNER_RESPONSE);
        };
    }

    private static String safeUnsupported(String reasonCode) {
        return switch (reasonCode) {
            case "DATASET_NOT_AVAILABLE" -> "La consulta no está disponible en el catálogo actual.";
            case "CAPABILITY_NOT_AVAILABLE" -> "La consulta no está disponible con las capacidades actuales.";
            case "UNRECOGNIZED_REQUEST" -> "La consulta no está disponible en esta demostración.";
            default -> throw new AiFlowException(AiFlowException.Code.INVALID_PLANNER_RESPONSE);
        };
    }
}
