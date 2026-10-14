package com.hrcoresystem.hrcore_api.common.config;

import com.hrcoresystem.hrcore_api.common.security.http.RestAccessDeniedHandler;
import com.hrcoresystem.hrcore_api.common.security.http.RestAuthenticationEntryPoint;
import com.hrcoresystem.hrcore_api.common.security.jwt.JwtAuthenticationFilter;
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
import com.hrcoresystem.hrcore_api.common.tenancy.filter.TenantContextResolutionFilter;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
  private final RestAccessDeniedHandler restAccessDeniedHandler;
  private final TenantContextResolutionFilter tenantContextResolutionFilter;

  public SecurityConfig(
      JwtAuthenticationFilter jwtAuthenticationFilter,
      RestAuthenticationEntryPoint restAuthenticationEntryPoint,
      RestAccessDeniedHandler restAccessDeniedHandler, TenantContextResolutionFilter tenantContextResolutionFilter) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
    this.restAccessDeniedHandler = restAccessDeniedHandler;
    this.tenantContextResolutionFilter = tenantContextResolutionFilter;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint(restAuthenticationEntryPoint)
            .accessDeniedHandler(restAccessDeniedHandler))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
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
            .requestMatchers("/api/**").authenticated()
            .anyRequest().denyAll())
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    http.addFilterBefore(tenantContextResolutionFilter, JwtAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
