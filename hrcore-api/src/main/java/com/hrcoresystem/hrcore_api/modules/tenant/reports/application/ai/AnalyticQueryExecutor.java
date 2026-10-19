package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Runs one compiled page on Spring's tenant-aware datasource. */
@Service
public class AnalyticQueryExecutor {
    private static final int TIMEOUT_SECONDS = 3;
    private final JdbcTemplate jdbc;

    public AnalyticQueryExecutor(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true, timeout = TIMEOUT_SECONDS)
    public QueryResultV1 execute(QueryPlanSqlCompiler.CompiledQuery query) {
        try {
            return jdbc.execute((ConnectionCallback<QueryResultV1>) connection -> {
                try (Statement controls = connection.createStatement()) {
                    controls.execute("SET TRANSACTION READ ONLY");
                    controls.execute("SET LOCAL statement_timeout = '3000ms'");
                }
                try (PreparedStatement statement = connection.prepareStatement(query.sql())) {
                    statement.setQueryTimeout(TIMEOUT_SECONDS);
                    statement.setMaxRows(query.pageSize() + 1);
                    for (int i = 0; i < query.parameters().size(); i++)
                        statement.setObject(i + 1, query.parameters().get(i));
                    try (ResultSet result = statement.executeQuery()) {
                        return page(query, result);
                    }
                }
            });
        } catch (QueryTimeoutException | TransactionTimedOutException timeout) {
            throw new QueryPlanException(QueryPlanException.Code.QUERY_TIMEOUT);
        } catch (DataAccessException failure) {
            for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                if (cause instanceof SQLException sql && "57014".equals(sql.getSQLState()))
                    throw new QueryPlanException(QueryPlanException.Code.QUERY_TIMEOUT);
            }
            throw new QueryPlanException(QueryPlanException.Code.SERVICE_UNAVAILABLE);
        }
    }

    private static QueryResultV1 page(QueryPlanSqlCompiler.CompiledQuery query, ResultSet result)
            throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        Set<String> warnings = new LinkedHashSet<>();
        while (rows.size() <= query.pageSize() && result.next()) {
            if (rows.size() == query.pageSize()) break;
            if (result.getBoolean("missing_primary_assignment"))
                warnings.add("MISSING_PRIMARY_ASSIGNMENT");
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 0; i < query.columns().size(); i++) {
                QueryResultV1.Column column = query.columns().get(i);
                Object value = value(result, "c" + i, column.type());
                row.put(column.key(), value);
            }
            rows.add(row);
        }
        boolean hasMore = result.getRow() > query.pageSize();
        return new QueryResultV1(query.columns(), rows, query.pageNumber(), query.pageSize(),
                hasMore, List.copyOf(warnings));
    }

    private static Object value(ResultSet result, String alias, SemanticCatalogV1.ValueType type)
            throws SQLException {
        return switch (type) {
            case UUID -> result.getObject(alias, UUID.class);
            case DATE -> result.getObject(alias, LocalDate.class);
            case LOCAL_TIME -> result.getObject(alias, LocalTime.class);
            case TEXT, ENUM -> result.getString(alias);
            case INTEGER -> {
                Number number = (Number) result.getObject(alias);
                yield number == null ? null : number.longValue();
            }
        };
    }
}
