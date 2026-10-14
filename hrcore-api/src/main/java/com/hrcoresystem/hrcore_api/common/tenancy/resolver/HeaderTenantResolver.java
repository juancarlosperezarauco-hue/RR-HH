package com.hrcoresystem.hrcore_api.common.tenancy.resolver;

import com.hrcoresystem.hrcore_api.common.tenancy.TenancyProperties;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContext;
import com.hrcoresystem.hrcore_api.common.tenancy.exception.TenantResolutionException;
import com.hrcoresystem.hrcore_api.common.tenancy.schema.TenantSchemaNamingStrategy;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

@Component
public class HeaderTenantResolver implements TenantResolver {

  private final TenancyProperties tenancyProperties;
  private final TenantSchemaNamingStrategy tenantSchemaNamingStrategy;
  private final JdbcTemplate publicJdbc;

  @Autowired
  public HeaderTenantResolver(
      TenancyProperties tenancyProperties,
      TenantSchemaNamingStrategy tenantSchemaNamingStrategy,
      @Qualifier("targetDataSource") DataSource targetDataSource) {
    this.tenancyProperties = tenancyProperties;
    this.tenantSchemaNamingStrategy = tenantSchemaNamingStrategy;
    this.publicJdbc = new JdbcTemplate(targetDataSource);
  }

  @Override
  public TenantContext resolve(HttpServletRequest request) {

    String requestPath = request.getRequestURI();

    // Las rutas públicas y globales utilizan el esquema public.
    if (tenancyProperties.isPublicPath(requestPath)
        || tenancyProperties.isGlobalPath(requestPath)) {

      return new TenantContext(
          null,
          tenancyProperties.getPublicSchema(),
          true);
    }

    // Obtener el tenant desde el encabezado HTTP.
    String rawTenantSlug = request.getHeader(
        tenancyProperties.getHeaderName());

    if (!StringUtils.hasText(rawTenantSlug)) {
      throw new TenantResolutionException(
          "Required tenant header is missing");
    }

    // Normalizar y validar el slug.
    String normalizedSlug = tenantSchemaNamingStrategy.normalizeSlug(rawTenantSlug);

    if (!normalizedSlug.equals(rawTenantSlug.trim())) {
      throw new TenantResolutionException(
          "Tenant slug is invalid");
    }

    // Verificar que el tenant existe en la plataforma.
    TenantRecord tenant = publicJdbc.query(
        """
            SELECT
                schema_name,
                active,
                status
            FROM public.platform_tenants
            WHERE slug = ?
            """,
        (rs, rowNum) -> new TenantRecord(
            rs.getString("schema_name"),
            rs.getBoolean("active"),
            rs.getString("status")),
        normalizedSlug).stream()
        .findFirst()
        .orElseThrow(() -> new TenantResolutionException(
            "Tenant was not found"));

    // Verificar estado administrativo.
    if (!tenant.active()
        || (tenant.status() != null
            && !"ACTIVE".equalsIgnoreCase(tenant.status()))) {

      throw new TenantResolutionException(
          "Tenant is not active");
    }

    // Obtener el esquema registrado.
    String schemaName = tenant.schemaName();

    String expectedSchema = tenantSchemaNamingStrategy.toSchemaName(normalizedSlug);

    if (!StringUtils.hasText(schemaName)
        || !schemaName.equals(expectedSchema)) {

      throw new TenantResolutionException(
          "Tenant schema does not match its registered slug");
    }

    // El contexto se construye únicamente con datos validados.
    return new TenantContext(
        normalizedSlug,
        schemaName,
        false);
  }

  private record TenantRecord(
      String schemaName,
      boolean active,
      String status) {
  }
}
