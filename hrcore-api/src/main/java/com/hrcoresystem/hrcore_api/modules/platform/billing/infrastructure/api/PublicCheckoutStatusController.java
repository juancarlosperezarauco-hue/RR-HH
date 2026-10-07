package com.hrcoresystem.hrcore_api.modules.platform.billing.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.dto.PublicCheckoutActivationStatusResponse;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.usecase.GetPublicCheckoutActivationStatusUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/checkout-sessions")
public class PublicCheckoutStatusController {

    private final GetPublicCheckoutActivationStatusUseCase getPublicCheckoutActivationStatusUseCase;

    public PublicCheckoutStatusController(
            GetPublicCheckoutActivationStatusUseCase getPublicCheckoutActivationStatusUseCase
    ) {
        this.getPublicCheckoutActivationStatusUseCase = getPublicCheckoutActivationStatusUseCase;
    }

    @GetMapping("/{stripeSessionId}/status")
    public ApiResponse<PublicCheckoutActivationStatusResponse> status(
            @PathVariable String stripeSessionId
    ) {
        return ApiResponse.success(
                "Checkout activation status retrieved successfully",
                getPublicCheckoutActivationStatusUseCase.execute(stripeSessionId)
        );
    }
}
