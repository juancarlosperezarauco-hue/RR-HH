package com.hrcoresystem.hrcore_api.modules.governance.audit.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.common.pagination.PaginationSupport;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.dto.AuditEventResponse;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.usecase.ListPlatformAuditEventsUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/platform/audit/events")
@SecurityRequirement(name = "bearerAuth")
public class PlatformAuditController {

    private final ListPlatformAuditEventsUseCase listPlatformAuditEventsUseCase;

    public PlatformAuditController(ListPlatformAuditEventsUseCase listPlatformAuditEventsUseCase) {
        this.listPlatformAuditEventsUseCase = listPlatformAuditEventsUseCase;
    }

    @GetMapping
    @PreAuthorize("@authorizationGuards.isPlatformAdmin()")
    public ApiResponse<Page<AuditEventResponse>> listPlatformAuditEvents(
            @ParameterObject @PageableDefault(size = 50) Pageable pageable
    ) {
        return ApiResponse.success(
                "Platform audit events retrieved successfully",
                PaginationSupport.page(listPlatformAuditEventsUseCase.execute(), pageable)
        );
    }
}
