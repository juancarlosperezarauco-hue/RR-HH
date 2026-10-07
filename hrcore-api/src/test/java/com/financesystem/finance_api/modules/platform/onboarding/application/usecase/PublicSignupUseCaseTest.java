package com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.usecase;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.dto.PublicSignupRequest;
import com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.dto.PublicSignupResponse;
import com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.service.TenantOwnerAdminProvisioningService;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.model.PlatformPlan;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.repository.PlatformPlanRepository;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.PlatformSubscription;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.PlatformSubscriptionStatus;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.repository.PlatformSubscriptionRepository;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.dto.PlatformTenantResponse;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.usecase.CreateTenantUseCase;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenantStatus;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PublicSignupUseCaseTest {

    @Test
    void shouldCompletePublicSignupSuccessfully() {
        CreateTenantUseCase createTenantUseCase = mock(CreateTenantUseCase.class);
        PlatformTenantRepository platformTenantRepository = mock(PlatformTenantRepository.class);
        PlatformSubscriptionRepository platformSubscriptionRepository = mock(PlatformSubscriptionRepository.class);
        PlatformPlanRepository platformPlanRepository = mock(PlatformPlanRepository.class);
        TenantOwnerAdminProvisioningService tenantOwnerAdminProvisioningService = mock(TenantOwnerAdminProvisioningService.class);
        AuditTrailService auditTrailService = mock(AuditTrailService.class);

        PublicSignupUseCase useCase = new PublicSignupUseCase(
                createTenantUseCase,
                platformTenantRepository,
                platformSubscriptionRepository,
                platformPlanRepository,
                tenantOwnerAdminProvisioningService,
                auditTrailService
        );

        UUID tenantId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(864000);

        PublicSignupRequest request = new PublicSignupRequest(
                "FinanCruz Ltda",
                "financruz",
                "admin@financruz.com",
                "Password123!",
                "Carlos",
                "Rojas"
        );

        PlatformTenantResponse createdTenantResponse = new PlatformTenantResponse(
                tenantId,
                "FinanCruz Ltda",
                "financruz",
                "tenant_financruz",
                "ACTIVE",
                planId,
                null,
                true,
                now,
                now
        );

        PlatformTenant createdTenant = new PlatformTenant(
                tenantId,
                "FinanCruz Ltda",
                "financruz",
                "tenant_financruz",
                PlatformTenantStatus.ACTIVE,
                planId,
                true,
                now,
                now
        );

        PlatformSubscription currentSubscription = new PlatformSubscription(
                subscriptionId,
                tenantId,
                planId,
                PlatformSubscriptionStatus.TRIAL,
                true,
                true,
                now,
                expiresAt,
                now,
                now
        );

        PlatformPlan demoPlan = new PlatformPlan(
                planId,
                "DEMO",
                "Demo",
                "Demo trial plan",
                2,
                2,
                "DEMO",
                10,
                true,
                now,
                now
        );

        when(createTenantUseCase.execute(any())).thenReturn(createdTenantResponse);
        when(platformTenantRepository.findById(tenantId)).thenReturn(Optional.of(createdTenant));
        when(platformSubscriptionRepository.findCurrentByTenantId(tenantId)).thenReturn(Optional.of(currentSubscription));
        when(platformPlanRepository.findById(planId)).thenReturn(Optional.of(demoPlan));

        PublicSignupResponse response = useCase.execute(request);

        assertEquals(tenantId, response.tenantId());
        assertEquals("financruz", response.tenantSlug());
        assertEquals("admin@financruz.com", response.adminEmail());
        assertEquals("OWNER_ADMIN", response.initialRole());
        assertEquals("DEMO", response.currentPlanCode());
        assertEquals("TRIAL", response.subscriptionStatus());
        assertEquals(expiresAt, response.trialExpiresAt());
        assertTrue(response.loginHint().contains("financruz"));
        verify(tenantOwnerAdminProvisioningService).provisionOwnerAdmin(
                "tenant_financruz",
                "financruz",
                "admin@financruz.com",
                "Password123!",
                "Carlos",
                "Rojas"
        );
        verify(auditTrailService).recordPlatformEvent(anyString(), anyString(), anyString(), any(), any(), any());
    }
}
