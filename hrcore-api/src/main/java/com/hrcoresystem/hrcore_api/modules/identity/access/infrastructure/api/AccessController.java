package com.hrcoresystem.hrcore_api.modules.identity.access.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.common.pagination.PaginationSupport;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.usecase.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/access")
@SecurityRequirement(name = "bearerAuth")
public class AccessController {

    private final ListSystemPermissionsUseCase listSystemPermissionsUseCase;
    private final CreateTenantRoleUseCase createTenantRoleUseCase;
    private final ListTenantRolesUseCase listTenantRolesUseCase;
    private final GetTenantRoleByIdUseCase getTenantRoleByIdUseCase;
    private final UpdateTenantRoleUseCase updateTenantRoleUseCase;
    private final ActivateTenantRoleUseCase activateTenantRoleUseCase;
    private final DeactivateTenantRoleUseCase deactivateTenantRoleUseCase;
    private final GetUserRolesUseCase getUserRolesUseCase;
    private final AssignUserRolesUseCase assignUserRolesUseCase;

    public AccessController(
            ListSystemPermissionsUseCase listSystemPermissionsUseCase,
            CreateTenantRoleUseCase createTenantRoleUseCase,
            ListTenantRolesUseCase listTenantRolesUseCase,
            GetTenantRoleByIdUseCase getTenantRoleByIdUseCase,
            UpdateTenantRoleUseCase updateTenantRoleUseCase,
            ActivateTenantRoleUseCase activateTenantRoleUseCase,
            DeactivateTenantRoleUseCase deactivateTenantRoleUseCase,
            GetUserRolesUseCase getUserRolesUseCase,
            AssignUserRolesUseCase assignUserRolesUseCase
    ) {
        this.listSystemPermissionsUseCase = listSystemPermissionsUseCase;
        this.createTenantRoleUseCase = createTenantRoleUseCase;
        this.listTenantRolesUseCase = listTenantRolesUseCase;
        this.getTenantRoleByIdUseCase = getTenantRoleByIdUseCase;
        this.updateTenantRoleUseCase = updateTenantRoleUseCase;
        this.activateTenantRoleUseCase = activateTenantRoleUseCase;
        this.deactivateTenantRoleUseCase = deactivateTenantRoleUseCase;
        this.getUserRolesUseCase = getUserRolesUseCase;
        this.assignUserRolesUseCase = assignUserRolesUseCase;
    }

    // @GetMapping("/permissions")
    // @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('access.permissions.read')")
    public ApiResponse<Page<SystemPermissionResponse>> listPermissions(@ParameterObject @PageableDefault(size = 50) Pageable pageable) {
        return ApiResponse.success(
                "System permissions retrieved successfully",
                PaginationSupport.page(listSystemPermissionsUseCase.execute(), pageable)
        );
    }

    // @GetMapping("/roles")
    // @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('access.roles.read')")
    public ApiResponse<Page<TenantRoleResponse>> listRoles(@ParameterObject @PageableDefault(size = 50) Pageable pageable) {
        return ApiResponse.success(
                "Tenant roles retrieved successfully",
                PaginationSupport.page(listTenantRolesUseCase.execute(), pageable)
        );
    }

    // @PostMapping("/roles")
    // @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('access.roles.create')")
    public ApiResponse<TenantRoleResponse> createRole(@Valid @RequestBody CreateTenantRoleRequest request) {
        return ApiResponse.success(
                "Tenant role created successfully",
                createTenantRoleUseCase.execute(request)
        );
    }

    // @GetMapping("/roles/{id}")
    // @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('access.roles.detail')")
    public ApiResponse<TenantRoleResponse> getRoleById(@PathVariable UUID id) {
        return ApiResponse.success(
                "Tenant role retrieved successfully",
                getTenantRoleByIdUseCase.execute(id)
        );
    }

    // @PutMapping("/roles/{id}")
    // @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('access.roles.update')")
    public ApiResponse<TenantRoleResponse> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTenantRoleRequest request
    ) {
        return ApiResponse.success(
                "Tenant role updated successfully",
                updateTenantRoleUseCase.execute(id, request)
        );
    }

    // @PatchMapping("/roles/{id}/activate")
    // @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/roles/{id}/activate")
    @PreAuthorize("hasAuthority('access.roles.activate')")
    public ApiResponse<TenantRoleResponse> activateRole(@PathVariable UUID id) {
        return ApiResponse.success(
                "Tenant role activated successfully",
                activateTenantRoleUseCase.execute(id)
        );
    }

    // @PatchMapping("/roles/{id}/deactivate")
    // @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/roles/{id}/deactivate")
    @PreAuthorize("hasAuthority('access.roles.deactivate')")
    public ApiResponse<TenantRoleResponse> deactivateRole(@PathVariable UUID id) {
        return ApiResponse.success(
                "Tenant role deactivated successfully",
                deactivateTenantRoleUseCase.execute(id)
        );
    }

    // @GetMapping("/users/{userId}/roles")
    // @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}/roles")
    @PreAuthorize("hasAuthority('access.users.roles.read')")
    public ApiResponse<UserRolesResponse> getUserRoles(@PathVariable UUID userId) {
        return ApiResponse.success(
                "User roles retrieved successfully",
                getUserRolesUseCase.execute(userId)
        );
    }

    // @PutMapping("/users/{userId}/roles")
    // @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/users/{userId}/roles")
    @PreAuthorize("hasAuthority('access.users.roles.assign')")
    public ApiResponse<UserRolesResponse> assignUserRoles(
            @PathVariable UUID userId,
            @RequestBody AssignUserRolesRequest request
    ) {
        return ApiResponse.success(
                "User roles assigned successfully",
                assignUserRolesUseCase.execute(userId, request)
        );
    }
}
