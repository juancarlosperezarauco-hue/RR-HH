package com.hrcoresystem.hrcore_api.bootstrap.tenant;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContext;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/tenant")
public class TenantContextTestController {

    @GetMapping("/context")
    public ApiResponse<Map<String, Object>> currentTenantContext() {
        TenantContext tenantContext = TenantContextHolder.getRequired();

        return ApiResponse.success(
                "Tenant context resolved successfully",
                Map.of(
                        "tenantSlug", tenantContext.tenantSlug(),
                        "schemaName", tenantContext.schemaName(),
                        "publicRequest", tenantContext.publicRequest()
                )
        );
    }
}