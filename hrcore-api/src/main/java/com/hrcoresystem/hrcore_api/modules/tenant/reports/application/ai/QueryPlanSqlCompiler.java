package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Compiles only the two server-owned dataset shapes; plan values never become SQL text. */
@Component
final class QueryPlanSqlCompiler {
    private static final Map<String, String> COMMON = Map.ofEntries(
            Map.entry("employee.id", "employee_id"),
            Map.entry("employee.full_name", "employee_full_name"),
            Map.entry("employment_period.id", "employment_period_id"),
            Map.entry("department.id", "department_id"),
            Map.entry("department.name", "department_name"),
            Map.entry("position.id", "position_id"),
            Map.entry("position.name", "position_name"));
    private static final Map<String, String> ATTENDANCE = Map.ofEntries(
            Map.entry("attendance.work_date", "attendance_work_date"),
            Map.entry("attendance.status", "attendance_status"),
            Map.entry("attendance.first_in_local_time", "attendance_first_in_local_time"),
            Map.entry("attendance.late_minutes", "attendance_late_minutes"));
    private static final Map<String, String> METRICS = Map.of(
            "employees.distinct_count", "COUNT(DISTINCT base.employee_id)",
            "attendance.record_count", "COUNT(DISTINCT base.attendance_record_id)",
            "attendance.late_days", "COUNT(DISTINCT CASE WHEN base.attendance_status = 'LATE' THEN base.attendance_record_id END)",
            "attendance.late_minutes_total", "COALESCE(SUM(CASE WHEN base.attendance_status = 'LATE' THEN base.attendance_late_minutes ELSE 0 END), 0)");
    private static final String COMMON_PROJECTION = """
            e.id AS employee_id,
            concat_ws(' ', e.first_name, NULLIF(BTRIM(e.middle_name), ''),
                e.last_name, NULLIF(BTRIM(e.second_last_name), '')) AS employee_full_name,
            ep.id AS employment_period_id,
            ea.id AS primary_assignment_id,
            d.id AS department_id, d.name AS department_name,
            pos.id AS position_id, pos.name AS position_name
            """;
    private static final String ORGANIZATION_JOIN = """
            LEFT JOIN departments d ON d.id = ea.department_id
            LEFT JOIN positions pos ON pos.id = ea.position_id
            """;

    record CompiledQuery(String sql, List<Object> parameters, List<QueryResultV1.Column> columns,
                         int pageNumber, int pageSize) {
        CompiledQuery {
            parameters = List.copyOf(parameters);
            columns = List.copyOf(columns);
        }
    }

    private final SemanticCatalogV1 catalog;

    QueryPlanSqlCompiler(SemanticCatalogV1 catalog) {
        this.catalog = catalog;
    }

    CompiledQuery compile(QueryPlanValidationService.ValidatedPlan validated, ZoneId tenantZone) {
        QueryPlanV1 plan = validated.plan();
        SemanticCatalogV1.Dataset dataset = catalog.dataset(plan.dataset());
        Map<String, String> aliases = aliases(plan.dataset());
        List<Object> parameters = new ArrayList<>();
        StringBuilder sql = new StringBuilder("WITH base AS (");
        if ("attendance.daily".equals(plan.dataset())) {
            if (tenantZone == null) throw new QueryPlanException(QueryPlanException.Code.UNSUPPORTED_QUERY);
            sql.append(attendanceBase());
            parameters.add(tenantZone.getId());
            parameters.add(plan.dateRange().from());
            parameters.add(plan.dateRange().toExclusive());
        } else {
            sql.append(rosterBase());
            for (int i = 0; i < 4; i++) parameters.add(plan.asOfDate());
        }
        sql.append(") SELECT ");

        List<String> projected = new ArrayList<>();
        List<QueryResultV1.Column> columns = new ArrayList<>();
        boolean aggregate = "aggregate".equals(plan.mode());
        for (String id : plan.select()) {
            String expression = aggregate ? groupedDimension(id, plan.groupBy(), aliases) : dimension(id, aliases);
            projected.add(expression + " AS c" + projected.size());
            columns.add(new QueryResultV1.Column(id, dataset.dimensions().get(id).type()));
        }
        for (String id : plan.metrics()) {
            projected.add(metric(id) + " AS c" + projected.size());
            columns.add(new QueryResultV1.Column(id, SemanticCatalogV1.ValueType.INTEGER));
        }
        projected.add(aggregate ? "BOOL_OR(base.primary_assignment_id IS NULL) AS missing_primary_assignment"
                : "base.primary_assignment_id IS NULL AS missing_primary_assignment");
        sql.append(String.join(", ", projected)).append(" FROM base");

        if (!plan.filters().isEmpty()) {
            List<String> predicates = new ArrayList<>();
            for (QueryPlanV1.Filter filter : plan.filters())
                predicates.add(predicate(filter, dataset.dimensions().get(filter.field()), aliases, parameters));
            sql.append(" WHERE ").append(String.join(" AND ", predicates));
        }
        if (aggregate && !plan.groupBy().isEmpty()) {
            sql.append(" GROUP BY ");
            sql.append(String.join(", ", plan.groupBy().stream().map(id -> dimension(id, aliases)).toList()));
        }
        if (!plan.having().isEmpty()) {
            List<String> conditions = new ArrayList<>();
            for (QueryPlanV1.Having having : plan.having()) {
                conditions.add(metric(having.metric()) + comparison(having.op()) + "?");
                parameters.add(having.value());
            }
            sql.append(" HAVING ").append(String.join(" AND ", conditions));
        }

        List<String> order = new ArrayList<>();
        for (QueryPlanV1.Order item : plan.orderBy()) {
            String expression = dataset.metrics().containsKey(item.field()) ? metric(item.field())
                    : aggregate ? groupedDimension(item.field(), plan.groupBy(), aliases)
                    : dimension(item.field(), aliases);
            order.add(expression + " " + item.direction());
        }
        if (aggregate) {
            for (String id : plan.groupBy()) order.add(dimension(id, aliases) + " ASC");
        } else {
            order.add("attendance.daily".equals(plan.dataset())
                    ? "base.attendance_record_id ASC" : "base.employee_id ASC");
        }
        if (!order.isEmpty()) sql.append(" ORDER BY ").append(String.join(", ", order));
        sql.append(" LIMIT ? OFFSET ?");
        parameters.add(plan.page().size() + 1);
        parameters.add(plan.page().number() * plan.page().size());
        return new CompiledQuery(sql.toString(), parameters, columns,
                plan.page().number(), plan.page().size());
    }

    Map<String, String> supportedDimensions(String dataset) { return aliases(dataset); }

    Map<String, String> supportedMetrics(String dataset) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String id : catalog.dataset(dataset).metrics().keySet()) result.put(id, metric(id));
        return Map.copyOf(result);
    }

    private static Map<String, String> aliases(String dataset) {
        if ("employees.roster".equals(dataset)) return COMMON;
        if (!"attendance.daily".equals(dataset)) throw unsupported();
        Map<String, String> result = new LinkedHashMap<>(COMMON);
        result.putAll(ATTENDANCE);
        return Map.copyOf(result);
    }

    private static String dimension(String id, Map<String, String> aliases) {
        String alias = aliases.get(id);
        if (alias == null) throw unsupported();
        return "base." + alias;
    }

    private static String groupedDimension(String id, List<String> groups, Map<String, String> aliases) {
        String expression = dimension(id, aliases);
        return groups.contains(id) ? expression : "MIN(" + expression + ")";
    }

    private static String metric(String id) {
        String expression = METRICS.get(id);
        if (expression == null) throw unsupported();
        return expression;
    }

    private static String predicate(QueryPlanV1.Filter filter, SemanticCatalogV1.Dimension descriptor,
                                    Map<String, String> aliases, List<Object> parameters) {
        String field = dimension(filter.field(), aliases);
        if ("is_null".equals(filter.op())) return field + " IS NULL";
        if ("is_not_null".equals(filter.op())) return field + " IS NOT NULL";
        if ("in".equals(filter.op())) {
            List<?> values = (List<?>) filter.value();
            for (Object value : values) parameters.add(typed(value, descriptor.type()));
            return field + " IN (" + String.join(", ", java.util.Collections.nCopies(values.size(), "?")) + ")";
        }
        if ("between".equals(filter.op())) {
            List<?> values = (List<?>) filter.value();
            parameters.add(typed(values.get(0), descriptor.type()));
            parameters.add(typed(values.get(1), descriptor.type()));
            return "(" + field + " >= ? AND " + field
                    + (descriptor.type() == SemanticCatalogV1.ValueType.DATE ? " < ?" : " <= ?") + ")";
        }
        parameters.add(typed(filter.value(), descriptor.type()));
        return field + comparison(filter.op()) + "?";
    }

    private static Object typed(Object value, SemanticCatalogV1.ValueType type) {
        return switch (type) {
            case UUID -> UUID.fromString((String) value);
            case DATE -> LocalDate.parse((String) value);
            case LOCAL_TIME -> LocalTime.parse((String) value);
            case INTEGER -> (BigDecimal) value;
            case TEXT, ENUM -> value;
        };
    }

    private static String comparison(String operator) {
        return switch (operator) {
            case "eq" -> " = ";
            case "neq" -> " <> ";
            case "gt" -> " > ";
            case "gte" -> " >= ";
            case "lt" -> " < ";
            case "lte" -> " <= ";
            default -> throw unsupported();
        };
    }

    private static QueryPlanException unsupported() {
        return new QueryPlanException(QueryPlanException.Code.UNSUPPORTED_QUERY);
    }

    private static String attendanceBase() {
        return """
                SELECT ar.id AS attendance_record_id, ar.work_date AS attendance_work_date,
                    ar.status AS attendance_status, ar.late_minutes AS attendance_late_minutes,
                    timezone(CAST(? AS text), ar.actual_first_in_at)::time AS attendance_first_in_local_time,
                """ + COMMON_PROJECTION + """
                FROM attendance_records ar
                JOIN employees e ON e.id = ar.employee_id
                JOIN employment_periods ep ON ep.id = ar.employment_period_id
                    AND ep.employee_id = ar.employee_id
                    AND ep.start_date <= ar.work_date
                    AND (ep.end_date IS NULL OR ep.end_date >= ar.work_date)
                LEFT JOIN employee_assignments ea ON ea.employee_id = ar.employee_id
                    AND ea.employment_period_id = ar.employment_period_id
                    AND ea.assignment_type = 'PRIMARY' AND ea.assignment_status = 'ACTIVE'
                    AND ea.start_date <= ar.work_date
                    AND (ea.end_date IS NULL OR ea.end_date >= ar.work_date)
                """ + ORGANIZATION_JOIN + """
                WHERE ar.work_date >= ? AND ar.work_date < ?
                """;
    }

    private static String rosterBase() {
        return "SELECT " + COMMON_PROJECTION + """
                FROM employees e
                JOIN employment_periods ep ON ep.employee_id = e.id
                    AND ep.start_date <= ? AND (ep.end_date IS NULL OR ep.end_date >= ?)
                LEFT JOIN employee_assignments ea ON ea.employee_id = e.id
                    AND ea.employment_period_id = ep.id
                    AND ea.assignment_type = 'PRIMARY' AND ea.assignment_status = 'ACTIVE'
                    AND ea.start_date <= ? AND (ea.end_date IS NULL OR ea.end_date >= ?)
                """ + ORGANIZATION_JOIN;
    }
}
