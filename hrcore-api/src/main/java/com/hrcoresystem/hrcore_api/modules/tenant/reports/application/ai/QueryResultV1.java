package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A single bounded page. Column keys are catalog identifiers, not SQL aliases. */
public record QueryResultV1(List<Column> columns, List<Map<String, Object>> rows,
                            int pageNumber, int pageSize, boolean hasMore, List<String> warnings) {
    public record Column(String key, SemanticCatalogV1.ValueType type) {}

    public QueryResultV1 {
        columns = List.copyOf(columns);
        rows = rows.stream()
                .map(row -> Collections.unmodifiableMap(new LinkedHashMap<>(row)))
                .toList();
        warnings = List.copyOf(warnings);
    }
}
