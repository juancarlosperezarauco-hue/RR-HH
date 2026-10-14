package com.hrcoresystem.hrcore_api.modules.platform.auth.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.platform.auth.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.platform.auth.application.usecase.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;
import java.util.UUID;
import com.hrcoresystem.hrcore_api.common.security.session.AuthSessionManagementService;
import com.hrcoresystem.hrcore_api.common.security.session.AuthSessionResponse;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;

@RestController
@RequestMapping("/api/platform/auth")
public class PlatformAuthController {

    private final LoginPlatformSuperadminUseCase loginPlatformSuperadminUseCase;
    private final RefreshPlatformTokenUseCase refreshPlatformTokenUseCase;
    private final GetCurrentAuthenticatedPlatformSuperadminUseCase getCurrentAuthenticatedPlatformSuperadminUseCase;
    private final ChangePlatformPasswordUseCase changePlatformPasswordUseCase;
    private final LogoutPlatformSuperadminUseCase logoutPlatformSuperadminUseCase;
    private final AuthSessionManagementService sessionManagement;
    private final SecurityContextFacade securityContextFacade;

    public PlatformAuthController(
            LoginPlatformSuperadminUseCase loginPlatformSuperadminUseCase,
            RefreshPlatformTokenUseCase refreshPlatformTokenUseCase,
            GetCurrentAuthenticatedPlatformSuperadminUseCase getCurrentAuthenticatedPlatformSuperadminUseCase,
            ChangePlatformPasswordUseCase changePlatformPasswordUseCase,
            LogoutPlatformSuperadminUseCase logoutPlatformSuperadminUseCase,
            AuthSessionManagementService sessionManagement,
            SecurityContextFacade securityContextFacade
    ) {
        this.loginPlatformSuperadminUseCase = loginPlatformSuperadminUseCase;
        this.refreshPlatformTokenUseCase = refreshPlatformTokenUseCase;
        this.getCurrentAuthenticatedPlatformSuperadminUseCase = getCurrentAuthenticatedPlatformSuperadminUseCase;
        this.changePlatformPasswordUseCase = changePlatformPasswordUseCase;
        this.logoutPlatformSuperadminUseCase = logoutPlatformSuperadminUseCase;
        this.sessionManagement = sessionManagement;
        this.securityContextFacade = securityContextFacade;
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

    @GetMapping("/sessions")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("@authorizationGuards.isPlatformAuthenticated()")
    public ApiResponse<List<AuthSessionResponse>> sessions() { return ApiResponse.success("Sessions retrieved successfully", sessionManagement.platformSessions(securityContextFacade.requireCurrentUserId(), securityContextFacade.getCurrentSessionId())); }

    @DeleteMapping("/sessions/{sessionId}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("@authorizationGuards.isPlatformAuthenticated()")
    public ApiResponse<Map<String,String>> revokeSession(@PathVariable UUID sessionId) { sessionManagement.revokePlatform(sessionId, securityContextFacade.requireCurrentUserId()); return ApiResponse.success("Session revoked successfully", Map.of("status","ok")); }

    @PostMapping("/sessions/revoke-others")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("@authorizationGuards.isPlatformAuthenticated()")
    public ApiResponse<Map<String,String>> revokeOtherSessions() { sessionManagement.revokeOtherPlatform(securityContextFacade.requireCurrentUserId(), securityContextFacade.getCurrentSessionId()); return ApiResponse.success("Other sessions revoked successfully", Map.of("status","ok")); }

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
