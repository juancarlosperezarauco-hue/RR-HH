package com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.platform.audit.PlatformAuditPayloads;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.PlatformSubscription;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.PlatformSubscriptionStatus;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.repository.PlatformSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class PlatformSubscriptionLifecycleService {

    private final PlatformSubscriptionRepository platformSubscriptionRepository;
    private final AuditTrailService auditTrailService;

    public PlatformSubscriptionLifecycleService(
            PlatformSubscriptionRepository platformSubscriptionRepository,
            AuditTrailService auditTrailService
    ) {
        this.platformSubscriptionRepository = platformSubscriptionRepository;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public void refreshExpiredSubscriptions() {
        List<PlatformSubscription> subscriptions = platformSubscriptionRepository.findAll();

        for (PlatformSubscription subscription : subscriptions) {
            if (!subscription.currentSubscription()) {
                continue;
            }

            if (subscription.expiresAt() == null) {
                continue;
            }

            if (subscription.status() == PlatformSubscriptionStatus.EXPIRED
                    || subscription.status() == PlatformSubscriptionStatus.CANCELLED) {
                continue;
            }

            if (subscription.expiresAt().isBefore(Instant.now())) {
                PlatformSubscription expired = new PlatformSubscription(
                        subscription.id(),
                        subscription.tenantId(),
                        subscription.planId(),
                        PlatformSubscriptionStatus.EXPIRED,
                        subscription.trial(),
                        subscription.currentSubscription(),
                        subscription.startedAt(),
                        subscription.expiresAt(),
                        subscription.createdAt(),
                        subscription.updatedAt()
                );

                platformSubscriptionRepository.save(expired);

                auditTrailService.recordPlatformEvent(
                        AuditEventTypes.SUBSCRIPTION_EXPIRED,
                        "TENANT_SUBSCRIPTION",
                        expired.id().toString(),
                        PlatformAuditPayloads.details(
                                "tenantId", expired.tenantId(),
                                "planId", expired.planId(),
                                "status", expired.status().name(),
                                "expiresAt", expired.expiresAt()
                        ),
                        PlatformAuditPayloads.subscriptionState(subscription),
                        PlatformAuditPayloads.subscriptionState(expired)
                );
            }
        }
    }
}
