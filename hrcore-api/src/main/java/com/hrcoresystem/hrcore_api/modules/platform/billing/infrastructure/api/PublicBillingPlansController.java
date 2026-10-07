package com.hrcoresystem.hrcore_api.modules.platform.billing.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.dto.PublicBillingPlanResponse;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.usecase.ListPublicBillingPlansUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/plans")
public class PublicBillingPlansController {

    private final ListPublicBillingPlansUseCase listPublicBillingPlansUseCase;

    public PublicBillingPlansController(ListPublicBillingPlansUseCase listPublicBillingPlansUseCase) {
        this.listPublicBillingPlansUseCase = listPublicBillingPlansUseCase;
    }

    @GetMapping
    public ApiResponse<List<PublicBillingPlanResponse>> listPlans() {
        return ApiResponse.success(
                "Public billing plans retrieved successfully",
                listPublicBillingPlansUseCase.execute()
        );
    }
}
