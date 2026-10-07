package com.hrcoresystem.hrcore_api.modules.platform.auth.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.platform.auth.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.platform.auth.application.usecase.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/platform/auth")
public class PlatformAuthController {

    private final LoginPlatformSuperadminUseCase loginPlatformSuperadminUseCase;
    private final RefreshPlatformTokenUseCase refreshPlatformTokenUseCase;
    private final GetCurrentAuthenticatedPlatformSuperadminUseCase getCurrentAuthenticatedPlatformSuperadminUseCase;
    private final ChangePlatformPasswordUseCase changePlatformPasswordUseCase;
    private final LogoutPlatformSuperadminUseCase logoutPlatformSuperadminUseCase;

    public PlatformAuthController(
            LoginPlatformSuperadminUseCase loginPlatformSuperadminUseCase,
            RefreshPlatformTokenUseCase refreshPlatformTokenUseCase,
            GetCurrentAuthenticatedPlatformSuperadminUseCase getCurrentAuthenticatedPlatformSuperadminUseCase,
            ChangePlatformPasswordUseCase changePlatformPasswordUseCase,
            LogoutPlatformSuperadminUseCase logoutPlatformSuperadminUseCase
    ) {
        this.loginPlatformSuperadminUseCase = loginPlatformSuperadminUseCase;
        this.refreshPlatformTokenUseCase = refreshPlatformTokenUseCase;
        this.getCurrentAuthenticatedPlatformSuperadminUseCase = getCurrentAuthenticatedPlatformSuperadminUseCase;
        this.changePlatformPasswordUseCase = changePlatformPasswordUseCase;
        this.logoutPlatformSuperadminUseCase = logoutPlatformSuperadminUseCase;
    }

    @PostMapping("/login")
    public ApiResponse<PlatformAuthTokenResponse> login(@Valid @RequestBody PlatformLoginRequest request) {
        return ApiResponse.success(
                "Platform login successful",
                loginPlatformSuperadminUseCase.execute(request)
        );
    }

    @PostMapping("/refresh")
    public ApiResponse<PlatformAuthTokenResponse> refresh(@Valid @RequestBody PlatformRefreshTokenRequest request) {
        return ApiResponse.success(
                "Platform token refreshed successfully",
                refreshPlatformTokenUseCase.execute(request)
        );
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("@authorizationGuards.isPlatformAuthenticated()")
    public ApiResponse<AuthenticatedPlatformSuperadminResponse> me() {
        return ApiResponse.success(
                "Authenticated platform superadmin retrieved successfully",
                getCurrentAuthenticatedPlatformSuperadminUseCase.execute()
        );
    }

    @PostMapping("/change-password")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("@authorizationGuards.isPlatformAuthenticated()")
    public ApiResponse<Map<String, String>> changePassword(
            @Valid @RequestBody PlatformChangePasswordRequest request
    ) {
        changePlatformPasswordUseCase.execute(request);

        return ApiResponse.success(
                "Platform password changed successfully",
                Map.of("status", "ok")
        );
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("@authorizationGuards.isPlatformAuthenticated()")
    public ApiResponse<Map<String, String>> logout() {
        logoutPlatformSuperadminUseCase.execute();

        return ApiResponse.success(
                "Platform logout successful",
                Map.of("status", "ok")
        );
    }
}
