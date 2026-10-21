package com.hrcoresystem.hrcore_api.common.tenancy.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrcoresystem.hrcore_api.common.response.ApiErrorResponse;
import com.hrcoresystem.hrcore_api.common.tenancy.TenancyProperties;
import com.hrcoresystem.hrcore_api.common.tenancy.context.*;
import com.hrcoresystem.hrcore_api.common.tenancy.exception.TenantResolutionException;
import com.hrcoresystem.hrcore_api.common.tenancy.resolver.TenantResolver;
import com.hrcoresystem.hrcore_api.common.tenancy.validation.TenantSchemaReadinessService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
public class TenantContextResolutionFilter extends OncePerRequestFilter {
  private final TenancyProperties props;
  private final TenantResolver resolver;
  private final TenantSchemaReadinessService readiness;
  private final ObjectMapper objectMapper;

  public TenantContextResolutionFilter(TenancyProperties props, TenantResolver resolver,
      TenantSchemaReadinessService readiness, ObjectMapper objectMapper) {
    this.props = props;
    this.resolver = resolver;
    this.readiness = readiness;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest req, @NonNull HttpServletResponse res,
      @NonNull FilterChain chain) throws ServletException, IOException {
    try {
      if (props.usesPublicSchema(req.getRequestURI())) {
        TenantContextHolder.set(new TenantContext(null, props.getPublicSchema(), true));
      } else {
        TenantContext c = resolver.resolve(req);
        readiness.assertSchemaMigrated(c.schemaName(), c.tenantSlug());
        TenantContextHolder.set(c);
        if (req.getHeader("Authorization") != null && !req.getHeader("Authorization").isBlank()) {
          readiness.assertTenantOperational(c.schemaName(), c.tenantSlug());
        }
      }
      chain.doFilter(req, res);
    } catch (TenantResolutionException ex) {
      handleTenantResolutionException(res, ex);
    } finally {
      TenantContextHolder.clear();
    }
  }

  private void handleTenantResolutionException(HttpServletResponse res, TenantResolutionException ex)
      throws IOException {
    String message = ex.getMessage() != null ? ex.getMessage() : "";
    int status = HttpServletResponse.SC_BAD_REQUEST;
    String userMessage = "La configuración del tenant no es válida";
    String guidance = "Verifique el identificador del tenant e inténtelo nuevamente.";

    if (message.contains("not active")) {
      status = HttpServletResponse.SC_FORBIDDEN;
      userMessage = "Tu cuenta de empresa fue desactivada por el momento";
      guidance = "La organización se encuentra desactivada o suspendida. Comuníquese con el superadministrador de la plataforma.";
    } else if (message.contains("was not found") || message.contains("does not match any registered tenant")) {
      status = HttpServletResponse.SC_NOT_FOUND;
      userMessage = "El tenant no existe";
      guidance = "No se encontró la organización indicada.";
    } else if (message.contains("missing")) {
      status = HttpServletResponse.SC_BAD_REQUEST;
      userMessage = "Falta el identificador del tenant";
      guidance = "Debe proporcionar el identificador del tenant.";
    }

    res.setStatus(status);
    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
    res.setCharacterEncoding("UTF-8");

    ApiErrorResponse body = ApiErrorResponse.of(userMessage, List.of(guidance));
    objectMapper.writeValue(res.getOutputStream(), body);
  }
}
