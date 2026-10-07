package com.hrcoresystem.hrcore_api.common.tenancy.validation;

import com.hrcoresystem.hrcore_api.common.tenancy.exception.TenantResolutionException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.List;

@Service
public class TenantSchemaReadinessService {

    private static final List<String> REQUIRED_TABLES = List.of(
            "tenant_users",
            "tenant_roles",
            "tenant_user_roles",
            "tenant_role_permissions"
    );

    private final JdbcTemplate jdbcTemplate;

    public TenantSchemaReadinessService(@Qualifier("targetDataSource") DataSource targetDataSource) {
        this.jdbcTemplate = new JdbcTemplate(targetDataSource);
    }

    public void assertTenantSchemaReady(String schemaName, String tenantHeaderName, String tenantSlug) {
        if (!StringUtils.hasText(schemaName)) {
            throw new TenantResolutionException("Tenant schema is not available");
        }

        if (!schemaExists(schemaName)) {
            throw new TenantResolutionException(
                    "Tenant schema '" + schemaName + "' was not found for tenant slug '" + tenantSlug + "'. " +
                            "Check the '" + tenantHeaderName + "' header for typos and verify the tenant was created."
            );
        }

        List<String> missingTables = REQUIRED_TABLES.stream()
                .filter(tableName -> !tableExists(schemaName, tableName))
                .toList();

        if (!missingTables.isEmpty()) {
            throw new TenantResolutionException(
                    "Tenant schema '" + schemaName + "' is not ready for tenant slug '" + tenantSlug + "'. " +
                            "Missing required tables: " + String.join(", ", missingTables) + ". " +
                            "Run tenant migrations before retrying."
            );
        }
    }

    private boolean schemaExists(String schemaName) {
        Integer schemaCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.schemata
                WHERE schema_name = ?
                """,
                Integer.class,
                schemaName
        );

        return schemaCount != null && schemaCount > 0;
    }

    private boolean tableExists(String schemaName, String tableName) {
        Integer tableCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = ? AND table_name = ?
                """,
                Integer.class,
                schemaName,
                tableName
        );

        return tableCount != null && tableCount > 0;
    }
}
