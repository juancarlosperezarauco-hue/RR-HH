package com.hrcoresystem.hrcore_api.modules.governance.audit.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.common.pagination.PaginationSupport;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.dto.AuditEventResponse;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.usecase.ListTenantAuditEventsUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit/events")
@SecurityRequirement(name = "bearerAuth")
public class TenantAuditController {

    private final ListTenantAuditEventsUseCase listTenantAuditEventsUseCase;

    public TenantAuditController(ListTenantAuditEventsUseCase listTenantAuditEventsUseCase) {
        this.listTenantAuditEventsUseCase = listTenantAuditEventsUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('audit.events.read')")
    public ApiResponse<Page<AuditEventResponse>> listTenantAuditEvents(
            @ParameterObject @PageableDefault(size = 50) Pageable pageable
    ) {
        return ApiResponse.success(
                "Tenant audit events retrieved successfully",
                PaginationSupport.page(listTenantAuditEventsUseCase.execute(), pageable)
        );
    }
}
