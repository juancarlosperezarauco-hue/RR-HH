package com.hrcoresystem.hrcore_api.modules.identity.auth.application.usecase;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto.NotificationPublishRequest;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationCategory;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationPriority;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationType;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.port.NotificationPublisherPort;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.identity.audit.IdentityAuditPayloads;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto.ResetPasswordRequest;
import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.exception.InvalidPasswordResetTokenException;
import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.model.PasswordResetToken;
import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.repository.PasswordResetTokenRepository;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUser;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUserStatus;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;

@Service
public class ResetPasswordUseCase {

    private static final Logger log = LoggerFactory.getLogger(ResetPasswordUseCase.class);

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final TenantUserRepository tenantUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditTrailService auditTrailService;
    private final NotificationPublisherPort notificationPublisherPort;
    private final ObjectMapper objectMapper;

    public ResetPasswordUseCase(
            PasswordResetTokenRepository passwordResetTokenRepository,
            TenantUserRepository tenantUserRepository,
            PasswordEncoder passwordEncoder,
            AuditTrailService auditTrailService,
            NotificationPublisherPort notificationPublisherPort,
            ObjectMapper objectMapper
    ) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.tenantUserRepository = tenantUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditTrailService = auditTrailService;
        this.notificationPublisherPort = notificationPublisherPort;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void execute(ResetPasswordRequest request) {
        PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByToken(request.token().trim())
                .orElseThrow(() -> new InvalidPasswordResetTokenException("Password reset token is invalid"));

        if (passwordResetToken.used()) {
            throw new InvalidPasswordResetTokenException("Password reset token has already been used");
        }

        if (passwordResetToken.expiresAt().isBefore(Instant.now())) {
            throw new InvalidPasswordResetTokenException("Password reset token has expired");
        }

        TenantUser tenantUser = tenantUserRepository.findByEmail(passwordResetToken.email())
                .orElseThrow(() -> new BusinessException("User associated with password reset token was not found"));

        if (tenantUser.status() == TenantUserStatus.PENDING || !tenantUser.active()) {
            if (tenantUser.status() == TenantUserStatus.PENDING) {
                throw new BusinessException("Tu cuenta aún no está activada. Revisa tu correo.");
            }
            throw new BusinessException("User is inactive");
        }

        TenantUser updatedUser = new TenantUser(
                tenantUser.id(),
                tenantUser.email(),
                passwordEncoder.encode(request.newPassword()),
                tenantUser.firstName(),
                tenantUser.lastName(),
                tenantUser.active(),
                tenantUser.status(),
                tenantUser.createdAt(),
                tenantUser.updatedAt()
        );

        tenantUserRepository.save(updatedUser);

        Instant now = Instant.now();
        passwordResetTokenRepository.markUsed(passwordResetToken.token(), now);
        passwordResetTokenRepository.invalidateAllByEmail(passwordResetToken.email(), now);

        ObjectNode data = objectMapper.createObjectNode()
                .put("email", tenantUser.email())
                .put("status", "COMPLETED")
                .put("completedAt", now.toString());

        publishNotificationSafely(new NotificationPublishRequest(
                tenantUser.id(),
                NotificationType.PASSWORD_RESET_COMPLETED,
                NotificationCategory.SECURITY,
                NotificationPriority.HIGH,
                "Password reset completed",
                "Your password was reset successfully.",
                data,
                null,
                "/security/password-reset",
                now.plusSeconds(3600)
        ));

        auditTrailService.recordTenantEvent(
                AuditEventTypes.PASSWORD_RESET_COMPLETED,
                "USER",
                tenantUser.id().toString(),
                IdentityAuditPayloads.of(
                        "operation", "RESET_PASSWORD",
                        "email", tenantUser.email()
                ),
                IdentityAuditPayloads.of(
                        "resetTokenState", "ISSUED",
                        "used", false
                ),
                IdentityAuditPayloads.of(
                        "resetTokenState", "USED",
                        "used", true,
                        "completedAt", now.toString()
                )
        );
    }

    private void publishNotificationSafely(NotificationPublishRequest request) {
        try {
            notificationPublisherPort.publish(request);
        } catch (Exception exception) {
            log.warn("Unable to publish password reset completion notification: {}", exception.getMessage());
        }
    }
}
