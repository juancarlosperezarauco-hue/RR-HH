package com.hrcoresystem.hrcore_api.bootstrap.tenant;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.service.OwnerPermissionSynchronizer;

/** Initializes the SaaS tenant security and settings baseline. */
@Service
public class TenantBootstrapService {
  private final JdbcTemplate jdbcTemplate;
  private final OwnerPermissionSynchronizer ownerPermissionSynchronizer;

  public TenantBootstrapService(@Qualifier("targetDataSource") DataSource targetDataSource,
      OwnerPermissionSynchronizer ownerPermissionSynchronizer) {
    this.jdbcTemplate = new JdbcTemplate(targetDataSource);
    this.ownerPermissionSynchronizer = ownerPermissionSynchronizer;
  }

  public void initializeTenantData(String schemaName, String tenantName) {
    validateSchemaName(schemaName);
    Integer ownerRoleCount = jdbcTemplate.queryForObject(
        """
            SELECT COUNT(*) FROM %s.tenant_roles
            WHERE code = 'OWNER_ADMIN' AND system_role = TRUE AND active = TRUE
            """.formatted(schemaName),
        Integer.class);
    if (ownerRoleCount == null || ownerRoleCount != 1) {
      throw new IllegalStateException("Tenant schema is missing the OWNER_ADMIN system role");
    }
    ownerPermissionSynchronizer.synchronizeTenant(schemaName);
    jdbcTemplate.update("""
        INSERT INTO %s.tenant_settings (setting_key, setting_value, created_at, updated_at)
        VALUES ('company.name', ?, NOW(), NOW())
        ON CONFLICT (setting_key) DO NOTHING
        """.formatted(schemaName), tenantName);
  }

  private void validateSchemaName(String schemaName) {
    if (schemaName == null || schemaName.isBlank() || !schemaName.matches("^[a-zA-Z0-9_]+$")) {
      throw new IllegalArgumentException("Schema name is invalid");
    }
  }
}
