package com.hrcoresystem.hrcore_api.bootstrap.tenant;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.List;

/** Initializes the SaaS tenant security and settings baseline. */
@Service
public class TenantBootstrapService {
    private static final List<String> DEFAULT_PERMISSION_CODES = List.of(
            "access.permissions.read", "access.roles.read", "access.roles.create",
            "access.roles.detail", "access.roles.update", "access.roles.activate",
            "access.roles.deactivate", "access.users.roles.read", "access.users.roles.assign",
            "users.list", "users.create", "users.detail", "users.update", "users.activate",
            "users.deactivate", "audit.events.read", "notifications.templates.read",
            "notifications.templates.detail", "notifications.deliveries.read",
            "billing.subscription.manage", "backups.create", "backups.list", "backups.detail",
            "backups.download", "backups.restore");

    private final JdbcTemplate jdbcTemplate;

    public TenantBootstrapService(@Qualifier("targetDataSource") DataSource targetDataSource) {
        this.jdbcTemplate = new JdbcTemplate(targetDataSource);
    }

    public void initializeTenantData(String schemaName, String tenantName) {
        validateSchemaName(schemaName);
        seedRole(schemaName, "OWNER_ADMIN", "Tenant owner administrator role");
        seedPermissions(schemaName);
        jdbcTemplate.update("""
                INSERT INTO %s.tenant_settings (setting_key, setting_value, created_at, updated_at)
                VALUES ('company.name', ?, NOW(), NOW())
                ON CONFLICT (setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value, updated_at = NOW()
                """.formatted(schemaName), tenantName);
    }

    private void seedRole(String schemaName, String name, String description) {
        jdbcTemplate.update("""
                INSERT INTO %s.tenant_roles (name, description, active, created_at)
                VALUES (?, ?, true, NOW())
                ON CONFLICT (name) DO UPDATE SET active = true, description = EXCLUDED.description
                """.formatted(schemaName), name, description);
    }

    private void seedPermissions(String schemaName) {
        for (String code : DEFAULT_PERMISSION_CODES) {
            jdbcTemplate.update("""
                    INSERT INTO %s.tenant_role_permissions (role_id, permission_code, assigned_at)
                    SELECT r.id, ?, NOW() FROM %s.tenant_roles r
                    JOIN public.system_permissions p ON p.code = ? AND p.active = true
                    WHERE r.name = 'OWNER_ADMIN'
                    ON CONFLICT (role_id, permission_code) DO NOTHING
                    """.formatted(schemaName, schemaName), code, code);
        }
    }

    private void validateSchemaName(String schemaName) {
        if (schemaName == null || schemaName.isBlank() || !schemaName.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Schema name is invalid");
        }
    }
}
