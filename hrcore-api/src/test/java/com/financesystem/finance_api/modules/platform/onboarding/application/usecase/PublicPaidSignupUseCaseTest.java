package com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.usecase;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.service.StripeCheckoutService;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.service.StripeCustomerService;
import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.model.SubscriptionCheckoutSession;
import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.repository.SubscriptionCheckoutSessionRepository;
import com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.dto.PublicPaidSignupRequest;
import com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.dto.PublicPaidSignupResponse;
import com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.service.TenantOwnerAdminProvisioningService;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.model.PlatformPlan;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.repository.PlatformPlanRepository;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.BillingInterval;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.PlatformSubscription;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.PlatformSubscriptionStatus;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.repository.PlatformSubscriptionRepository;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.dto.PlatformTenantResponse;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.usecase.CreateTenantUseCase;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenantStatus;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PublicPaidSignupUseCaseTest {

    @Test
    void shouldCreateTenantCheckoutForPaidSignup() {
        CreateTenantUseCase createTenantUseCase = mock(CreateTenantUseCase.class);
        PlatformTenantRepository platformTenantRepository = mock(PlatformTenantRepository.class);
        PlatformSubscriptionRepository platformSubscriptionRepository = mock(PlatformSubscriptionRepository.class);
        PlatformPlanRepository platformPlanRepository = mock(PlatformPlanRepository.class);
        TenantOwnerAdminProvisioningService tenantOwnerAdminProvisioningService = mock(TenantOwnerAdminProvisioningService.class);
        StripeCustomerService stripeCustomerService = mock(StripeCustomerService.class);
        StripeCheckoutService stripeCheckoutService = mock(StripeCheckoutService.class);
        SubscriptionCheckoutSessionRepository checkoutSessionRepository = mock(SubscriptionCheckoutSessionRepository.class);
        AuditTrailService auditTrailService = mock(AuditTrailService.class);

        PublicPaidSignupUseCase useCase = new PublicPaidSignupUseCase(
                createTenantUseCase,
                platformTenantRepository,
                platformSubscriptionRepository,
                platformPlanRepository,
                tenantOwnerAdminProvisioningService,
                stripeCustomerService,
                stripeCheckoutService,
                checkoutSessionRepository,
                auditTrailService
        );

        UUID tenantId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        Instant now = Instant.now();

        PublicPaidSignupRequest request = new PublicPaidSignupRequest(
                "FinanCruz Ltda",
                "financruz",
                "admin@financruz.com",
                "Password123!",
                "Carlos",
                "Rojas",
                "PRO",
                "MONTHLY"
        );

        PlatformTenantResponse createdTenantResponse = new PlatformTenantResponse(
                tenantId,
                "FinanCruz Ltda",
                "financruz",
                "tenant_financruz",
                "ACTIVE",
                UUID.randomUUID(),
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
                UUID.randomUUID(),
                null,
                true,
                now,
                now
        );

        PlatformTenant tenantWithCustomer = new PlatformTenant(
                tenantId,
                "FinanCruz Ltda",
                "financruz",
                "tenant_financruz",
                PlatformTenantStatus.ACTIVE,
                UUID.randomUUID(),
                "cus_123",
                true,
                now,
                now
        );

        PlatformSubscription currentSubscription = new PlatformSubscription(
                subscriptionId,
                tenantId,
                UUID.randomUUID(),
                PlatformSubscriptionStatus.TRIAL,
                true,
                true,
                now,
                now.plusSeconds(86400),
                now,
                now
        );

        PlatformPlan selectedPlan = new PlatformPlan(
                planId,
                "PRO",
                "Professional",
                "Paid plan",
                10,
                5,
                "PAID",
                null,
                new BigDecimal("25.00"),
                new BigDecimal("250.00"),
                "BOB",
                true,
                1,
                true,
                now,
                now
        );

        PlatformPlan initialPlan = new PlatformPlan(
                currentSubscription.planId(),
                "DEMO",
                "Demo",
                "Demo plan",
                2,
                2,
                "DEMO",
                10,
                null,
                null,
                "BOB",
                true,
                0,
                true,
                now,
                now
        );

        Session stripeSession = mock(Session.class);

        SubscriptionCheckoutSession savedCheckout = new SubscriptionCheckoutSession(
                UUID.randomUUID(),
                tenantId,
                planId,
                null,
                "admin@financruz.com",
                BillingInterval.MONTHLY,
                "OPEN",
                "cus_123",
                "cs_test_123",
                null,
                null,
                "https://checkout.example",
                null,
                null,
                new BigDecimal("25.00"),
                "BOB",
                null,
                now.plusSeconds(1800),
                now,
                now
        );

        when(createTenantUseCase.execute(any())).thenReturn(createdTenantResponse);
        when(platformTenantRepository.findById(tenantId)).thenReturn(Optional.of(createdTenant));
        when(platformPlanRepository.findByCode("PRO")).thenReturn(Optional.of(selectedPlan));
        when(platformSubscriptionRepository.findCurrentByTenantId(tenantId)).thenReturn(Optional.of(currentSubscription));
        when(platformPlanRepository.findById(currentSubscription.planId())).thenReturn(Optional.of(initialPlan));
        when(stripeCustomerService.ensureStripeCustomer(createdTenant, "admin@financruz.com")).thenReturn(tenantWithCustomer);
        when(stripeCheckoutService.createSubscriptionCheckoutSession(tenantWithCustomer, selectedPlan, BillingInterval.MONTHLY)).thenReturn(stripeSession);
        when(stripeSession.getStatus()).thenReturn("open");
        when(stripeSession.getId()).thenReturn("cs_test_123");
        when(stripeSession.getUrl()).thenReturn("https://checkout.example");
        when(stripeSession.getExpiresAt()).thenReturn(now.plusSeconds(1800).getEpochSecond());
        when(checkoutSessionRepository.save(any())).thenReturn(savedCheckout);

        PublicPaidSignupResponse response = useCase.execute(request);

        assertEquals(tenantId, response.tenantId());
        assertEquals("financruz", response.tenantSlug());
        assertEquals("PRO", response.selectedPlanCode());
        assertEquals("MONTHLY", response.billingInterval());
        assertEquals("cs_test_123", response.checkoutSessionId());
        assertEquals("https://checkout.example", response.checkoutUrl());
        assertEquals("OPEN", response.checkoutStatus());
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
