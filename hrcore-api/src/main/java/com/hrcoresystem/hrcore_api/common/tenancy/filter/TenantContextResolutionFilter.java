package com.hrcoresystem.hrcore_api.common.tenancy.filter;

import com.hrcoresystem.hrcore_api.common.tenancy.TenancyProperties;
import com.hrcoresystem.hrcore_api.common.tenancy.context.*;
import com.hrcoresystem.hrcore_api.common.tenancy.resolver.TenantResolver;
import com.hrcoresystem.hrcore_api.common.tenancy.validation.TenantSchemaReadinessService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class TenantContextResolutionFilter extends OncePerRequestFilter {
  private final TenancyProperties props;
  private final TenantResolver resolver;
  private final TenantSchemaReadinessService readiness;

  public TenantContextResolutionFilter(TenancyProperties props, TenantResolver resolver,
      TenantSchemaReadinessService readiness) {
    this.props = props;
    this.resolver = resolver;
    this.readiness = readiness;
  }

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest req, @NonNull HttpServletResponse res,
      @NonNull FilterChain chain) throws ServletException, IOException {
    try {
      if (props.usesPublicSchema(req.getRequestURI()))
        TenantContextHolder.set(new TenantContext(null, props.getPublicSchema(), true));
      else {
        TenantContext c = resolver.resolve(req);
        readiness.assertSchemaMigrated(c.schemaName(), c.tenantSlug());
        TenantContextHolder.set(c);
        if (req.getHeader("Authorization") != null && !req.getHeader("Authorization").isBlank()) {
          readiness.assertTenantOperational(c.schemaName(), c.tenantSlug());
        }
      }
      chain.doFilter(req, res);
    } finally {
      TenantContextHolder.clear();
    }
  }
}
