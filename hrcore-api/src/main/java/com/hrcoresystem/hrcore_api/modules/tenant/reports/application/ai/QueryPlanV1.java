package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai;

import java.time.LocalDate;
import java.util.List;

/** A descriptive plan. It contains no SQL, tenant identifier, or effective authorities. */
public record QueryPlanV1(
        String version, String catalogVersion, String dataset, String mode,
        LocalDate asOfDate, DateRange dateRange, List<String> select,
        List<String> metrics, List<Filter> filters, List<String> groupBy,
        List<Having> having, List<Order> orderBy, Page page,
        Visualization visualization) {

    public record DateRange(String field, LocalDate from, LocalDate toExclusive) {}
    public record Filter(String field, String op, Object value) {}
    public record Having(String metric, String op, java.math.BigDecimal value) {}
    public record Order(String field, String direction) {}
    public record Page(int number, int size) {}
    public record Visualization(String type, String x, String y) {}
}
