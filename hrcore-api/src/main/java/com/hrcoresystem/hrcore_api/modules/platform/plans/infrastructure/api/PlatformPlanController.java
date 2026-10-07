package com.hrcoresystem.hrcore_api.modules.platform.plans.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.common.pagination.PaginationSupport;
import com.hrcoresystem.hrcore_api.modules.platform.plans.application.dto.CreatePlatformPlanRequest;
import com.hrcoresystem.hrcore_api.modules.platform.plans.application.dto.PlatformPlanResponse;
import com.hrcoresystem.hrcore_api.modules.platform.plans.application.usecase.*;
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
@RequestMapping("/api/platform/plans")
@SecurityRequirement(name = "bearerAuth")
public class PlatformPlanController {

    private final CreatePlatformPlanUseCase createPlatformPlanUseCase;
    private final ListPlatformPlansUseCase listPlatformPlansUseCase;
    private final GetPlatformPlanByIdUseCase getPlatformPlanByIdUseCase;
    private final ActivatePlatformPlanUseCase activatePlatformPlanUseCase;
    private final DeactivatePlatformPlanUseCase deactivatePlatformPlanUseCase;

    public PlatformPlanController(
            CreatePlatformPlanUseCase createPlatformPlanUseCase,
            ListPlatformPlansUseCase listPlatformPlansUseCase,
            GetPlatformPlanByIdUseCase getPlatformPlanByIdUseCase,
            ActivatePlatformPlanUseCase activatePlatformPlanUseCase,
            DeactivatePlatformPlanUseCase deactivatePlatformPlanUseCase
    ) {
        this.createPlatformPlanUseCase = createPlatformPlanUseCase;
        this.listPlatformPlansUseCase = listPlatformPlansUseCase;
        this.getPlatformPlanByIdUseCase = getPlatformPlanByIdUseCase;
        this.activatePlatformPlanUseCase = activatePlatformPlanUseCase;
        this.deactivatePlatformPlanUseCase = deactivatePlatformPlanUseCase;
    }

    @PostMapping
    @PreAuthorize("@authorizationGuards.isPlatformAdmin()")
    public ApiResponse<PlatformPlanResponse> createPlan(@Valid @RequestBody CreatePlatformPlanRequest request) {
        return ApiResponse.success(
                "Platform plan created successfully",
                createPlatformPlanUseCase.execute(request)
        );
    }

    @GetMapping
    @PreAuthorize("@authorizationGuards.isPlatformAdmin()")
    public ApiResponse<Page<PlatformPlanResponse>> listPlans(@ParameterObject @PageableDefault(size = 50) Pageable pageable) {
        return ApiResponse.success(
                "Platform plans retrieved successfully",
                PaginationSupport.page(listPlatformPlansUseCase.execute(), pageable)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authorizationGuards.isPlatformAdmin()")
    public ApiResponse<PlatformPlanResponse> getPlanById(@PathVariable UUID id) {
        return ApiResponse.success(
                "Platform plan retrieved successfully",
                getPlatformPlanByIdUseCase.execute(id)
        );
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("@authorizationGuards.isPlatformAdmin()")
    public ApiResponse<PlatformPlanResponse> activatePlan(@PathVariable UUID id) {
        return ApiResponse.success(
                "Platform plan activated successfully",
                activatePlatformPlanUseCase.execute(id)
        );
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("@authorizationGuards.isPlatformAdmin()")
    public ApiResponse<PlatformPlanResponse> deactivatePlan(@PathVariable UUID id) {
        return ApiResponse.success(
                "Platform plan deactivated successfully",
                deactivatePlatformPlanUseCase.execute(id)
        );
    }
}
