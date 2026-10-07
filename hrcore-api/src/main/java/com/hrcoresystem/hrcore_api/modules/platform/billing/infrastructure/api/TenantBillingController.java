package com.hrcoresystem.hrcore_api.modules.platform.billing.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.dto.BillingPlanResponse;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.dto.BillingCheckoutResultResponse;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.dto.BillingStatusResponse;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.dto.CheckoutSessionResponse;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.dto.CheckoutSessionStatusResponse;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.dto.CreateCheckoutSessionRequest;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.dto.CustomerPortalResponse;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.dto.SubscriptionPaymentResponse;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.usecase.CreateTenantCheckoutSessionUseCase;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.usecase.CreateTenantCustomerPortalUseCase;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.usecase.GetTenantBillingStatusUseCase;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.usecase.GetTenantCheckoutResultUseCase;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.usecase.GetTenantCheckoutSessionStatusUseCase;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.usecase.ListTenantAvailableBillingPlansUseCase;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.usecase.ListTenantSubscriptionPaymentsUseCase;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.dto.PlatformSubscriptionResponse;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.usecase.GetCurrentTenantSubscriptionUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/subscription")
@SecurityRequirement(name = "bearerAuth")
public class TenantBillingController {

    private final GetCurrentTenantSubscriptionUseCase getCurrentTenantSubscriptionUseCase;
    private final ListTenantAvailableBillingPlansUseCase listTenantAvailableBillingPlansUseCase;
    private final ListTenantSubscriptionPaymentsUseCase listTenantSubscriptionPaymentsUseCase;
    private final GetTenantCheckoutSessionStatusUseCase getTenantCheckoutSessionStatusUseCase;
    private final CreateTenantCheckoutSessionUseCase createTenantCheckoutSessionUseCase;
    private final CreateTenantCustomerPortalUseCase createTenantCustomerPortalUseCase;
    private final GetTenantBillingStatusUseCase getTenantBillingStatusUseCase;
    private final GetTenantCheckoutResultUseCase getTenantCheckoutResultUseCase;

    public TenantBillingController(
            GetCurrentTenantSubscriptionUseCase getCurrentTenantSubscriptionUseCase,
            ListTenantAvailableBillingPlansUseCase listTenantAvailableBillingPlansUseCase,
            ListTenantSubscriptionPaymentsUseCase listTenantSubscriptionPaymentsUseCase,
            GetTenantCheckoutSessionStatusUseCase getTenantCheckoutSessionStatusUseCase,
            CreateTenantCheckoutSessionUseCase createTenantCheckoutSessionUseCase,
            CreateTenantCustomerPortalUseCase createTenantCustomerPortalUseCase,
            GetTenantBillingStatusUseCase getTenantBillingStatusUseCase,
            GetTenantCheckoutResultUseCase getTenantCheckoutResultUseCase
    ) {
        this.getCurrentTenantSubscriptionUseCase = getCurrentTenantSubscriptionUseCase;
        this.listTenantAvailableBillingPlansUseCase = listTenantAvailableBillingPlansUseCase;
        this.listTenantSubscriptionPaymentsUseCase = listTenantSubscriptionPaymentsUseCase;
        this.getTenantCheckoutSessionStatusUseCase = getTenantCheckoutSessionStatusUseCase;
        this.createTenantCheckoutSessionUseCase = createTenantCheckoutSessionUseCase;
        this.createTenantCustomerPortalUseCase = createTenantCustomerPortalUseCase;
        this.getTenantBillingStatusUseCase = getTenantBillingStatusUseCase;
        this.getTenantCheckoutResultUseCase = getTenantCheckoutResultUseCase;
    }

    @GetMapping("/current")
    @PreAuthorize("hasAuthority('billing.subscription.manage')")
    public ApiResponse<PlatformSubscriptionResponse> currentSubscription() {
        return ApiResponse.success(
                "Current tenant subscription retrieved successfully",
                getCurrentTenantSubscriptionUseCase.execute(TenantContextHolder.getRequired().tenantSlug())
        );
    }

    @GetMapping("/status")
    @PreAuthorize("hasAuthority('billing.subscription.manage')")
    public ApiResponse<BillingStatusResponse> billingStatus() {
        return ApiResponse.success(
                "Billing status retrieved successfully",
                getTenantBillingStatusUseCase.execute()
        );
    }

    @GetMapping("/available-plans")
    @PreAuthorize("hasAuthority('billing.subscription.manage')")
    public ApiResponse<List<BillingPlanResponse>> availablePlans() {
        return ApiResponse.success(
                "Available billing plans retrieved successfully",
                listTenantAvailableBillingPlansUseCase.execute()
        );
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasAuthority('billing.subscription.manage')")
    public ApiResponse<CheckoutSessionResponse> createCheckout(
            @Valid @RequestBody CreateCheckoutSessionRequest request
    ) {
        return ApiResponse.success(
                "Stripe checkout session created successfully",
                createTenantCheckoutSessionUseCase.execute(request)
        );
    }

    @GetMapping("/checkout-result")
    @PreAuthorize("hasAuthority('billing.subscription.manage')")
    public ApiResponse<BillingCheckoutResultResponse> checkoutResult(@RequestParam("session_id") String sessionId) {
        return ApiResponse.success(
                "Checkout result retrieved successfully",
                getTenantCheckoutResultUseCase.execute(sessionId)
        );
    }

    @PostMapping("/customer-portal")
    @PreAuthorize("hasAuthority('billing.subscription.manage')")
    public ApiResponse<CustomerPortalResponse> createCustomerPortal() {
        return ApiResponse.success(
                "Stripe customer portal session created successfully",
                createTenantCustomerPortalUseCase.execute()
        );
    }

    @GetMapping("/payments")
    @PreAuthorize("hasAuthority('billing.subscription.manage')")
    public ApiResponse<List<SubscriptionPaymentResponse>> payments() {
        return ApiResponse.success(
                "Subscription payments retrieved successfully",
                listTenantSubscriptionPaymentsUseCase.execute()
        );
    }

    @GetMapping("/checkout-sessions/{id}")
    @PreAuthorize("hasAuthority('billing.subscription.manage')")
    public ApiResponse<CheckoutSessionStatusResponse> checkoutSessionStatus(@PathVariable UUID id) {
        return ApiResponse.success(
                "Checkout session status retrieved successfully",
                getTenantCheckoutSessionStatusUseCase.execute(id)
        );
    }
}
