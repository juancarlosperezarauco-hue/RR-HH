package com.hrcoresystem.hrcore_api.modules.platform.onboarding.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.dto.PublicPaidSignupRequest;
import com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.dto.PublicPaidSignupResponse;
import com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.dto.PublicSignupRequest;
import com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.dto.PublicSignupResponse;
import com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.usecase.PublicPaidSignupUseCase;
import com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.usecase.PublicSignupDirectUseCase;
import com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.usecase.PublicSignupUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
public class PublicOnboardingController {

    private final PublicSignupUseCase publicSignupUseCase;
    private final PublicPaidSignupUseCase publicPaidSignupUseCase;
    private final PublicSignupDirectUseCase publicSignupDirectUseCase;

    public PublicOnboardingController(
            PublicSignupUseCase publicSignupUseCase,
            PublicPaidSignupUseCase publicPaidSignupUseCase,
            PublicSignupDirectUseCase publicSignupDirectUseCase
    ) {
        this.publicSignupUseCase = publicSignupUseCase;
        this.publicPaidSignupUseCase = publicPaidSignupUseCase;
        this.publicSignupDirectUseCase = publicSignupDirectUseCase;
    }

    /**
     * Registro público estándar.
     * Utilizado para crear un tenant DEMO.
     */
    @PostMapping("/signup")
    public ApiResponse<PublicSignupResponse> signup(
            @Valid @RequestBody PublicSignupRequest request
    ) {
        return ApiResponse.success(
                "Public signup completed successfully",
                publicSignupUseCase.execute(request)
        );
    }

    /**
     * Registro público con Stripe Checkout.
     * Crea el tenant en estado pendiente y devuelve la URL de Stripe.
     */
    @PostMapping("/signup/checkout")
    public ApiResponse<PublicPaidSignupResponse> paidSignup(
            @Valid @RequestBody PublicPaidSignupRequest request
    ) {
        return ApiResponse.success(
                "Public paid signup checkout created successfully",
                publicPaidSignupUseCase.execute(request)
        );
    }

    /**
     * Registro directo con verificación por correo.
     * Mantiene compatibilidad con el flujo agregado por el equipo.
     */
    @PostMapping("/signup-direct")
    public ApiResponse<PublicSignupResponse> signupDirect(
            @Valid @RequestBody PublicSignupRequest request
    ) {
        return ApiResponse.success(
                "Public signup completed successfully",
                publicSignupDirectUseCase.execute(request)
        );
    }
}
