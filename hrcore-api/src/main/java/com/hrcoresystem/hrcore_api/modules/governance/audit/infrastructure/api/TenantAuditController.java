package com.hrcoresystem.hrcore_api.modules.governance.audit.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.dto.AuditEventResponse;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.dto.TenantAuditFilterCriteria;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.usecase.GetTenantAuditEventByIdUseCase;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.usecase.ListTenantAuditEventsUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit/events")
@SecurityRequirement(name = "bearerAuth")
public class TenantAuditController {

    private final ListTenantAuditEventsUseCase listTenantAuditEventsUseCase;
    private final GetTenantAuditEventByIdUseCase getTenantAuditEventByIdUseCase;

    public TenantAuditController(
            ListTenantAuditEventsUseCase listTenantAuditEventsUseCase,
            GetTenantAuditEventByIdUseCase getTenantAuditEventByIdUseCase
    ) {
        this.listTenantAuditEventsUseCase = listTenantAuditEventsUseCase;
        this.getTenantAuditEventByIdUseCase = getTenantAuditEventByIdUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('audit.events.read') and @tenantOwnershipGuard.isCurrentOwner()")
    public ApiResponse<Page<AuditEventResponse>> listTenantAuditEvents(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        TenantAuditFilterCriteria criteria = new TenantAuditFilterCriteria(
                startDate,
                endDate,
                actor,
                module,
                action,
                resourceType,
                search
        );

        return ApiResponse.success(
                "Tenant audit events retrieved successfully",
                listTenantAuditEventsUseCase.execute(criteria, pageable)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('audit.events.read') and @tenantOwnershipGuard.isCurrentOwner()")
    public ApiResponse<AuditEventResponse> getTenantAuditEventById(@PathVariable UUID id) {
        return ApiResponse.success(
                "Tenant audit event retrieved successfully",
                getTenantAuditEventByIdUseCase.execute(id)
        );
    }
}
