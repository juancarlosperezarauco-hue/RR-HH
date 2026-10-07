package com.hrcoresystem.hrcore_api.bootstrap.platform;

import com.hrcoresystem.hrcore_api.common.tenancy.migration.TenantSchemaMigrationService;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.PlatformSubscriptionLifecycleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class PlatformBootstrapRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(PlatformBootstrapRunner.class);

    private final PlatformBootstrapService platformBootstrapService;
    private final TenantSchemaMigrationService tenantSchemaMigrationService;
    private final PlatformSubscriptionLifecycleService platformSubscriptionLifecycleService;

    public PlatformBootstrapRunner(
            PlatformBootstrapService platformBootstrapService,
            TenantSchemaMigrationService tenantSchemaMigrationService,
            PlatformSubscriptionLifecycleService platformSubscriptionLifecycleService
    ) {
        this.platformBootstrapService = platformBootstrapService;
        this.tenantSchemaMigrationService = tenantSchemaMigrationService;
        this.platformSubscriptionLifecycleService = platformSubscriptionLifecycleService;
    }

    @Override
    public void run(ApplicationArguments args) {
        logger.info("Starting platform bootstrap runner...");

        platformBootstrapService.seedBasePlans();
        platformBootstrapService.seedBaseSystemPermissions();
        platformBootstrapService.seedBaseBackupPermissions();
        platformBootstrapService.seedBaseBillingPermissions();
        platformBootstrapService.seedInitialPlatformSuperadmin();

        tenantSchemaMigrationService.migrateRegisteredTenantSchemas();
        platformBootstrapService.seedBackupPermissionsForRegisteredTenants();
        platformBootstrapService.seedBillingPermissionsForRegisteredTenants();
        platformSubscriptionLifecycleService.refreshExpiredSubscriptions();

        logger.info("Platform bootstrap runner completed successfully.");
    }
}
