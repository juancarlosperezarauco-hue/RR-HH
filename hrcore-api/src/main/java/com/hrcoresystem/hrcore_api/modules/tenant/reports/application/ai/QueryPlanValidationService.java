package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.model.AttendanceStatus;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

import static com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.QueryPlanException.Code.*;

/** Strict QueryPlan v1 shape and catalog semantics, without data access. */
@Service
public final class QueryPlanValidationService {
    private static final int MAX_BYTES = 128 * 1024;
    private static final Set<String> ROOT = Set.of("version", "catalogVersion", "dataset", "mode",
            "asOfDate", "dateRange", "select", "metrics", "filters", "groupBy", "having",
            "orderBy", "page", "visualization");
    private static final Set<String> FILTER_OPS = Set.of("eq", "neq", "in", "gt", "gte", "lt",
            "lte", "between", "is_null", "is_not_null");
    private static final Set<String> HAVING_OPS = Set.of("eq", "neq", "gt", "gte", "lt", "lte");
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final Pattern TIME_PATTERN = Pattern.compile("[0-9]{2}:[0-9]{2}:[0-9]{2}");

    private final ObjectMapper mapper;
    private final SemanticCatalogV1 catalog;

    public QueryPlanValidationService(ObjectMapper mapper, SemanticCatalogV1 catalog) {
        this.mapper = mapper.copy();
        this.mapper.getFactory().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        this.catalog = catalog;
    }

    /** The only type accepted by the authorization policy; callers cannot construct one. */
    public static final class ValidatedPlan {
        private final QueryPlanV1 plan;
        private ValidatedPlan(QueryPlanV1 plan) { this.plan = plan; }
        public QueryPlanV1 plan() { return plan; }
    }

    public ValidatedPlan validate(String json) {
        if (json == null) throw error(INVALID_QUERY_PLAN);
        if (json.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) throw error(QUERY_TOO_COMPLEX);
        try (JsonParser parser = mapper.getFactory().createParser(json)) {
            JsonNode root = mapper.readTree(parser);
            if (root == null || parser.nextToken() != null) throw error(INVALID_QUERY_PLAN);
            QueryPlanV1 plan = parse(root);
            validateSemantics(plan);
            return new ValidatedPlan(plan);
        } catch (IOException ex) {
            throw error(INVALID_QUERY_PLAN);
        }
    }

    private QueryPlanV1 parse(JsonNode root) {
        object(root, ROOT, Set.of("version", "catalogVersion", "dataset", "mode", "select",
                "metrics", "filters", "groupBy", "having", "orderBy", "page", "visualization"));
        String version = string(root.get("version"));
        String catalogVersion = string(root.get("catalogVersion"));
        if (!"1".equals(version)) throw error(INVALID_QUERY_PLAN);
        if (!SemanticCatalogV1.VERSION.equals(catalogVersion)) throw error(CATALOG_VERSION_MISMATCH);
        String dataset = string(root.get("dataset"));
        if (catalog.dataset(dataset) == null) throw error(INVALID_QUERY_PLAN);
        String mode = string(root.get("mode"));
        if (!Set.of("detail", "aggregate").contains(mode)) throw error(INVALID_QUERY_PLAN);

        LocalDate asOfDate = root.has("asOfDate") ? date(root.get("asOfDate")) : null;
        QueryPlanV1.DateRange range = root.has("dateRange") ? dateRange(root.get("dateRange")) : null;
        List<String> select = ids(root.get("select"), 12, false);
        List<String> metrics = ids(root.get("metrics"), 8, true);
        List<QueryPlanV1.Filter> filters = filters(root.get("filters"));
        List<String> groupBy = ids(root.get("groupBy"), 8, false);
        List<QueryPlanV1.Having> having = having(root.get("having"));
        List<QueryPlanV1.Order> orderBy = orderBy(root.get("orderBy"));
        QueryPlanV1.Page page = page(root.get("page"));
        QueryPlanV1.Visualization visualization = visualization(root.get("visualization"));
        return new QueryPlanV1(version, catalogVersion, dataset, mode, asOfDate, range,
                select, metrics, filters, groupBy, having, orderBy, page, visualization);
    }

    private QueryPlanV1.DateRange dateRange(JsonNode node) {
        object(node, Set.of("field", "from", "toExclusive"), Set.of("field", "from", "toExclusive"));
        String field = string(node.get("field"));
        if (!"attendance.work_date".equals(field)) throw error(INVALID_QUERY_PLAN);
        return new QueryPlanV1.DateRange(field, date(node.get("from")), date(node.get("toExclusive")));
    }

    private List<String> ids(JsonNode node, int max, boolean metric) {
        array(node, max);
        List<String> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (JsonNode item : node) {
            String id = string(item);
            if (!seen.add(id) || !knownId(id, metric)) throw error(INVALID_QUERY_PLAN);
            result.add(id);
        }
        return List.copyOf(result);
    }

    private boolean knownId(String id, boolean metric) {
        return catalog.datasets().values().stream().anyMatch(dataset -> metric
                ? dataset.metrics().containsKey(id) : dataset.dimensions().containsKey(id));
    }

    private List<QueryPlanV1.Filter> filters(JsonNode node) {
        array(node, 12);
        List<QueryPlanV1.Filter> result = new ArrayList<>();
        for (JsonNode item : node) {
            object(item, Set.of("field", "op", "value"), Set.of("field", "op"));
            String field = string(item.get("field"));
            String op = string(item.get("op"));
            if (!knownId(field, false) || !FILTER_OPS.contains(op)) throw error(INVALID_QUERY_PLAN);
            boolean hasValue = item.has("value");
            if (op.startsWith("is_")) {
                if (hasValue) throw error(INVALID_QUERY_PLAN);
                result.add(new QueryPlanV1.Filter(field, op, null));
            } else {
                if (!hasValue) throw error(INVALID_QUERY_PLAN);
                JsonNode value = item.get("value");
                if ("in".equals(op) || "between".equals(op)) {
                    array(value, "in".equals(op) ? 50 : 2);
                    if (value.size() < ("in".equals(op) ? 1 : 2)) throw error(INVALID_QUERY_PLAN);
                    List<Object> values = new ArrayList<>();
                    for (JsonNode entry : value) values.add(scalar(entry));
                    result.add(new QueryPlanV1.Filter(field, op, List.copyOf(values)));
                } else {
                    result.add(new QueryPlanV1.Filter(field, op, scalar(value)));
                }
            }
        }
        return List.copyOf(result);
    }

    private List<QueryPlanV1.Having> having(JsonNode node) {
        array(node, 6);
        List<QueryPlanV1.Having> result = new ArrayList<>();
        for (JsonNode item : node) {
            object(item, Set.of("metric", "op", "value"), Set.of("metric", "op", "value"));
            String metric = string(item.get("metric"));
            String op = string(item.get("op"));
            if (!knownId(metric, true) || !HAVING_OPS.contains(op) || !item.get("value").isNumber())
                throw error(INVALID_QUERY_PLAN);
            result.add(new QueryPlanV1.Having(metric, op, item.get("value").decimalValue()));
        }
        return List.copyOf(result);
    }

    private List<QueryPlanV1.Order> orderBy(JsonNode node) {
        array(node, 4);
        List<QueryPlanV1.Order> result = new ArrayList<>();
        for (JsonNode item : node) {
            object(item, Set.of("field", "direction"), Set.of("field", "direction"));
            String field = string(item.get("field"));
            String direction = string(item.get("direction"));
            if ((!knownId(field, false) && !knownId(field, true)) || !Set.of("ASC", "DESC").contains(direction))
                throw error(INVALID_QUERY_PLAN);
            result.add(new QueryPlanV1.Order(field, direction));
        }
        return List.copyOf(result);
    }

    private QueryPlanV1.Page page(JsonNode node) {
        object(node, Set.of("number", "size"), Set.of("number", "size"));
        int number = integer(node.get("number"));
        int size = integer(node.get("size"));
        if (number < 0 || number > 1000 || size < 1 || size > 100) throw error(QUERY_TOO_COMPLEX);
        return new QueryPlanV1.Page(number, size);
    }

    private QueryPlanV1.Visualization visualization(JsonNode node) {
        object(node, Set.of("type", "x", "y"), Set.of("type"));
        String type = string(node.get("type"));
        if (!Set.of("table", "bar").contains(type)) throw error(INVALID_QUERY_PLAN);
        String x = node.has("x") ? string(node.get("x")) : null;
        String y = node.has("y") ? string(node.get("y")) : null;
        if ((x != null && !knownId(x, false)) || (y != null && !knownId(y, true))
                || ("bar".equals(type) && (x == null || y == null))) throw error(INVALID_QUERY_PLAN);
        return new QueryPlanV1.Visualization(type, x, y);
    }

    private void validateSemantics(QueryPlanV1 plan) {
        SemanticCatalogV1.Dataset dataset = catalog.dataset(plan.dataset());
        if ("employees.roster".equals(plan.dataset())) {
            if (plan.asOfDate() == null || plan.dateRange() != null) throw error(INVALID_QUERY_PLAN);
        } else {
            if (plan.asOfDate() != null || plan.dateRange() == null) throw error(INVALID_QUERY_PLAN);
            long days = ChronoUnit.DAYS.between(plan.dateRange().from(), plan.dateRange().toExclusive());
            if (days <= 0) throw error(INVALID_QUERY_PLAN);
            if (days > 366) throw error(QUERY_TOO_COMPLEX);
        }
        boolean aggregate = "aggregate".equals(plan.mode());
        if (aggregate ? plan.metrics().isEmpty()
                : plan.select().isEmpty() || !plan.metrics().isEmpty()
                || !plan.groupBy().isEmpty() || !plan.having().isEmpty()) throw error(INVALID_QUERY_PLAN);

        for (String id : plan.select()) dimension(dataset, id);
        for (String id : plan.groupBy()) {
            if (!dimension(dataset, id).groupKey()) throw error(UNSUPPORTED_QUERY);
        }
        for (String id : plan.metrics()) metric(dataset, id);
        if (aggregate) {
            for (String id : plan.select()) assertGrouped(dataset, id, plan.groupBy());
        }
        for (QueryPlanV1.Filter filter : plan.filters()) validateFilter(dimension(dataset, filter.field()), filter);
        for (QueryPlanV1.Having having : plan.having()) {
            metric(dataset, having.metric());
            if (!plan.metrics().contains(having.metric())) throw error(INVALID_QUERY_PLAN);
        }
        for (QueryPlanV1.Order order : plan.orderBy()) {
            if (dataset.metrics().containsKey(order.field())) {
                metric(dataset, order.field());
                if (!aggregate || !plan.metrics().contains(order.field())) throw error(INVALID_QUERY_PLAN);
            } else {
                dimension(dataset, order.field());
                if (aggregate) assertGrouped(dataset, order.field(), plan.groupBy());
            }
        }
        QueryPlanV1.Visualization view = plan.visualization();
        if ("table".equals(view.type())) {
            if (view.x() != null || view.y() != null) throw error(INVALID_QUERY_PLAN);
        } else {
            if (!aggregate || !plan.select().contains(view.x()) || !plan.metrics().contains(view.y()))
                throw error(INVALID_QUERY_PLAN);
            assertGrouped(dataset, view.x(), plan.groupBy());
            metric(dataset, view.y());
        }
    }

    private void assertGrouped(SemanticCatalogV1.Dataset dataset, String id, List<String> groups) {
        SemanticCatalogV1.Dimension dimension = dimension(dataset, id);
        if (!groups.contains(id) && (dimension.labelFor() == null || !groups.contains(dimension.labelFor())))
            throw error(UNSUPPORTED_QUERY);
    }

    private SemanticCatalogV1.Dimension dimension(SemanticCatalogV1.Dataset dataset, String id) {
        SemanticCatalogV1.Dimension value = dataset.dimensions().get(id);
        if (value == null || !value.enabled()) throw error(UNSUPPORTED_QUERY);
        return value;
    }

    private void metric(SemanticCatalogV1.Dataset dataset, String id) {
        if (!dataset.metrics().containsKey(id)) throw error(UNSUPPORTED_QUERY);
    }

    private void validateFilter(SemanticCatalogV1.Dimension dimension, QueryPlanV1.Filter filter) {
        if (!dimension.operators().contains(filter.op())) throw error(UNSUPPORTED_QUERY);
        if (filter.value() == null) {
            if (!dimension.nullable()) throw error(UNSUPPORTED_QUERY);
            return;
        }
        List<?> values = filter.value() instanceof List<?> list ? list : List.of(filter.value());
        for (Object value : values) validateValue(dimension, value);
        if ("between".equals(filter.op())) {
            Comparable<?> lower = orderedValue(dimension, values.get(0));
            Comparable<?> upper = orderedValue(dimension, values.get(1));
            @SuppressWarnings({"rawtypes", "unchecked"}) int comparison = ((Comparable) lower).compareTo(upper);
            if (comparison >= (dimension.type() == SemanticCatalogV1.ValueType.DATE ? 0 : 1))
                throw error(INVALID_QUERY_PLAN);
        }
    }

    private Comparable<?> orderedValue(SemanticCatalogV1.Dimension dimension, Object value) {
        return switch (dimension.type()) {
            case DATE -> LocalDate.parse((String) value);
            case LOCAL_TIME -> LocalTime.parse((String) value);
            case INTEGER -> (BigDecimal) value;
            default -> throw error(UNSUPPORTED_QUERY);
        };
    }

    private void validateValue(SemanticCatalogV1.Dimension dimension, Object value) {
        switch (dimension.type()) {
            case UUID -> {
                if (!(value instanceof String text) || !UUID_PATTERN.matcher(text).matches())
                    throw error(INVALID_QUERY_PLAN);
                UUID.fromString(text);
            }
            case TEXT -> {
                if (!(value instanceof String)) throw error(INVALID_QUERY_PLAN);
            }
            case ENUM -> {
                if (!(value instanceof String text)) throw error(INVALID_QUERY_PLAN);
                if ("attendance.status".equals(dimension.id())) {
                    try { AttendanceStatus.valueOf(text); }
                    catch (IllegalArgumentException ex) { throw error(INVALID_QUERY_PLAN); }
                }
            }
            case DATE -> dateText(value);
            case LOCAL_TIME -> {
                if (!(value instanceof String text) || !TIME_PATTERN.matcher(text).matches())
                    throw error(INVALID_QUERY_PLAN);
                try { LocalTime.parse(text); }
                catch (DateTimeParseException ex) { throw error(INVALID_QUERY_PLAN); }
            }
            case INTEGER -> {
                if (!(value instanceof BigDecimal number) || number.stripTrailingZeros().scale() > 0)
                    throw error(INVALID_QUERY_PLAN);
            }
        }
    }

    private static void dateText(Object value) {
        if (!(value instanceof String text)) throw error(INVALID_QUERY_PLAN);
        try { LocalDate.parse(text); }
        catch (DateTimeParseException ex) { throw error(INVALID_QUERY_PLAN); }
    }

    private static Object scalar(JsonNode node) {
        if (node.isTextual()) {
            String text = node.textValue();
            if (text.codePointCount(0, text.length()) > 160) throw error(INVALID_QUERY_PLAN);
            return text;
        }
        if (node.isNumber()) return node.decimalValue();
        if (node.isBoolean()) return node.booleanValue();
        throw error(INVALID_QUERY_PLAN);
    }

    private static String string(JsonNode node) {
        if (node == null || !node.isTextual()) throw error(INVALID_QUERY_PLAN);
        return node.textValue();
    }

    private static LocalDate date(JsonNode node) {
        String text = string(node);
        try { return LocalDate.parse(text); }
        catch (DateTimeParseException ex) { throw error(INVALID_QUERY_PLAN); }
    }

    private static int integer(JsonNode node) {
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt())
            throw error(INVALID_QUERY_PLAN);
        return node.intValue();
    }

    private static void array(JsonNode node, int max) {
        if (node == null || !node.isArray()) throw error(INVALID_QUERY_PLAN);
        if (node.size() > max) throw error(QUERY_TOO_COMPLEX);
    }

    private static void object(JsonNode node, Set<String> allowed, Set<String> required) {
        if (node == null || !node.isObject()) throw error(INVALID_QUERY_PLAN);
        if (!node.properties().stream().map(Map.Entry::getKey).allMatch(allowed::contains)
                || !required.stream().allMatch(node::has)) throw error(INVALID_QUERY_PLAN);
    }

    private static QueryPlanException error(QueryPlanException.Code code) {
        return new QueryPlanException(code);
    }
}
