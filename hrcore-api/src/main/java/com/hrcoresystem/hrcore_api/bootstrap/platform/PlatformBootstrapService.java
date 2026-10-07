package com.hrcoresystem.hrcore_api.bootstrap.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class PlatformBootstrapService {

    private static final Logger logger = LoggerFactory.getLogger(PlatformBootstrapService.class);
    private static final Pattern PERMISSION_CODE_PATTERN = Pattern.compile("^[a-z][a-z0-9-]*(?:[.][a-z0-9-]+)*$");
    private static final Pattern MODULE_PATTERN = Pattern.compile("^[a-z][a-z0-9-]*$");

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final SuperadminBootstrapProperties superadminBootstrapProperties;

    public PlatformBootstrapService(
            @Qualifier("targetDataSource") DataSource targetDataSource,
            PasswordEncoder passwordEncoder,
            SuperadminBootstrapProperties superadminBootstrapProperties
    ) {
        this.jdbcTemplate = new JdbcTemplate(targetDataSource);
        this.passwordEncoder = passwordEncoder;
        this.superadminBootstrapProperties = superadminBootstrapProperties;
    }

    public void seedBasePlans() {
        logger.info("Seeding base platform plans with billing metadata...");

        List<PlanSeed> plans = List.of(
                new PlanSeed(
                        "DEMO",
                        "Demo",
                        "Demo trial plan",
                        5,
                        3,
                        "DEMO",
                        10,
                        null,
                        null,
                        envOrDefault("STRIPE_DEFAULT_CURRENCY", "USD"),
                        false,
                        1
                ),
                new PlanSeed(
                        "BASIC",
                        "Basic",
                        "Basic subscription plan",
                        10,
                        5,
                        "PAID",
                        null,
                        envDecimalOrDefault("BASIC_MONTHLY_AMOUNT", "9.99"),
                        envDecimalOrDefault("BASIC_YEARLY_AMOUNT", "99.99"),
                        envOrDefault("STRIPE_DEFAULT_CURRENCY", "USD"),
                        true,
                        2
                ),
                new PlanSeed(
                        "PRO",
                        "Professional",
                        "Professional subscription plan",
                        25,
                        10,
                        "PAID",
                        null,
                        envDecimalOrDefault("PRO_MONTHLY_AMOUNT", "19.99"),
                        envDecimalOrDefault("PRO_YEARLY_AMOUNT", "199.99"),
                        envOrDefault("STRIPE_DEFAULT_CURRENCY", "USD"),
                        true,
                        3
                ),
                new PlanSeed(
                        "ENTERPRISE",
                        "Enterprise",
                        "Enterprise subscription plan",
                        9999,
                        999,
                        "ENTERPRISE",
                        null,
                        null,
                        null,
                        envOrDefault("STRIPE_DEFAULT_CURRENCY", "USD"),
                        true,
                        4
                )
        );

        for (PlanSeed plan : plans) {
            jdbcTemplate.update(
                    """
                    INSERT INTO public.platform_plans (
                        code,
                        name,
                        description,
                        max_users,
                        max_roles,
                        plan_type,
                        trial_days,
                        monthly_amount,
                        yearly_amount,
                        currency,
                        public_visible,
                        sort_order,
                        active,
                        created_at,
                        updated_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, true, NOW(), NOW())
                    ON CONFLICT (code) DO UPDATE SET
                        name = EXCLUDED.name,
                        description = EXCLUDED.description,
                        max_users = EXCLUDED.max_users,
                        max_roles = EXCLUDED.max_roles,
                        plan_type = EXCLUDED.plan_type,
                        trial_days = EXCLUDED.trial_days,
                        monthly_amount = EXCLUDED.monthly_amount,
                        yearly_amount = EXCLUDED.yearly_amount,
                        currency = EXCLUDED.currency,
                        public_visible = EXCLUDED.public_visible,
                        sort_order = EXCLUDED.sort_order,
                        active = true,
                        updated_at = NOW()
                    """,
                    plan.code(),
                    plan.name(),
                    plan.description(),
                    plan.maxUsers(),
                    plan.maxRoles(),
                    plan.planType(),
                    plan.trialDays(),
                    plan.monthlyAmount(),
                    plan.yearlyAmount(),
                    plan.currency(),
                    plan.publicVisible(),
                    plan.sortOrder()
            );
        }

        logger.info("Base platform plans seeded successfully.");
    }

    public void seedBaseSystemPermissions() {
        logger.info("Seeding base system permissions...");

        List<PermissionSeed> permissions = List.of(
                // IDENTITY / ACCESS PERMISSIONS
                new PermissionSeed("access.permissions.read", "access", "Read system permissions"),
                new PermissionSeed("access.roles.read", "access", "Read tenant roles"),
                new PermissionSeed("access.roles.create", "access", "Create tenant roles"),
                new PermissionSeed("access.roles.detail", "access", "Read tenant role details"),
                new PermissionSeed("access.roles.update", "access", "Update tenant roles"),
                new PermissionSeed("access.roles.activate", "access", "Activate tenant roles"),
                new PermissionSeed("access.roles.deactivate", "access", "Deactivate tenant roles"),
                new PermissionSeed("access.users.roles.read", "access", "Read user role assignments"),
                new PermissionSeed("access.users.roles.assign", "access", "Assign roles to users"),
                new PermissionSeed("users.list", "users", "List tenant users"),
                new PermissionSeed("users.create", "users", "Create tenant users"),
                new PermissionSeed("users.detail", "users", "Read tenant user details"),
                new PermissionSeed("users.update", "users", "Update tenant users"),
                new PermissionSeed("users.activate", "users", "Activate tenant users"),
                new PermissionSeed("users.deactivate", "users", "Deactivate tenant users"),
                // GOVERNANCE PERMISSIONS
                new PermissionSeed("audit.events.read", "audit", "Read tenant audit events"),
                new PermissionSeed("notifications.templates.read", "notifications", "Read notification templates"),
                new PermissionSeed("notifications.templates.detail", "notifications", "Read notification template details"),
                new PermissionSeed("notifications.deliveries.read", "notifications", "Read notification deliveries"),

                // TENANT SETTINGS PERMISSIONS
                new PermissionSeed("tenant-settings.read", "tenantsettings", "Read tenant settings"),
                new PermissionSeed("tenant-settings.update", "tenantsettings", "Update tenant settings")
        );

        seedPermissions(permissions);

        logger.info("Base system permissions seeded successfully.");
    }

    public void seedBaseAccountPermissions() {
        logger.info("Seeding base accounts permissions...");

        List<PermissionSeed> permissions = List.of(
                // OWNER_ADMIN PERMISSIONS
                new PermissionSeed("accounts.create", "accounts", "Create tenant accounts"),
                new PermissionSeed("accounts.list", "accounts", "List tenant accounts"),
                new PermissionSeed("accounts.view", "accounts", "View tenant account details"),
                new PermissionSeed("accounts.balance.read", "accounts", "Read tenant account balances"),
                new PermissionSeed("accounts.update", "accounts", "Update tenant accounts"),
                new PermissionSeed("accounts.approve", "accounts", "Approve tenant accounts"),
                new PermissionSeed("accounts.activate", "accounts", "Activate tenant accounts"),
                new PermissionSeed("accounts.block", "accounts", "Block tenant accounts"),
                new PermissionSeed("accounts.freeze", "accounts", "Freeze tenant accounts"),
                new PermissionSeed("accounts.close", "accounts", "Close tenant accounts"),
                new PermissionSeed("accounts.transactions.read", "accounts", "Read transactions for a tenant account"),
                // CLIENT PERMISSIONS
                new PermissionSeed("me.accounts.create", "accounts", "Create own accounts"),
                new PermissionSeed("me.accounts.list", "accounts", "List own accounts"),
                new PermissionSeed("me.accounts.view", "accounts", "View own account details"),
                new PermissionSeed("me.accounts.balance.read", "accounts", "Read own account balances"),
                new PermissionSeed("me.accounts.update.alias", "accounts", "Update own account aliases"),
                new PermissionSeed("me.accounts.transactions.read", "accounts", "Read own account transactions")
        );

        seedPermissions(permissions);

        logger.info("Base accounts permissions seeded successfully.");
    }

    public void seedBaseTransactionPermissions() {
        logger.info("Seeding base transactions permissions...");

        List<PermissionSeed> permissions = List.of(
                // OWNER_ADMIN PERMISSIONS
                new PermissionSeed("transactions.read", "transactions", "Read tenant transactions"),
                new PermissionSeed("transactions.detail", "transactions", "Read tenant transaction details"),
                new PermissionSeed("transactions.create.transfer", "transactions", "Create transfer transactions"),
                new PermissionSeed("transactions.create.deposit", "transactions", "Create deposit transactions"),
                new PermissionSeed("transactions.create.withdrawal", "transactions", "Create withdrawal transactions"),
                new PermissionSeed("transactions.create.payment", "transactions", "Create payment transactions"),
                new PermissionSeed("transactions.reverse", "transactions", "Reverse transactions"),
                new PermissionSeed("transactions.refund", "transactions", "Refund transactions"),
                new PermissionSeed("transactions.fee", "transactions", "Create fee transactions"),
                new PermissionSeed("transactions.hold", "transactions", "Hold account funds"),
                new PermissionSeed("transactions.release", "transactions", "Release held funds"),
                new PermissionSeed("transactions.adjust", "transactions", "Adjust transactions"),
                new PermissionSeed("transactions.admin.read", "transactions", "Read admin transaction views"),
                new PermissionSeed("transactions.admin.export", "transactions", "Export transaction data"),
                new PermissionSeed("transactions.qr.create", "transactions", "Create QR transaction intents"),
                new PermissionSeed("transactions.qr.confirm", "transactions", "Confirm QR transaction intents"),
                // CLIENT PERMISSIONS
                new PermissionSeed("me.transactions.read", "transactions", "Read own transactions"),
                new PermissionSeed("me.transactions.detail", "transactions", "Read own transaction details"),
                new PermissionSeed("me.transactions.transfer", "transactions", "Create own transfer transactions"),
                new PermissionSeed("me.transactions.deposit", "transactions", "Create own deposit transactions"),
                new PermissionSeed("me.transactions.withdrawal", "transactions", "Create own withdrawal transactions"),
                new PermissionSeed("me.transactions.payment", "transactions", "Create own payment transactions"),
                new PermissionSeed("me.transactions.hold", "transactions", "Hold own account funds"),
                new PermissionSeed("me.transactions.release", "transactions", "Release own held funds"),
                new PermissionSeed("me.transactions.qr.create", "transactions", "Create own QR transaction intents"),
                new PermissionSeed("me.transactions.qr.read", "transactions", "Read own QR transaction intents"),
                new PermissionSeed("me.transactions.qr.cancel", "transactions", "Cancel own QR transaction intents"),
                new PermissionSeed("me.transactions.qr.confirm", "transactions", "Confirm own QR transaction intents")
        );

        seedPermissions(permissions);

        logger.info("Base transactions permissions seeded successfully.");
    }

    public void seedBaseLimitPermissions() {
        logger.info("Seeding base limits permissions...");

        List<PermissionSeed> permissions = List.of(
                // OWNER_ADMIN PERMISSIONS
                new PermissionSeed("limits.read", "limits", "Read limit rules"),
                new PermissionSeed("limits.detail", "limits", "Read limit rule details"),
                new PermissionSeed("limits.create", "limits", "Create limit rules"),
                new PermissionSeed("limits.update", "limits", "Update limit rules"),
                new PermissionSeed("limits.delete", "limits", "Delete limit rules"),
                new PermissionSeed("limits.evaluate", "limits", "Evaluate limit rules")
        );

        seedPermissions(permissions);

        logger.info("Base limits permissions seeded successfully.");
    }

    public void seedBaseLoanPermissions() {
        logger.info("Seeding base loans permissions...");

        List<PermissionSeed> permissions = List.of(
                // OWNER_ADMIN PERMISSIONS
                new PermissionSeed("loans.request", "loans", "Request (open) tenant loans"),
                new PermissionSeed("loans.list", "loans", "List tenant loans"),
                new PermissionSeed("loans.view", "loans", "View tenant loan details and schedule"),
                new PermissionSeed("loans.approve", "loans", "Approve tenant loans"),
                new PermissionSeed("loans.reject", "loans", "Reject tenant loans"),
                new PermissionSeed("loans.disburse", "loans", "Disburse approved tenant loans"),
                new PermissionSeed("loans.repay", "loans", "Record repayments for tenant loans"),
                // CLIENT PERMISSIONS
                new PermissionSeed("me.loans.request", "loans", "Request own loans"),
                new PermissionSeed("me.loans.list", "loans", "List own loans"),
                new PermissionSeed("me.loans.view", "loans", "View own loan details and schedule"),
                new PermissionSeed("me.loans.pay", "loans", "Record repayments for own loans")
        );

        seedPermissions(permissions);

        logger.info("Base loans permissions seeded successfully.");
    }

    public void seedBaseAccountingPermissions() {
        logger.info("Seeding base accounting permissions...");

        List<PermissionSeed> permissions = List.of(
                // OWNER_ADMIN PERMISSIONS
                new PermissionSeed("accounting.journal.read", "accounting", "Read journal entries"),
                new PermissionSeed("accounting.journal.detail", "accounting", "Read journal entry details"),
                new PermissionSeed("accounting.periods.read", "accounting", "Read accounting periods"),
                new PermissionSeed("accounting.periods.create", "accounting", "Create accounting periods"),
                new PermissionSeed("accounting.periods.close", "accounting", "Close accounting periods")
        );

        seedPermissions(permissions);

        logger.info("Base accounting permissions seeded successfully.");
    }

    public void seedBaseFxPermissions() {
        logger.info("Seeding base fx permissions...");

        List<PermissionSeed> permissions = List.of(
                // OWNER_ADMIN PERMISSIONS
                new PermissionSeed("fx.rates.read", "fx", "Read exchange rates"),
                new PermissionSeed("fx.rates.detail", "fx", "Read exchange rate details"),
                new PermissionSeed("fx.rates.create", "fx", "Create exchange rates"),
                new PermissionSeed("fx.rates.update", "fx", "Update exchange rates"),
                new PermissionSeed("fx.rates.delete", "fx", "Delete exchange rates"),
                new PermissionSeed("fx.fees.read", "fx", "Read operation fees"),
                new PermissionSeed("fx.fees.detail", "fx", "Read operation fee details"),
                new PermissionSeed("fx.fees.create", "fx", "Create operation fees"),
                new PermissionSeed("fx.fees.update", "fx", "Update operation fees"),
                new PermissionSeed("fx.fees.delete", "fx", "Delete operation fees")
        );

        seedPermissions(permissions);

        logger.info("Base fx permissions seeded successfully.");
    }

    public void seedBaseBackupPermissions() {
        logger.info("Seeding base backup permissions...");

        List<PermissionSeed> permissions = List.of(
                new PermissionSeed("backups.create", "backups", "Create tenant backups"),
                new PermissionSeed("backups.list", "backups", "List tenant backups"),
                new PermissionSeed("backups.detail", "backups", "Read tenant backup details"),
                new PermissionSeed("backups.download", "backups", "Download tenant backups"),
                new PermissionSeed("backups.restore", "backups", "Restore tenant backups")
        );

        seedPermissions(permissions);

        logger.info("Base backup permissions seeded successfully.");
    }

    public void seedBaseServicePermissions() {
        logger.info("Seeding base service permissions...");

        List<PermissionSeed> permissions = List.of(
                new PermissionSeed("me.service-providers.read", "services", "Read own service providers"),
                new PermissionSeed("me.service-enrollments.read", "services", "Read own service enrollments"),
                new PermissionSeed("me.service-enrollments.create", "services", "Create own service enrollments"),
                new PermissionSeed("me.service-enrollments.update", "services", "Update own service enrollments"),
                new PermissionSeed("me.service-enrollments.delete", "services", "Delete own service enrollments"),
                new PermissionSeed("me.service-bills.query", "services", "Query own service bills"),
                new PermissionSeed("me.service-payments.create", "services", "Create own service payments"),
                new PermissionSeed("me.service-payments.read", "services", "Read own service payments"),
                new PermissionSeed("me.service-payments.detail", "services", "Read own service payment details"),
                new PermissionSeed("service-providers.read", "services", "Read service providers"),
                new PermissionSeed("service-bills.query", "services", "Query service bills"),
                new PermissionSeed("service-payments.create", "services", "Create service payments"),
                new PermissionSeed("service-payments.read", "services", "Read service payments"),
                new PermissionSeed("service-payments.detail", "services", "Read service payment details"),
                new PermissionSeed("service-enrollments.read", "services", "Read service enrollments")
        );

        seedPermissions(permissions);

        logger.info("Base service permissions seeded successfully.");
    }

    public void seedBaseBillingPermissions() {
        logger.info("Seeding base billing permissions...");

        seedPermissions(List.of(
                new PermissionSeed("billing.subscription.manage", "billing", "Manage tenant subscriptions")
        ));

        logger.info("Base billing permissions seeded successfully.");
    }

    public void seedServicePermissionsForRegisteredTenants() {
        logger.info("Assigning service permissions to registered tenant roles...");

        List<TenantSchemaSeed> tenants = jdbcTemplate.query(
                """
                SELECT slug, schema_name
                FROM public.platform_tenants
                WHERE active = true
                  AND schema_name IS NOT NULL
                  AND schema_name <> ''
                ORDER BY slug ASC
                """,
                (rs, rowNum) -> new TenantSchemaSeed(rs.getString("slug"), rs.getString("schema_name"))
        );

        List<String> tenantCodes = List.of(
                "service-providers.read",
                "service-bills.query",
                "service-payments.create",
                "service-payments.read",
                "service-payments.detail",
                "service-enrollments.read"
        );

        List<String> clientCodes = List.of(
                "me.service-providers.read",
                "me.service-enrollments.read",
                "me.service-enrollments.create",
                "me.service-enrollments.update",
                "me.service-enrollments.delete",
                "me.service-bills.query",
                "me.service-payments.create",
                "me.service-payments.read",
                "me.service-payments.detail"
        );

        for (TenantSchemaSeed tenant : tenants) {
            validateSchemaName(tenant.schemaName());

            seedTenantRolePermissions(tenant.schemaName(), "OWNER_ADMIN", tenantCodes);
            seedTenantRolePermissions(tenant.schemaName(), "OWNER_ADMIN", clientCodes);
            seedTenantRolePermissions(tenant.schemaName(), "ADMIN", tenantCodes);
            seedTenantRolePermissions(tenant.schemaName(), "ADMIN", clientCodes);
            seedTenantRolePermissions(tenant.schemaName(), "USER", clientCodes);
        }

        logger.info("Service permissions assigned to registered tenants successfully.");
    }

    public void seedBillingPermissionsForRegisteredTenants() {
        logger.info("Assigning billing permissions to registered tenant roles...");

        List<TenantSchemaSeed> tenants = jdbcTemplate.query(
                """
                SELECT slug, schema_name
                FROM public.platform_tenants
                WHERE active = true
                  AND schema_name IS NOT NULL
                  AND schema_name <> ''
                ORDER BY slug ASC
                """,
                (rs, rowNum) -> new TenantSchemaSeed(rs.getString("slug"), rs.getString("schema_name"))
        );

        for (TenantSchemaSeed tenant : tenants) {
            validateSchemaName(tenant.schemaName());
            seedTenantRolePermissions(
                    tenant.schemaName(),
                    "OWNER_ADMIN",
                    List.of("billing.subscription.manage")
            );
        }

        logger.info("Billing permissions assigned to registered tenants successfully.");
    }

    public void seedInitialPlatformSuperadmin() {
        logger.info("Seeding initial platform superadmin...");

        String email = superadminBootstrapProperties.getEmail().trim().toLowerCase();
        String passwordHash = passwordEncoder.encode(superadminBootstrapProperties.getPassword());

        jdbcTemplate.update(
                """
                INSERT INTO public.platform_superadmins (
                    email, password_hash, first_name, last_name, active, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, true, NOW(), NOW())
                ON CONFLICT (email) DO UPDATE SET
                    password_hash = EXCLUDED.password_hash,
                    first_name = EXCLUDED.first_name,
                    last_name = EXCLUDED.last_name,
                    active = true,
                    updated_at = NOW()
                """,
                email,
                passwordHash,
                superadminBootstrapProperties.getFirstName(),
                superadminBootstrapProperties.getLastName()
        );

        logger.info("Initial platform superadmin seeded successfully for email '{}'.", email);
    }

    private record PlanSeed(
            String code,
            String name,
            String description,
            int maxUsers,
            int maxRoles,
            String planType,
            Integer trialDays,
            BigDecimal monthlyAmount,
            BigDecimal yearlyAmount,
            String currency,
            boolean publicVisible,
            int sortOrder
            ) {

    }

    private record PermissionSeed(
            String code,
            String module,
            String description
            ) {

    }

    private void seedPermissions(List<PermissionSeed> permissions) {
        validatePermissionSeeds(permissions);

        for (PermissionSeed permission : permissions) {
            jdbcTemplate.update(
                    """
                    INSERT INTO public.system_permissions (
                        code, module, description, active, created_at
                    )
                    VALUES (?, ?, ?, true, NOW())
                    ON CONFLICT (code) DO UPDATE SET
                        module = EXCLUDED.module,
                        description = EXCLUDED.description,
                        active = true
                    """,
                    permission.code(),
                    permission.module(),
                    permission.description()
            );
        }
    }

    private void validatePermissionSeeds(List<PermissionSeed> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            throw new IllegalArgumentException("Permission seed list must not be empty");
        }

        Set<String> uniqueCodes = permissions.stream()
                .map(PermissionSeed::code)
                .map(this::normalizeRequiredText)
                .collect(Collectors.toSet());

        if (uniqueCodes.size() != permissions.size()) {
            throw new IllegalArgumentException("Permission seed codes must be unique");
        }

        for (PermissionSeed permission : permissions) {
            String code = normalizeRequiredText(permission.code());
            String module = normalizeRequiredText(permission.module());
            String description = normalizeRequiredText(permission.description());

            if (!PERMISSION_CODE_PATTERN.matcher(code).matches()) {
                throw new IllegalArgumentException("Invalid permission code seed: " + code);
            }

            if (!MODULE_PATTERN.matcher(module).matches()) {
                throw new IllegalArgumentException("Invalid permission module seed: " + module);
            }

            if (description.isBlank()) {
                throw new IllegalArgumentException("Permission seed description must not be blank");
            }
        }
    }

    private String normalizeRequiredText(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Permission seed values must not be blank");
        }

        return value.trim();
    }

    public void seedBackupPermissionsForRegisteredTenants() {
        logger.info("Assigning backup permissions to registered tenant roles...");

        List<TenantSchemaSeed> tenants = jdbcTemplate.query(
                """
            SELECT slug, schema_name
            FROM public.platform_tenants
            WHERE active = true
              AND schema_name IS NOT NULL
              AND schema_name <> ''
            ORDER BY slug ASC
            """,
                (rs, rowNum) -> new TenantSchemaSeed(
                        rs.getString("slug"),
                        rs.getString("schema_name")
                )
        );

        for (TenantSchemaSeed tenant : tenants) {
            validateSchemaName(tenant.schemaName());

            seedTenantRolePermissions(
                    tenant.schemaName(),
                    "OWNER_ADMIN",
                    List.of(
                            "backups.create",
                            "backups.list",
                            "backups.detail",
                            "backups.download",
                            "backups.restore"
                    )
            );

            seedTenantRolePermissions(
                    tenant.schemaName(),
                    "ADMIN",
                    List.of(
                            "backups.create",
                            "backups.list",
                            "backups.detail",
                            "backups.download"
                    )
            );
        }

        logger.info("Backup permissions assigned to registered tenants successfully.");
    }

    private void seedTenantRolePermissions(String schemaName, String roleName, List<String> permissionCodes) {
        for (String permissionCode : permissionCodes) {
            jdbcTemplate.update(
                    """
                INSERT INTO %s.tenant_role_permissions (role_id, permission_code, assigned_at)
                SELECT r.id, ?, NOW()
                FROM %s.tenant_roles r
                JOIN public.system_permissions sp
                    ON sp.code = ? AND sp.active = true
                WHERE r.name = ?
                ON CONFLICT (role_id, permission_code) DO NOTHING
                """.formatted(schemaName, schemaName),
                    permissionCode,
                    permissionCode,
                    roleName
            );
        }
    }

    private void validateSchemaName(String schemaName) {
        if (schemaName == null || schemaName.isBlank()) {
            throw new IllegalArgumentException("Schema name must not be blank");
        }

        if (!schemaName.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName);
        }
    }

    private record TenantSchemaSeed(
            String slug,
            String schemaName
            ) {

    }

    private String envOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return value.trim();
    }

    private String envOrNull(String key) {
        String value = System.getenv(key);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private BigDecimal envDecimalOrDefault(String key, String defaultValue) {
        return new BigDecimal(envOrDefault(key, defaultValue));
    }

}
