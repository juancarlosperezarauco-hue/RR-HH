package com.hrcoresystem.hrcore_api.modules.identity.auth.application.usecase;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.identity.audit.IdentityAuditPayloads;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto.NotificationPublishRequest;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationCategory;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationPriority;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationType;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.port.NotificationPublisherPort;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto.ChangePasswordRequest;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUser;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;

@Service
public class ChangePasswordUseCase {

    private static final Logger log = LoggerFactory.getLogger(ChangePasswordUseCase.class);

    private final SecurityContextFacade securityContextFacade;
    private final TenantUserRepository tenantUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditTrailService auditTrailService;
    private final NotificationPublisherPort notificationPublisherPort;
    private final ObjectMapper objectMapper;

    public ChangePasswordUseCase(
            SecurityContextFacade securityContextFacade,
            TenantUserRepository tenantUserRepository,
            PasswordEncoder passwordEncoder,
            AuditTrailService auditTrailService,
            NotificationPublisherPort notificationPublisherPort,
            ObjectMapper objectMapper
    ) {
        this.securityContextFacade = securityContextFacade;
        this.tenantUserRepository = tenantUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditTrailService = auditTrailService;
        this.notificationPublisherPort = notificationPublisherPort;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void execute(ChangePasswordRequest request) {
        String currentSubject = securityContextFacade.getCurrentSubject();

        TenantUser tenantUser = tenantUserRepository.findById(parseSubjectAsUserId(currentSubject))
                .orElseThrow(() -> new BusinessException("Authenticated user was not found"));

        if (!passwordEncoder.matches(request.currentPassword(), tenantUser.passwordHash())) {
            throw new BusinessException("Current password is incorrect");
        }

        if (request.currentPassword().equals(request.newPassword())) {
            throw new BusinessException("New password must be different from current password");
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

        Instant changedAt = Instant.now();
        ObjectNode data = objectMapper.createObjectNode()
                .put("email", tenantUser.email())
                .put("status", "CHANGED")
                .put("changedAt", changedAt.toString());

        publishNotificationSafely(new NotificationPublishRequest(
                tenantUser.id(),
                NotificationType.PASSWORD_CHANGED,
                NotificationCategory.SECURITY,
                NotificationPriority.HIGH,
                "Password changed",
                "Your password was changed successfully.",
                data,
                null,
                "/security/password",
                Instant.now().plusSeconds(3600)
        ));

        auditTrailService.recordTenantEvent(
                AuditEventTypes.PASSWORD_CHANGED,
                "USER",
                tenantUser.id().toString(),
                IdentityAuditPayloads.of(
                        "operation", "CHANGE_PASSWORD",
                        "email", tenantUser.email(),
                        "tenantSlug", securityContextFacade.getCurrentTenantSlug(),
                        "subject", currentSubject
                ),
                IdentityAuditPayloads.of(
                        "passwordState", "CURRENT"
                ),
                IdentityAuditPayloads.of(
                        "passwordState", "UPDATED",
                        "changedAt", changedAt.toString()
                )
        );
    }

    private UUID parseSubjectAsUserId(String subject) {
        try {
            return UUID.fromString(subject.trim());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("Authenticated subject is not a valid user id");
        }
    }

    private void publishNotificationSafely(NotificationPublishRequest request) {
        try {
            notificationPublisherPort.publish(request);
        } catch (Exception exception) {
            log.warn("Unable to publish password change notification: {}", exception.getMessage());
        }
    }
}
