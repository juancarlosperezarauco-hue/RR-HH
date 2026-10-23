package com.hrcoresystem.hrcore_api.modules.identity.users.application.usecase;

import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto.NotificationPublishRequest;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationCategory;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationPriority;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationType;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.port.NotificationPublisherPort;
import com.hrcoresystem.hrcore_api.modules.identity.audit.IdentityAuditPayloads;
import com.hrcoresystem.hrcore_api.modules.identity.users.application.dto.TenantUserResponse;
import com.hrcoresystem.hrcore_api.modules.identity.users.application.mapper.TenantUserMapper;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.exception.TenantUserNotFoundException;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUser;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUserStatus;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import com.hrcoresystem.hrcore_api.common.security.authorization.domain.repository.TenantAuthorizationStateRepository;
import com.hrcoresystem.hrcore_api.common.security.session.domain.repository.TenantAuthSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DeactivateTenantUserUseCase {

    private final TenantUserRepository tenantUserRepository;
    private final TenantUserMapper tenantUserMapper;
    private final AuditTrailService auditTrailService;
    private final TenantAuthorizationStateRepository authorizationStateRepository;
    private final TenantAuthSessionRepository sessions;
    private final SecurityContextFacade securityContextFacade;
    private final NotificationPublisherPort notificationPublisher;

    @Autowired
    public DeactivateTenantUserUseCase(
            TenantUserRepository tenantUserRepository,
            TenantUserMapper tenantUserMapper,
            AuditTrailService auditTrailService,
            TenantAuthorizationStateRepository authorizationStateRepository,
            TenantAuthSessionRepository sessions,
            @Autowired(required = false) SecurityContextFacade securityContextFacade,
            @Autowired(required = false) NotificationPublisherPort notificationPublisher
    ) {
        this.tenantUserRepository = tenantUserRepository;
        this.tenantUserMapper = tenantUserMapper;
        this.auditTrailService = auditTrailService;
        this.authorizationStateRepository = authorizationStateRepository;
        this.sessions = sessions;
        this.securityContextFacade = securityContextFacade;
        this.notificationPublisher = notificationPublisher;
    }

    public DeactivateTenantUserUseCase(
            TenantUserRepository tenantUserRepository,
            TenantUserMapper tenantUserMapper,
            AuditTrailService auditTrailService,
            TenantAuthorizationStateRepository authorizationStateRepository,
            TenantAuthSessionRepository sessions
    ) {
        this(tenantUserRepository, tenantUserMapper, auditTrailService, authorizationStateRepository, sessions, null, null);
    }

    public DeactivateTenantUserUseCase(TenantUserRepository tenantUserRepository, TenantUserMapper tenantUserMapper,
            AuditTrailService auditTrailService, TenantAuthorizationStateRepository authorizationStateRepository,
            TenantAuthSessionRepository sessions, SecurityContextFacade securityContextFacade) {
        this(tenantUserRepository, tenantUserMapper, auditTrailService, authorizationStateRepository, sessions, securityContextFacade, null);
    }

    @Transactional
    public TenantUserResponse execute(UUID id) {
        TenantUser existingUser = tenantUserRepository.findById(id)
                .orElseThrow(() -> new TenantUserNotFoundException("Tenant user not found with id: " + id));

        if (id.equals(authorizationStateRepository.ownerUserId())) {
            throw new IllegalStateException("The tenant owner cannot be deactivated");
        }

        UUID currentUserId = securityContextFacade != null ? securityContextFacade.getCurrentUserId() : null;
        if (currentUserId != null && currentUserId.equals(id)) {
            throw new IllegalStateException("No es posible desactivar tu propia cuenta activa de administrador o propietario");
        }

        TenantUser updatedUser = new TenantUser(
                existingUser.id(),
                existingUser.email(),
                existingUser.passwordHash(),
                existingUser.firstName(),
                existingUser.lastName(),
                false,
                TenantUserStatus.INACTIVE,
                existingUser.securityVersion() + 1,
                existingUser.createdAt(),
                existingUser.updatedAt()
        );

        TenantUser savedUser = tenantUserRepository.save(updatedUser);
        sessions.revokeAllByUserId(savedUser.id());

        auditTrailService.recordTenantEvent(
                AuditEventTypes.USER_DEACTIVATED,
                "USER",
                savedUser.id().toString(),
                IdentityAuditPayloads.of(
                        "operation", "DEACTIVATE_USER",
                        "email", savedUser.email(),
                        "active", false
                ),
                IdentityAuditPayloads.userState(
                        existingUser.email(),
                        existingUser.firstName(),
                        existingUser.lastName(),
                        existingUser.active(),
                        existingUser.status().name()
                ),
                IdentityAuditPayloads.userState(
                        savedUser.email(),
                        savedUser.firstName(),
                        savedUser.lastName(),
                        savedUser.active(),
                        savedUser.status().name()
                )
        );

        publishAccessNotification(savedUser.id());

        return tenantUserMapper.toResponse(savedUser);
    }

    private void publishAccessNotification(UUID userId) {
        if (notificationPublisher == null) return;
        try {
            notificationPublisher.publish(new NotificationPublishRequest(
                    userId, NotificationType.ACCOUNT_BLOCKED, NotificationCategory.SECURITY,
                    NotificationPriority.HIGH, "Acceso bloqueado",
                    "Tu acceso al sistema fue bloqueado por un administrador.",
                    null, null, null, null));
        } catch (Exception ignored) {
            // La notificación nunca debe impedir el bloqueo de la cuenta.
        }
    }
}
