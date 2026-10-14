package com.hrcoresystem.hrcore_api.common.tenancy.resolver;

import com.hrcoresystem.hrcore_api.common.tenancy.TenancyProperties;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContext;
import com.hrcoresystem.hrcore_api.common.tenancy.exception.TenantResolutionException;
import com.hrcoresystem.hrcore_api.common.tenancy.schema.TenantSchemaNamingStrategy;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;

@Component
public class HeaderTenantResolver implements TenantResolver {

  private final TenancyProperties tenancyProperties;
  private final TenantSchemaNamingStrategy tenantSchemaNamingStrategy;
  private final JdbcTemplate publicJdbc;

  public HeaderTenantResolver(
      TenancyProperties tenancyProperties,
      TenantSchemaNamingStrategy tenantSchemaNamingStrategy,
      @Qualifier("targetDataSource") DataSource targetDataSource) {
    this.tenancyProperties = tenancyProperties;
    this.tenantSchemaNamingStrategy = tenantSchemaNamingStrategy;
    this.publicJdbc = new JdbcTemplate(targetDataSource);
  }

  public HeaderTenantResolver(TenancyProperties tenancyProperties, TenantSchemaNamingStrategy tenantSchemaNamingStrategy) {
    this.tenancyProperties = tenancyProperties;
    this.tenantSchemaNamingStrategy = tenantSchemaNamingStrategy;
    this.publicJdbc = null;
  }

  @Override
  public TenantContext resolve(HttpServletRequest request) {
    String requestPath = request.getRequestURI();
    if (tenancyProperties.isPublicPath(requestPath) || tenancyProperties.isGlobalPath(requestPath)) {
      return new TenantContext(
          null,
          tenancyProperties.getPublicSchema(),
          true);
    }

    String rawTenantSlug = request.getHeader(tenancyProperties.getHeaderName());

    if (!StringUtils.hasText(rawTenantSlug)) {
      throw new TenantResolutionException(
          "Required tenant header '" + tenancyProperties.getHeaderName() + "' is missing");
    }

    String normalizedSlug = tenantSchemaNamingStrategy.normalizeSlug(rawTenantSlug);
    if (publicJdbc != null && !normalizedSlug.equals(rawTenantSlug.trim())) {
      throw new TenantResolutionException("Tenant slug is invalid");
    }
    if (publicJdbc == null) {
      return new TenantContext(normalizedSlug, tenantSchemaNamingStrategy.toSchemaName(normalizedSlug), false);
    }
    var tenant = publicJdbc.query("SELECT schema_name, active, status FROM public.platform_tenants WHERE slug = ?",
        (rs, rowNum) -> new TenantRecord(rs.getString("schema_name"), rs.getBoolean("active"), rs.getString("status")), normalizedSlug)
        .stream().findFirst().orElseThrow(() -> new TenantResolutionException("Tenant was not found"));
    if (!tenant.active() || (tenant.status() != null && !"ACTIVE".equalsIgnoreCase(tenant.status()))) {
      throw new TenantResolutionException("Tenant is not active");
    }
    String schemaName = tenant.schemaName();
    if (!StringUtils.hasText(schemaName) || !schemaName.equals(tenantSchemaNamingStrategy.toSchemaName(normalizedSlug))) {
      throw new TenantResolutionException("Tenant schema does not match its registered slug");
    }

    return new TenantContext(normalizedSlug, schemaName, false);
  }

  private record TenantRecord(String schemaName, boolean active, String status) {}
}
