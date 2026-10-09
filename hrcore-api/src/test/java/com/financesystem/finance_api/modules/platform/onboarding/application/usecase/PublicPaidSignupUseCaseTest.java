package com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.usecase;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.dto.PublicPaidSignupRequest;
import com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.dto.PublicPaidSignupResponse;
import com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.service.TenantOwnerAdminProvisioningService;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.model.PlatformPlan;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.repository.PlatformPlanRepository;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.PlatformSubscription;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.PlatformSubscriptionStatus;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.repository.PlatformSubscriptionRepository;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.dto.CreateTenantRequest;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.dto.PlatformTenantResponse;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.usecase.CreateTenantUseCase;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenantStatus;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PublicPaidSignupUseCaseTest {

    @Test
    void shouldCreateTenantWithSelectedPlanWithoutStripeCheckout() {
        CreateTenantUseCase createTenantUseCase = mock(CreateTenantUseCase.class);
        PlatformTenantRepository platformTenantRepository = mock(PlatformTenantRepository.class);
        PlatformSubscriptionRepository platformSubscriptionRepository = mock(PlatformSubscriptionRepository.class);
        PlatformPlanRepository platformPlanRepository = mock(PlatformPlanRepository.class);
        TenantOwnerAdminProvisioningService provisioningService = mock(TenantOwnerAdminProvisioningService.class);
        AuditTrailService auditTrailService = mock(AuditTrailService.class);

        PublicPaidSignupUseCase useCase = new PublicPaidSignupUseCase(
                createTenantUseCase,
                platformTenantRepository,
                platformSubscriptionRepository,
                platformPlanRepository,
                provisioningService,
                auditTrailService
        );

        UUID tenantId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        Instant now = Instant.now();
        PublicPaidSignupRequest request = new PublicPaidSignupRequest(
                "Talento Humano Ltda",
                "talento-humano",
                "owner@example.com",
                "Password123!",
                "Ana",
                "Rojas",
                "PRO",
                "MONTHLY"
        );
        PlatformPlan selectedPlan = new PlatformPlan(
                planId, "PRO", "Professional", "Paid plan", 25, 10,
                "PAID", null, new BigDecimal("19.99"), new BigDecimal("199.99"),
                "USD", true, 3, true, now, now
        );
        PlatformTenantResponse tenantResponse = new PlatformTenantResponse(
                tenantId, "Talento Humano Ltda", "talento-humano", "tenant_talento_humano",
                "ACTIVE", planId, null, true, now, now
        );
        PlatformTenant tenant = new PlatformTenant(
                tenantId, "Talento Humano Ltda", "talento-humano", "tenant_talento_humano",
                PlatformTenantStatus.ACTIVE, planId, null, true, now, now
        );
        PlatformSubscription subscription = new PlatformSubscription(
                UUID.randomUUID(), tenantId, planId, PlatformSubscriptionStatus.ACTIVE,
                true, false, now, null, now, now
        );

        when(platformPlanRepository.findByCode("PRO")).thenReturn(Optional.of(selectedPlan));
        when(createTenantUseCase.execute(any(CreateTenantRequest.class))).thenReturn(tenantResponse);
        when(platformTenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(platformSubscriptionRepository.findCurrentByTenantId(tenantId)).thenReturn(Optional.of(subscription));
        when(platformPlanRepository.findById(planId)).thenReturn(Optional.of(selectedPlan));

        PublicPaidSignupResponse response = useCase.execute(request);

        assertEquals(tenantId, response.tenantId());
        assertEquals("PRO", response.initialPlanCode());
        assertEquals("PRO", response.selectedPlanCode());
        assertEquals("NOT_REQUIRED", response.checkoutStatus());
        assertNull(response.checkoutSessionId());
        assertNull(response.checkoutUrl());
        verify(createTenantUseCase).execute(argThat(candidate -> "PRO".equals(candidate.planCode())));
        verify(provisioningService).provisionOwnerAdminWithoutVerification(
                "tenant_talento_humano",
                "talento-humano",
                "owner@example.com",
                "Password123!",
                "Ana",
                "Rojas"
        );
        verify(auditTrailService).recordPlatformEvent(anyString(), anyString(), anyString(), any(), any(), any());
    }
}
