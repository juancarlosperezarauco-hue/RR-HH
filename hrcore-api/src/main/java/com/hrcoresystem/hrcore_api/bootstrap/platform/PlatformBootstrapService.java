package com.hrcoresystem.hrcore_api.bootstrap.platform;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;

@Service
public class PlatformBootstrapService {
  private final JdbcTemplate jdbcTemplate;
  private final PasswordEncoder passwordEncoder;
  private final SuperadminBootstrapProperties properties;

  public PlatformBootstrapService(@Qualifier("targetDataSource") DataSource ds, PasswordEncoder encoder,
      SuperadminBootstrapProperties properties) {
    this.jdbcTemplate = new JdbcTemplate(ds);
    this.passwordEncoder = encoder;
    this.properties = properties;
  }

  public void seedBasePlans() {
    List<PlanSeed> plans = List.of(
        new PlanSeed("DEMO", "Demo", "Demo trial plan", 5, 3, "DEMO", 10, null, null, false, 1),
        new PlanSeed("BASIC", "Basic", "Basic subscription plan", 10, 5, "PAID", null,
            envDecimalOrDefault("BASIC_MONTHLY_AMOUNT", "9.99"), envDecimalOrDefault("BASIC_YEARLY_AMOUNT", "99.99"),
            true, 2),
        new PlanSeed("PRO", "Professional", "Professional subscription plan", 25, 10, "PAID", null,
            envDecimalOrDefault("PRO_MONTHLY_AMOUNT", "19.99"), envDecimalOrDefault("PRO_YEARLY_AMOUNT", "199.99"),
            true, 3),
        new PlanSeed("ENTERPRISE", "Enterprise", "Enterprise subscription plan", 9999, 999, "ENTERPRISE", null, null,
            null, true, 4));
    for (PlanSeed p : plans)
      jdbcTemplate.update(
          "INSERT INTO public.platform_plans(code,name,description,max_users,max_roles,plan_type,trial_days,monthly_amount,yearly_amount,currency,public_visible,sort_order,active,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?, ?,true,NOW(),NOW()) ON CONFLICT(code) DO NOTHING",
          p.code(), p.name(), p.description(), p.maxUsers(), p.maxRoles(), p.planType(), p.trialDays(),
          p.monthlyAmount(), p.yearlyAmount(), envOrDefault("STRIPE_DEFAULT_CURRENCY", "USD"), p.publicVisible(),
          p.sortOrder());
  }

  public void seedInitialPlatformSuperadmin() {
    String email = properties.getEmail().trim().toLowerCase();
    jdbcTemplate.update(
        "INSERT INTO public.platform_superadmins(email,password_hash,first_name,last_name,active,security_version,created_at,updated_at) VALUES (?,?,?,?,true,1,NOW(),NOW()) ON CONFLICT(email) DO NOTHING",
        email, passwordEncoder.encode(properties.getPassword()), properties.getFirstName(), properties.getLastName());
  }

  /**
   * @deprecated Permission registration is owned by
   *             PermissionCatalogSynchronizer. Kept only for source
   *             compatibility.
   */
  @Deprecated
  public void seedBaseSystemPermissions() {
  }

  private String envOrDefault(String key, String value) {
    String v = System.getenv(key);
    return StringUtils.hasText(v) ? v.trim() : value;
  }

  private BigDecimal envDecimalOrDefault(String key, String value) {
    return new BigDecimal(envOrDefault(key, value));
  }

  private record PlanSeed(String code, String name, String description, int maxUsers, int maxRoles, String planType,
      Integer trialDays, BigDecimal monthlyAmount, BigDecimal yearlyAmount, boolean publicVisible, int sortOrder) {
  }
}
