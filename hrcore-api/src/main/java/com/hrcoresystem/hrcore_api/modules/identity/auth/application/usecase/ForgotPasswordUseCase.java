package com.hrcoresystem.hrcore_api.modules.identity.auth.application.usecase;

import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContext;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.identity.audit.IdentityAuditPayloads;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto.NotificationPublishRequest;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.config.PasswordResetNotificationProperties;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationCategory;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationPriority;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationType;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.port.NotificationPublisherPort;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.usecase.SendPasswordResetNotificationUseCase;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.PasswordResetNotification;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto.ForgotPasswordRequest;
import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.model.PasswordResetToken;
import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.repository.PasswordResetTokenRepository;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUser;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class ForgotPasswordUseCase {

    private static final Logger log = LoggerFactory.getLogger(ForgotPasswordUseCase.class);

    private final TenantUserRepository tenantUserRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordResetNotificationProperties properties;
    private final SendPasswordResetNotificationUseCase sendPasswordResetNotificationUseCase;
    private final AuditTrailService auditTrailService;
    private final NotificationPublisherPort notificationPublisherPort;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public ForgotPasswordUseCase(
            TenantUserRepository tenantUserRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordResetNotificationProperties properties,
            SendPasswordResetNotificationUseCase sendPasswordResetNotificationUseCase,
            AuditTrailService auditTrailService,
            NotificationPublisherPort notificationPublisherPort,
            ObjectMapper objectMapper
    ) {
        this.tenantUserRepository = tenantUserRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.properties = properties;
        this.sendPasswordResetNotificationUseCase = sendPasswordResetNotificationUseCase;
        this.auditTrailService = auditTrailService;
        this.notificationPublisherPort = notificationPublisherPort;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void execute(ForgotPasswordRequest request) {
        TenantContext tenantContext = TenantContextHolder.getRequired();
        String normalizedEmail = request.email().trim().toLowerCase();

        TenantUser tenantUser = tenantUserRepository.findByEmail(normalizedEmail).orElse(null);

        if (tenantUser == null || !tenantUser.active()) {
            return;
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.getExpirationMinutes(), ChronoUnit.MINUTES);

        passwordResetTokenRepository.invalidateAllByEmail(normalizedEmail, now);

        String token = generateSecureToken();

        PasswordResetToken passwordResetToken = new PasswordResetToken(
                null,
                normalizedEmail,
                token,
                expiresAt,
                false,
                null,
                null
        );

        passwordResetTokenRepository.save(passwordResetToken);

        sendPasswordResetNotificationUseCase.execute(
                new PasswordResetNotification(
                        normalizedEmail,
                        tenantContext.tenantSlug(),
                        token,
                        expiresAt
                )
        );

        ObjectNode data = objectMapper.createObjectNode()
                .put("tenantSlug", tenantContext.tenantSlug())
                .put("email", normalizedEmail)
                .put("expiresAt", expiresAt.toString())
                .put("status", "REQUESTED");

        publishNotificationSafely(new NotificationPublishRequest(
                tenantUser.id(),
                NotificationType.PASSWORD_RESET_REQUESTED,
                NotificationCategory.SECURITY,
                NotificationPriority.HIGH,
                "Password reset requested",
                "A password reset request was created for your account.",
                data,
                null,
                "/security/password-reset",
                expiresAt
        ));

        auditTrailService.recordTenantEvent(
                AuditEventTypes.PASSWORD_RESET_REQUESTED,
                "USER",
                tenantUser.id().toString(),
                IdentityAuditPayloads.of(
                        "operation", "FORGOT_PASSWORD",
                        "email", normalizedEmail,
                        "tenantSlug", tenantContext.tenantSlug(),
                        "expiresAt", expiresAt.toString()
                ),
                IdentityAuditPayloads.of(
                        "resetTokenState", "NONE"
                ),
                IdentityAuditPayloads.of(
                        "resetTokenState", "REQUESTED",
                        "expiresAt", expiresAt.toString()
                )
        );
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void publishNotificationSafely(NotificationPublishRequest request) {
        try {
            notificationPublisherPort.publish(request);
        } catch (Exception exception) {
            log.warn("Unable to publish password reset notification: {}", exception.getMessage());
        }
    }
}
