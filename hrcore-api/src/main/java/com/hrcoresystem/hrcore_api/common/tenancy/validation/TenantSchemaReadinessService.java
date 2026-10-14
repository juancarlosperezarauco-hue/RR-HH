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
      "tenant_role_permissions",
      "tenant_authorization_state",
      "tenant_auth_sessions");

  private final JdbcTemplate jdbcTemplate;

  public TenantSchemaReadinessService(@Qualifier("targetDataSource") DataSource targetDataSource) {
    this.jdbcTemplate = new JdbcTemplate(targetDataSource);
  }

  public void assertSchemaMigrated(String schemaName, String tenantSlug) {
    if (!StringUtils.hasText(schemaName)) {
      throw new TenantResolutionException("Tenant schema is not available");
    }

    if (!schemaName.matches("^[a-zA-Z0-9_]+$")) {
      throw new TenantResolutionException("Tenant schema contains invalid characters");
    }

    if (!schemaExists(schemaName)) {
      throw new TenantResolutionException(
          "Tenant schema '" + schemaName + "' was not found for tenant slug '" + tenantSlug + "'.");
    }

    List<String> missingTables = REQUIRED_TABLES.stream()
        .filter(tableName -> !tableExists(schemaName, tableName))
        .toList();

    if (!missingTables.isEmpty()) {
      throw new TenantResolutionException(
          "Tenant schema '" + schemaName + "' is not migrated for tenant slug '" + tenantSlug + "'. " +
              "Missing required tables: " + String.join(", ", missingTables) + ". " +
              "Run tenant migrations before retrying.");
    }
  }

  public void assertTenantProvisioned(String schemaName, String tenantSlug) {
    assertSchemaMigrated(schemaName, tenantSlug);

    Long ownerCount = jdbcTemplate.queryForObject(
        """
            SELECT COUNT(*)
            FROM %s.tenant_authorization_state s
            JOIN %s.tenant_users u ON u.id = s.owner_user_id
            JOIN %s.tenant_user_roles ur ON ur.user_id = u.id
            JOIN %s.tenant_roles r ON r.id = ur.role_id
            WHERE s.id = 1
              AND s.owner_user_id IS NOT NULL
              AND r.code = 'OWNER_ADMIN'
              AND r.system_role = TRUE
              AND r.active = TRUE
            """.formatted(schemaName, schemaName, schemaName, schemaName),
        Long.class);

    if (ownerCount == null || ownerCount != 1) {
      throw new TenantResolutionException("Tenant schema '" + schemaName + "' is not provisioned for tenant slug '" + tenantSlug + "'.");
    }
  }

  public void assertTenantOperational(String schemaName, String tenantSlug) {
    assertTenantProvisioned(schemaName, tenantSlug);
    Long activeOwnerCount = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM %s.tenant_authorization_state s
            JOIN %s.tenant_users u ON u.id = s.owner_user_id
            JOIN %s.tenant_user_roles ur ON ur.user_id = u.id
            JOIN %s.tenant_roles r ON r.id = ur.role_id
            WHERE s.id = 1 AND s.owner_user_id IS NOT NULL AND u.active = TRUE
              AND u.status = 'ACTIVE' AND r.code = 'OWNER_ADMIN' AND r.system_role = TRUE AND r.active = TRUE
            """.formatted(schemaName, schemaName, schemaName, schemaName), Long.class);
    if (activeOwnerCount == null || activeOwnerCount != 1) throw new TenantResolutionException("Tenant schema is not operational for tenant slug '" + tenantSlug + "'.");
  }

  public void assertTenantSchemaReady(String schemaName, String tenantHeaderName, String tenantSlug) {
    assertTenantOperational(schemaName, tenantSlug);
  }

  private boolean schemaExists(String schemaName) {
    Integer schemaCount = jdbcTemplate.queryForObject(
        """
            SELECT COUNT(*)
            FROM information_schema.schemata
            WHERE schema_name = ?
            """,
        Integer.class,
        schemaName);

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
        tableName);

    return tableCount != null && tableCount > 0;
  }
}
