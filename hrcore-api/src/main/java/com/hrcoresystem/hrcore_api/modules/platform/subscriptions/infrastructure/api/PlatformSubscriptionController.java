package com.hrcoresystem.hrcore_api.modules.platform.subscriptions.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.common.pagination.PaginationSupport;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.dto.AssignPlatformSubscriptionRequest;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.dto.PlatformSubscriptionResponse;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.usecase.AssignPlatformSubscriptionUseCase;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.usecase.GetPlatformSubscriptionByIdUseCase;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.usecase.ListPlatformSubscriptionsUseCase;
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
@RequestMapping("/api/platform/subscriptions")
@SecurityRequirement(name = "bearerAuth")
public class PlatformSubscriptionController {

    private final AssignPlatformSubscriptionUseCase assignPlatformSubscriptionUseCase;
    private final ListPlatformSubscriptionsUseCase listPlatformSubscriptionsUseCase;
    private final GetPlatformSubscriptionByIdUseCase getPlatformSubscriptionByIdUseCase;

    public PlatformSubscriptionController(
            AssignPlatformSubscriptionUseCase assignPlatformSubscriptionUseCase,
            ListPlatformSubscriptionsUseCase listPlatformSubscriptionsUseCase,
            GetPlatformSubscriptionByIdUseCase getPlatformSubscriptionByIdUseCase
    ) {
        this.assignPlatformSubscriptionUseCase = assignPlatformSubscriptionUseCase;
        this.listPlatformSubscriptionsUseCase = listPlatformSubscriptionsUseCase;
        this.getPlatformSubscriptionByIdUseCase = getPlatformSubscriptionByIdUseCase;
    }

    @PostMapping("/assign")
    @PreAuthorize("@authorizationGuards.isPlatformAdmin()")
    public ApiResponse<PlatformSubscriptionResponse> assignSubscription(
            @Valid @RequestBody AssignPlatformSubscriptionRequest request
    ) {
        return ApiResponse.success(
                "Platform subscription assigned successfully",
                assignPlatformSubscriptionUseCase.execute(request)
        );
    }

    @GetMapping
    @PreAuthorize("@authorizationGuards.isPlatformAdmin()")
    public ApiResponse<Page<PlatformSubscriptionResponse>> listSubscriptions(@ParameterObject @PageableDefault(size = 50) Pageable pageable) {
        return ApiResponse.success(
                "Platform subscriptions retrieved successfully",
                PaginationSupport.page(listPlatformSubscriptionsUseCase.execute(), pageable)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authorizationGuards.isPlatformAdmin()")
    public ApiResponse<PlatformSubscriptionResponse> getSubscriptionById(@PathVariable UUID id) {
        return ApiResponse.success(
                "Platform subscription retrieved successfully",
                getPlatformSubscriptionByIdUseCase.execute(id)
        );
    }
}
