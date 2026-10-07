package com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.config.AccountActivationNotificationProperties;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.usecase.SendAccountActivationNotificationUseCase;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.AccountActivationNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

@Service
public class TenantOwnerAdminProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(TenantOwnerAdminProvisioningService.class);

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final AccountActivationNotificationProperties activationProperties;
    private final SendAccountActivationNotificationUseCase sendAccountActivationNotificationUseCase;
    private final SecureRandom secureRandom = new SecureRandom();

    public TenantOwnerAdminProvisioningService(
            @Qualifier("targetDataSource") DataSource targetDataSource,
            PasswordEncoder passwordEncoder,
            AccountActivationNotificationProperties activationProperties,
            SendAccountActivationNotificationUseCase sendAccountActivationNotificationUseCase
    ) {
        this.jdbcTemplate = new JdbcTemplate(targetDataSource);
        this.passwordEncoder = passwordEncoder;
        this.activationProperties = activationProperties;
        this.sendAccountActivationNotificationUseCase = sendAccountActivationNotificationUseCase;
    }

    public UUID provisionOwnerAdmin(
            String schemaName,
            String tenantSlug,
            String email,
            String rawPassword,
            String firstName,
            String lastName
    ) {
        return provisionOwnerAdminInternal(schemaName, tenantSlug, email, rawPassword, firstName, lastName, true);
    }

    public UUID provisionOwnerAdminWithoutVerification(
            String schemaName,
            String tenantSlug,
            String email,
            String rawPassword,
            String firstName,
            String lastName
    ) {
        return provisionOwnerAdminInternal(schemaName, tenantSlug, email, rawPassword, firstName, lastName, false);
    }

    private UUID provisionOwnerAdminInternal(
            String schemaName,
            String tenantSlug,
            String email,
            String rawPassword,
            String firstName,
            String lastName,
            boolean issueActivationEmail
    ) {
        validateSchemaName(schemaName);

        String normalizedEmail = email.trim().toLowerCase();
        String normalizedFirstName = firstName.trim();
        String normalizedLastName = lastName.trim();

        Integer existingUsers = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM %s.tenant_users
                WHERE email = ?
                """.formatted(schemaName),
                Integer.class,
                normalizedEmail
        );

        jdbcTemplate.execute("""
                INSERT INTO %s.tenant_roles (name, description, active, created_at)
                VALUES ('OWNER_ADMIN', 'Tenant owner administrator role', true, NOW())
                ON CONFLICT (name) DO UPDATE SET
                    active = true,
                    description = EXCLUDED.description
                """.formatted(schemaName));

        String passwordHash = passwordEncoder.encode(rawPassword);
        UUID userId;

        if (existingUsers != null && existingUsers > 0) {
            ExistingTenantUser existingUser = jdbcTemplate.queryForObject(
                    """
                    SELECT id, status
                    FROM %s.tenant_users
                    WHERE email = ?
                    LIMIT 1
                    """.formatted(schemaName),
                    (rs, rowNum) -> new ExistingTenantUser(
                            rs.getObject("id", UUID.class),
                            rs.getString("status")
                    ),
                    normalizedEmail
            );

            if (existingUser == null || existingUser.id() == null) {
                throw new BusinessException("A tenant user with email '" + normalizedEmail + "' already exists");
            }

            userId = existingUser.id();

            if (issueActivationEmail && "ACTIVE".equalsIgnoreCase(existingUser.status())) {
                throw new BusinessException("A tenant user with email '" + normalizedEmail + "' already exists");
            }

            jdbcTemplate.update(
                    """
                    UPDATE %s.tenant_users
                    SET password_hash = ?, first_name = ?, last_name = ?, active = ?, status = ?, updated_at = NOW()
                    WHERE email = ?
                    """.formatted(schemaName),
                    passwordHash,
                    normalizedFirstName,
                    normalizedLastName,
                    !issueActivationEmail,
                    issueActivationEmail ? "PENDING" : "ACTIVE",
                    normalizedEmail
            );
        } else {
            userId = UUID.randomUUID();

            boolean active = !issueActivationEmail;
            String status = issueActivationEmail ? "PENDING" : "ACTIVE";

            jdbcTemplate.update(
                    """
                    INSERT INTO %s.tenant_users (
                        id, email, password_hash, first_name, last_name, active, status, created_at, updated_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                    """.formatted(schemaName),
                    userId,
                    normalizedEmail,
                    passwordHash,
                    normalizedFirstName,
                    normalizedLastName,
                    active,
                    status
            );
        }

        UUID ownerAdminRoleId = jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM %s.tenant_roles
                WHERE name = 'OWNER_ADMIN'
                LIMIT 1
                """.formatted(schemaName),
                (rs, rowNum) -> rs.getObject("id", UUID.class)
        );

        jdbcTemplate.update(
                """
                INSERT INTO %s.tenant_user_roles (user_id, role_id, assigned_at)
                VALUES (?, ?, NOW())
                ON CONFLICT (user_id, role_id) DO NOTHING
                """.formatted(schemaName),
                userId,
                ownerAdminRoleId
        );

        jdbcTemplate.update(
                """
                INSERT INTO %s.tenant_settings (setting_key, setting_value, created_at, updated_at)
                VALUES ('company.contact_email', ?, NOW(), NOW())
                ON CONFLICT (setting_key) DO UPDATE SET
                    setting_value = EXCLUDED.setting_value,
                    updated_at = NOW()
                """.formatted(schemaName),
                normalizedEmail
        );

        if (issueActivationEmail) {
            issueActivation(schemaName, tenantSlug, normalizedEmail);
        }

        return userId;
    }

    private record ExistingTenantUser(UUID id, String status) {
    }

    private void issueActivation(String schemaName, String tenantSlug, String normalizedEmail) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(activationProperties.getExpirationMinutes(), ChronoUnit.MINUTES);
        String token = generateSecureToken();

        jdbcTemplate.update(
                """
                UPDATE %s.tenant_account_activation_tokens
                SET used = true, used_at = ?
                WHERE email = ? AND used = false
                """.formatted(schemaName),
                OffsetDateTime.ofInstant(now, ZoneOffset.UTC),
                normalizedEmail
        );

        jdbcTemplate.update(
                """
                INSERT INTO %s.tenant_account_activation_tokens (
                    id, email, token, expires_at, used, used_at, created_at
                )
                VALUES (?, ?, ?, ?, false, NULL, NOW())
                """.formatted(schemaName),
                UUID.randomUUID(),
                normalizedEmail,
                token,
                OffsetDateTime.ofInstant(expiresAt, ZoneOffset.UTC)
        );

        try {
            sendAccountActivationNotificationUseCase.execute(
                    new AccountActivationNotification(normalizedEmail, tenantSlug, token, expiresAt)
            );
        } catch (Exception exception) {
            log.warn("Unable to send owner activation email to '{}': {}", normalizedEmail, exception.getMessage());
        }
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void validateSchemaName(String schemaName) {
        if (schemaName == null || schemaName.isBlank()) {
            throw new IllegalArgumentException("Schema name must not be blank");
        }

        if (!schemaName.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Schema name contains invalid characters: " + schemaName);
        }
    }
}
