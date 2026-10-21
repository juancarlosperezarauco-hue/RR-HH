package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Server-owned, non-executable catalog for the first two analytic datasets. */
@Component
public final class SemanticCatalogV1 {
    public static final String VERSION = "1";

    public enum ValueType { UUID, TEXT, DATE, LOCAL_TIME, ENUM, INTEGER }

    public record Dimension(String id, ValueType type, Set<String> operators,
                            Set<String> permissions, boolean nominal, boolean nullable,
                            boolean groupKey, String labelFor, boolean enabled) {
        public Dimension {
            operators = Set.copyOf(operators);
            permissions = Set.copyOf(permissions);
        }
    }

    public record Metric(String id, String grain, Set<String> permissions) {
        public Metric { permissions = Set.copyOf(permissions); }
    }

    public record TemporalRelation(String path, String effectiveAt,
                                   String cardinality, String rule) {}

    public record Dataset(String id, String grain, String reportPermission, String detailPermission,
                          Map<String, Dimension> dimensions, Map<String, Metric> metrics,
                          List<TemporalRelation> temporalRelations) {
        public Dataset {
            dimensions = Map.copyOf(dimensions);
            metrics = Map.copyOf(metrics);
            temporalRelations = List.copyOf(temporalRelations);
        }
    }

    private static final Set<String> IDENTIFIER_OPS = Set.of("eq", "neq", "in");
    private static final Set<String> TEXT_OPS = Set.of("eq", "neq", "in");
    private static final Set<String> ORDERED_OPS = Set.of("eq", "neq", "gt", "gte", "lt", "lte", "between", "in");
    private static final Set<String> OPTIONAL_OPS = Set.of("eq", "neq", "gt", "gte", "lt", "lte", "between", "in", "is_null", "is_not_null");
    private static final Set<String> EMPLOYEE_READ = Set.of("hr.reports.employees.read", "employees.list");

    private final Map<String, Dataset> datasets;

    public SemanticCatalogV1() {
        Map<String, Dimension> roster = new LinkedHashMap<>();
        commonDimensions(roster);
        roster.put("employee.employment_status_at_date", dim("employee.employment_status_at_date",
                ValueType.ENUM, Set.of(), EMPLOYEE_READ, true, false, false, null, false));

        Map<String, Dimension> attendance = new LinkedHashMap<>();
        commonDimensions(attendance);
        attendance.put("attendance.work_date", dim("attendance.work_date", ValueType.DATE,
                ORDERED_OPS, Set.of(), false, false, true, null, true));
        attendance.put("attendance.status", dim("attendance.status", ValueType.ENUM,
                TEXT_OPS, Set.of(), false, false, true, null, true));
        attendance.put("attendance.first_in_local_time", dim("attendance.first_in_local_time",
                ValueType.LOCAL_TIME, OPTIONAL_OPS, Set.of(), false, true, false, null, true));
        attendance.put("attendance.late_minutes", dim("attendance.late_minutes", ValueType.INTEGER,
                ORDERED_OPS, Set.of(), false, false, false, null, true));

        Dataset employees = new Dataset("employees.roster", "employee at asOfDate",
                "hr.reports.employees.read", "employees.list", roster,
                Map.of("employees.distinct_count", new Metric("employees.distinct_count",
                        "distinct employee.id", Set.of())),
                List.of(
                        new TemporalRelation("employee -> employment period", "asOfDate", "zero or one",
                                "period covers the reference date"),
                        new TemporalRelation("employment period -> PRIMARY assignment", "asOfDate", "zero or one",
                                "assignment is ACTIVE and covers the reference date"),
                        new TemporalRelation("assignment -> department and position", "asOfDate", "zero or one",
                                "entity names are current labels")));
        Dataset daily = new Dataset("attendance.daily", "attendance record id",
                "hr.reports.attendance.read", "attendance.records.list", attendance,
                Map.of(
                        "attendance.record_count", new Metric("attendance.record_count", "record id", Set.of()),
                        "attendance.late_days", new Metric("attendance.late_days", "record id with status LATE", Set.of()),
                        "attendance.late_minutes_total", new Metric("attendance.late_minutes_total",
                                "sum late_minutes for records with status LATE", Set.of())),
                List.of(
                        new TemporalRelation("attendance record -> employee and employment period", "work_date", "one",
                                "record and period belong to the same employee"),
                        new TemporalRelation("employment period -> PRIMARY assignment", "work_date", "zero or one",
                                "assignment is ACTIVE and covers the work date"),
                        new TemporalRelation("assignment -> department and position", "work_date", "zero or one",
                                "entity names are current labels")));
        datasets = Map.of(employees.id(), employees, daily.id(), daily);
    }

    public Map<String, Dataset> datasets() { return datasets; }

    public Dataset dataset(String id) { return datasets.get(id); }

    private static void commonDimensions(Map<String, Dimension> dimensions) {
        dimensions.put("employee.id", dim("employee.id", ValueType.UUID, IDENTIFIER_OPS,
                EMPLOYEE_READ, true, false, true, null, true));
        dimensions.put("employee.full_name", dim("employee.full_name", ValueType.TEXT, TEXT_OPS,
                EMPLOYEE_READ, true, false, false, "employee.id", true));
        dimensions.put("employment_period.id", dim("employment_period.id", ValueType.UUID,
                IDENTIFIER_OPS, EMPLOYEE_READ, true, false, true, null, true));
        dimensions.put("department.id", dim("department.id", ValueType.UUID,
                IDENTIFIER_OPS, Set.of("departments.list"), false, true, true, null, true));
        dimensions.put("department.name", dim("department.name", ValueType.TEXT,
                withNull(TEXT_OPS), Set.of("departments.list"), false, true, false, "department.id", true));
        dimensions.put("position.id", dim("position.id", ValueType.UUID,
                IDENTIFIER_OPS, Set.of("positions.list"), false, true, true, null, true));
        dimensions.put("position.name", dim("position.name", ValueType.TEXT,
                withNull(TEXT_OPS), Set.of("positions.list"), false, true, false, "position.id", true));
    }

    private static Set<String> withNull(Set<String> operators) {
        java.util.HashSet<String> result = new java.util.HashSet<>(operators);
        result.add("is_null");
        result.add("is_not_null");
        return Set.copyOf(result);
    }

    private static Dimension dim(String id, ValueType type, Set<String> ops, Set<String> permissions,
                                 boolean nominal, boolean nullable, boolean groupKey,
                                 String labelFor, boolean enabled) {
        return new Dimension(id, type, ops, permissions, nominal, nullable, groupKey, labelFor, enabled);
    }
}
