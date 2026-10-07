package com.hrcoresystem.hrcore_api.modules.identity.auth.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.usecase.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginTenantUserUseCase loginTenantUserUseCase;
    private final RefreshTenantTokenUseCase refreshTenantTokenUseCase;
    private final ForgotPasswordUseCase forgotPasswordUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final ActivateUserAccountUseCase activateUserAccountUseCase;
    private final ResendAccountActivationUseCase resendAccountActivationUseCase;
    private final GetCurrentAuthenticatedTenantUserUseCase getCurrentAuthenticatedTenantUserUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final LogoutTenantUserUseCase logoutTenantUserUseCase;
    private final LoginTenantUserWithFaceUseCase loginTenantUserWithFaceUseCase;
    private final GetCurrentTenantProfileUseCase getCurrentTenantProfileUseCase;
    private final UpdateCurrentTenantProfileUseCase updateCurrentTenantProfileUseCase;
    private final GetCurrentTenantProfilePhotoUseCase getCurrentTenantProfilePhotoUseCase;

    public AuthController(
            LoginTenantUserUseCase loginTenantUserUseCase,
            RefreshTenantTokenUseCase refreshTenantTokenUseCase,
            ForgotPasswordUseCase forgotPasswordUseCase,
            ResetPasswordUseCase resetPasswordUseCase,
            ActivateUserAccountUseCase activateUserAccountUseCase,
            ResendAccountActivationUseCase resendAccountActivationUseCase,
            GetCurrentAuthenticatedTenantUserUseCase getCurrentAuthenticatedTenantUserUseCase,
            ChangePasswordUseCase changePasswordUseCase,
            LogoutTenantUserUseCase logoutTenantUserUseCase,
            LoginTenantUserWithFaceUseCase loginTenantUserWithFaceUseCase,
            GetCurrentTenantProfileUseCase getCurrentTenantProfileUseCase,
            UpdateCurrentTenantProfileUseCase updateCurrentTenantProfileUseCase,
            GetCurrentTenantProfilePhotoUseCase getCurrentTenantProfilePhotoUseCase
    ) {
        this.loginTenantUserUseCase = loginTenantUserUseCase;
        this.refreshTenantTokenUseCase = refreshTenantTokenUseCase;
        this.forgotPasswordUseCase = forgotPasswordUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
        this.activateUserAccountUseCase = activateUserAccountUseCase;
        this.resendAccountActivationUseCase = resendAccountActivationUseCase;
        this.getCurrentAuthenticatedTenantUserUseCase = getCurrentAuthenticatedTenantUserUseCase;
        this.changePasswordUseCase = changePasswordUseCase;
        this.logoutTenantUserUseCase = logoutTenantUserUseCase;
        this.loginTenantUserWithFaceUseCase = loginTenantUserWithFaceUseCase;
        this.getCurrentTenantProfileUseCase = getCurrentTenantProfileUseCase;
        this.updateCurrentTenantProfileUseCase = updateCurrentTenantProfileUseCase;
        this.getCurrentTenantProfilePhotoUseCase = getCurrentTenantProfilePhotoUseCase;
    }

    @PostMapping("/login")
    public ApiResponse<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(
                "Login successful",
                loginTenantUserUseCase.execute(request)
        );
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(
                "Token refreshed successfully",
                refreshTenantTokenUseCase.execute(request)
        );
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        forgotPasswordUseCase.execute(request);

        return ApiResponse.success(
                "If the user exists, a password reset email has been sent",
                Map.of("status", "ok")
        );
    }

    @PostMapping("/reset-password")
    public ApiResponse<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        resetPasswordUseCase.execute(request);

        return ApiResponse.success(
                "Password reset successfully",
                Map.of("status", "ok")
        );
    }

    @PostMapping("/activate-account")
    public ApiResponse<Map<String, String>> activateAccount(@Valid @RequestBody ActivateAccountRequest request) {
        activateUserAccountUseCase.execute(request);

        return ApiResponse.success(
                "Account activated successfully",
                Map.of("status", "ok")
        );
    }

    @PostMapping("/resend-activation")
    public ApiResponse<Map<String, String>> resendActivation(@Valid @RequestBody ResendActivationRequest request) {
        resendAccountActivationUseCase.execute(request);

        return ApiResponse.success(
                "If the account exists and is pending, an activation email has been sent",
                Map.of("status", "ok")
        );
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<Map<String, String>> logout() {
        logoutTenantUserUseCase.execute();

        return ApiResponse.success(
                "Logout successful",
                Map.of("status", "ok")
        );
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<AuthenticatedTenantUserResponse> me() {
        return ApiResponse.success(
                "Authenticated user retrieved successfully",
                getCurrentAuthenticatedTenantUserUseCase.execute()
        );
    }

    @GetMapping("/profile")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<CurrentTenantProfileResponse> profile() {
        return ApiResponse.success(
                "Current profile retrieved successfully",
                getCurrentTenantProfileUseCase.execute()
        );
    }

    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<CurrentTenantProfileResponse> updateProfile(
            @RequestParam(value = "firstName", required = false) String firstName,
            @RequestParam(value = "lastName", required = false) String lastName,
            @RequestPart(value = "photo", required = false) MultipartFile photo
    ) {
        UpdateCurrentTenantProfileRequest request = new UpdateCurrentTenantProfileRequest(firstName, lastName);
        return ApiResponse.success(
                "Profile updated successfully",
                updateCurrentTenantProfileUseCase.execute(request, photo)
        );
    }

    @PutMapping(value = "/profile/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<CurrentTenantProfileResponse> updateProfilePhoto(
            @RequestPart("photo") MultipartFile photo
    ) {
        return ApiResponse.success(
                "Profile photo updated successfully",
                updateCurrentTenantProfileUseCase.updatePhoto(photo)
        );
    }

    @GetMapping("/profile/photo")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<byte[]> profilePhoto() {
        CurrentTenantProfilePhotoResponse photo = getCurrentTenantProfilePhotoUseCase.execute();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + (photo.filename() == null ? "profile-photo" : photo.filename()) + "\"")
                .contentType(MediaType.parseMediaType(
                        photo.contentType() == null || photo.contentType().isBlank()
                                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                                : photo.contentType()
                ))
                .body(photo.bytes());
    }

    @DeleteMapping("/profile/photo")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<CurrentTenantProfileResponse> removeProfilePhoto() {
        return ApiResponse.success(
                "Profile photo removed successfully",
                updateCurrentTenantProfileUseCase.removePhoto()
        );
    }

    @PostMapping(value = "/face/login", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AuthTokenResponse> faceLogin(
            @RequestParam("email") String email,
            @RequestPart("image") MultipartFile image
    ) {
        return ApiResponse.success(
                "Face login successful",
                loginTenantUserWithFaceUseCase.execute(email, image)
        );
    }

    @PostMapping("/change-password")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        changePasswordUseCase.execute(request);

        return ApiResponse.success(
                "Password changed successfully",
                Map.of("status", "ok")
        );
    }
}
