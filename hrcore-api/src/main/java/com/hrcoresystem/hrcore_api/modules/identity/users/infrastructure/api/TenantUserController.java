package com.hrcoresystem.hrcore_api.modules.identity.users.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.common.pagination.PaginationSupport;
import com.hrcoresystem.hrcore_api.modules.identity.users.application.dto.CreateTenantUserRequest;
import com.hrcoresystem.hrcore_api.modules.identity.users.application.dto.TenantUserResponse;
import com.hrcoresystem.hrcore_api.modules.identity.users.application.dto.UpdateTenantUserRequest;
import com.hrcoresystem.hrcore_api.modules.identity.users.application.usecase.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@SecurityRequirement(name = "bearerAuth")
public class TenantUserController {

    private final CreateTenantUserUseCase createTenantUserUseCase;
    private final ListTenantUsersUseCase listTenantUsersUseCase;
    private final GetTenantUserByIdUseCase getTenantUserByIdUseCase;
    private final UpdateTenantUserUseCase updateTenantUserUseCase;
    private final ActivateTenantUserUseCase activateTenantUserUseCase;
    private final DeactivateTenantUserUseCase deactivateTenantUserUseCase;

    public TenantUserController(
            CreateTenantUserUseCase createTenantUserUseCase,
            ListTenantUsersUseCase listTenantUsersUseCase,
            GetTenantUserByIdUseCase getTenantUserByIdUseCase,
            UpdateTenantUserUseCase updateTenantUserUseCase,
            ActivateTenantUserUseCase activateTenantUserUseCase,
            DeactivateTenantUserUseCase deactivateTenantUserUseCase
    ) {
        this.createTenantUserUseCase = createTenantUserUseCase;
        this.listTenantUsersUseCase = listTenantUsersUseCase;
        this.getTenantUserByIdUseCase = getTenantUserByIdUseCase;
        this.updateTenantUserUseCase = updateTenantUserUseCase;
        this.activateTenantUserUseCase = activateTenantUserUseCase;
        this.deactivateTenantUserUseCase = deactivateTenantUserUseCase;
    }

    // @PostMapping
    // @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @PreAuthorize("hasAuthority('users.create')")
    public ApiResponse<TenantUserResponse> createUser(@Valid @RequestBody CreateTenantUserRequest request) {
        return ApiResponse.success(
                "Tenant user created successfully",
                createTenantUserUseCase.execute(request)
        );
    }

    @PostMapping("/direct")
    @PreAuthorize("hasAuthority('users.create')")
    public ApiResponse<TenantUserResponse> createUserWithoutVerification(@Valid @RequestBody CreateTenantUserRequest request) {
        return ApiResponse.success(
                "Tenant user created successfully",
                createTenantUserUseCase.execute(request)
        );
    }

    // @GetMapping
    // @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    @PreAuthorize("hasAuthority('users.list')")
    public ApiResponse<Page<TenantUserResponse>> listUsers(@ParameterObject @PageableDefault(size = 50) Pageable pageable) {
        return ApiResponse.success(
                "Tenant users retrieved successfully",
                PaginationSupport.page(listTenantUsersUseCase.execute(), pageable)
        );
    }

    // @GetMapping("/{id}")
    // @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('users.detail')")
    public ApiResponse<TenantUserResponse> getUserById(@PathVariable UUID id) {
        return ApiResponse.success(
                "Tenant user retrieved successfully",
                getTenantUserByIdUseCase.execute(id)
        );
    }

    // @PutMapping("/{id}")
    // @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('users.update')")
    public ApiResponse<TenantUserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTenantUserRequest request
    ) {
        return ApiResponse.success(
                "Tenant user updated successfully",
                updateTenantUserUseCase.execute(id, request)
        );
    }

    // @PatchMapping("/{id}/activate")
    // @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('users.activate')")
    public ApiResponse<TenantUserResponse> activateUser(@PathVariable UUID id) {
        return ApiResponse.success(
                "Tenant user activated successfully",
                activateTenantUserUseCase.execute(id)
        );
    }

    // @PatchMapping("/{id}/deactivate")
    // @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('users.deactivate')")
    public ApiResponse<TenantUserResponse> deactivateUser(@PathVariable UUID id) {
        return ApiResponse.success(
                "Tenant user deactivated successfully",
                deactivateTenantUserUseCase.execute(id)
        );
    }
}
