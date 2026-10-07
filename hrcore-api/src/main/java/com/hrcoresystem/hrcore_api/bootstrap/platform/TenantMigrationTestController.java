package com.hrcoresystem.hrcore_api.bootstrap.platform;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.common.tenancy.migration.TenantSchemaMigrationService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public/dev-tenancy")
@Profile("dev")
@Hidden
public class TenantMigrationTestController {

    private final TenantSchemaMigrationService tenantSchemaMigrationService;

    public TenantMigrationTestController(TenantSchemaMigrationService tenantSchemaMigrationService) {
        this.tenantSchemaMigrationService = tenantSchemaMigrationService;
    }

    @PostMapping("/migrate-schema/{schemaName}")
    public ApiResponse<Map<String, String>> migrateTenantSchema(@PathVariable String schemaName) {
        tenantSchemaMigrationService.migrateSchema(schemaName);

        return ApiResponse.success(
                "Tenant schema migrated successfully",
                Map.of("schemaName", schemaName)
        );
    }
}