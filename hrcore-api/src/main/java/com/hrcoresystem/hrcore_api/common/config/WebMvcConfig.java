package com.hrcoresystem.hrcore_api.common.config;

import com.hrcoresystem.hrcore_api.common.tenancy.maintenance.TenantMaintenanceInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  private final TenantMaintenanceInterceptor tenantMaintenanceInterceptor;

  public WebMvcConfig(
      TenantMaintenanceInterceptor tenantMaintenanceInterceptor) {
    this.tenantMaintenanceInterceptor = tenantMaintenanceInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(tenantMaintenanceInterceptor)
        .addPathPatterns("/api/**")
        .excludePathPatterns(
            "/api/platform/**",
            "/api/public/ping",
            "/api/public/signup",
            "/api/public/signup/checkout",
            "/api/public/signup-direct",
            "/api/public/plans",
            "/api/public/plans/**",
            "/api/public/checkout-sessions/**",
            "/api/public/stripe/webhook",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/auth/face/login",
            "/api-docs/**",
            "/swagger-ui/**",
            "/actuator/**")
        .order(1);
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(0, new com.hrcoresystem.hrcore_api.common.pagination.LegacyPageableArgumentResolver());
  }
}
