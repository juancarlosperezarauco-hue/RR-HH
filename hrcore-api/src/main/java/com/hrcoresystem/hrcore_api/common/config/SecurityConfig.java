package com.hrcoresystem.hrcore_api.common.config;

import com.hrcoresystem.hrcore_api.common.security.http.RestAccessDeniedHandler;
import com.hrcoresystem.hrcore_api.common.security.http.RestAuthenticationEntryPoint;
import com.hrcoresystem.hrcore_api.common.security.jwt.JwtAuthenticationFilter;
import com.hrcoresystem.hrcore_api.common.tenancy.filter.TenantContextResolutionFilter;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  private final TenantContextResolutionFilter tenantContextResolutionFilter;

  private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

  private final RestAccessDeniedHandler restAccessDeniedHandler;

  public SecurityConfig(
      JwtAuthenticationFilter jwtAuthenticationFilter,
      TenantContextResolutionFilter tenantContextResolutionFilter,
      RestAuthenticationEntryPoint restAuthenticationEntryPoint,
      RestAccessDeniedHandler restAccessDeniedHandler) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.tenantContextResolutionFilter = tenantContextResolutionFilter;
    this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
    this.restAccessDeniedHandler = restAccessDeniedHandler;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http) throws Exception {

    http
        .csrf(AbstractHttpConfigurer::disable)

        .cors(Customizer.withDefaults())

        .sessionManagement(session -> session.sessionCreationPolicy(
            SessionCreationPolicy.STATELESS))

        .exceptionHandling(ex -> ex
            .authenticationEntryPoint(
                restAuthenticationEntryPoint)
            .accessDeniedHandler(
                restAccessDeniedHandler))

        .authorizeHttpRequests(auth -> auth

            .requestMatchers(
                PathRequest.toStaticResources()
                    .atCommonLocations())
            .permitAll()

            .requestMatchers(
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/api-docs",
                "/api-docs/**",
                "/actuator/health",
                "/actuator/info",

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
                "/api/auth/activate-account",
                "/api/auth/resend-activation",
                "/api/auth/face/login",

                "/api/platform/auth/login",
                "/api/platform/auth/refresh",

                "/assets/**",
                "/logo.png")
            .permitAll()

            .requestMatchers("/api/**")
            .authenticated()

            .anyRequest()
            .denyAll())

        .formLogin(AbstractHttpConfigurer::disable)

        .httpBasic(AbstractHttpConfigurer::disable)

        // JWT después de resolver el contexto Tenant.
        .addFilterBefore(
            jwtAuthenticationFilter,
            UsernamePasswordAuthenticationFilter.class);

    // El contexto Tenant se resuelve antes de validar JWT.
    http.addFilterBefore(
        tenantContextResolutionFilter,
        JwtAuthenticationFilter.class);

    return http.build();
  }

  // Evitar el registro automático adicional del filtro JWT.
  @Bean
  public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
      JwtAuthenticationFilter filter) {
    FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);

    registration.setEnabled(false);

    return registration;
  }

  // Evitar el registro automático adicional del filtro Tenant.
  @Bean
  public FilterRegistrationBean<TenantContextResolutionFilter> tenantContextFilterRegistration(
      TenantContextResolutionFilter filter) {
    FilterRegistrationBean<TenantContextResolutionFilter> registration = new FilterRegistrationBean<>(filter);

    registration.setEnabled(false);

    return registration;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
