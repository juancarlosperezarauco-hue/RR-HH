package com.hrcoresystem.hrcore_api.modules.identity.access.application.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.List;

@Service
public class OwnerPermissionSynchronizer {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    public OwnerPermissionSynchronizer(@Qualifier("targetDataSource") DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.tx = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    public void synchronizeRegisteredTenants() {
        List<String> schemas = jdbc.query(
                "SELECT schema_name FROM public.platform_tenants WHERE schema_name IS NOT NULL",
                (rs, rowNum) -> rs.getString(1)
        );
        for (String schema : schemas) synchronizeTenant(schema);
    }

    public void synchronizeTenant(String schema) {
        validate(schema);
        tx.executeWithoutResult(status -> {
            int inserted = jdbc.update("""
                    INSERT INTO %s.tenant_role_permissions(role_id, permission_code, assigned_at)
                    SELECT r.id, p.code, NOW()
                    FROM %s.tenant_roles r
                    CROSS JOIN public.system_permissions p
                    WHERE r.code = 'OWNER_ADMIN'
                      AND r.system_role = true
                      AND r.active = true
                      AND p.scope = 'TENANT'
                      AND p.active = true
                    ON CONFLICT(role_id, permission_code) DO NOTHING
                    """.formatted(schema, schema));
            if (inserted > 0 && jdbc.update(
                    "UPDATE %s.tenant_authorization_state SET authorization_version = authorization_version + 1, updated_at = NOW() WHERE id = 1".formatted(schema)
            ) != 1) {
                throw new IllegalStateException("Tenant authorization state is missing for " + schema);
            }
        });
    }

    private void validate(String schema) {
        if (schema == null || !schema.matches("^[a-zA-Z0-9_]+$") || schema.equalsIgnoreCase("public")) {
            throw new IllegalArgumentException("Invalid tenant schema");
        }
    }
}
