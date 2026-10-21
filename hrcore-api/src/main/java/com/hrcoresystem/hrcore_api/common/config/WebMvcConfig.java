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
  private final TenantHrReadinessInterceptor tenantHrReadinessInterceptor;
  private final TenantPlanModuleInterceptor tenantPlanModuleInterceptor;

  public WebMvcConfig(
      TenantMaintenanceInterceptor tenantMaintenanceInterceptor, TenantHrReadinessInterceptor tenantHrReadinessInterceptor,
      TenantPlanModuleInterceptor tenantPlanModuleInterceptor) {
    this.tenantMaintenanceInterceptor = tenantMaintenanceInterceptor;
    this.tenantHrReadinessInterceptor = tenantHrReadinessInterceptor;
    this.tenantPlanModuleInterceptor = tenantPlanModuleInterceptor;
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
            "/api/public/enterprise-inquiries",
            "/api/public/enterprise-inquiries/**",
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
    registry.addInterceptor(tenantHrReadinessInterceptor).addPathPatterns("/api/shifts", "/api/shifts/**", "/api/attendance", "/api/attendance/**").order(2);
    registry.addInterceptor(tenantPlanModuleInterceptor)
        .addPathPatterns("/api/incidents", "/api/incidents/**", "/api/me/incidents", "/api/me/incidents/**",
            "/api/leaves", "/api/leaves/**", "/api/me/leaves", "/api/me/leaves/**", "/api/me/leave-balances", "/api/me/leave-balances/**",
            "/api/shifts", "/api/shifts/**", "/api/me/schedule", "/api/me/schedule/**", "/api/hr/reports", "/api/hr/reports/**", "/api/hr/ai", "/api/hr/ai/**")
            .addPathPatterns("/api/attendance", "/api/attendance/**", "/api/me/attendance", "/api/me/attendance/**")
        .order(3);
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(0, new com.hrcoresystem.hrcore_api.common.pagination.LegacyPageableArgumentResolver());
  }
}
