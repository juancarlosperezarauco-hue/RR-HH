package com.hrcoresystem.hrcore_api.bootstrap.platform;

import com.hrcoresystem.hrcore_api.common.tenancy.migration.TenantSchemaMigrationService;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.PlatformSubscriptionLifecycleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.service.PermissionCatalogSynchronizer;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.service.OwnerPermissionSynchronizer;

@Component
public class PlatformBootstrapRunner implements ApplicationRunner {

  private static final Logger logger = LoggerFactory.getLogger(PlatformBootstrapRunner.class);

  private final PlatformBootstrapService platformBootstrapService;
  private final TenantSchemaMigrationService tenantSchemaMigrationService;
  private final PlatformSubscriptionLifecycleService platformSubscriptionLifecycleService;
  private final PermissionCatalogSynchronizer permissionCatalogSynchronizer;
  private final OwnerPermissionSynchronizer ownerPermissionSynchronizer;

  public PlatformBootstrapRunner(
      PlatformBootstrapService platformBootstrapService,
      TenantSchemaMigrationService tenantSchemaMigrationService,
      PlatformSubscriptionLifecycleService platformSubscriptionLifecycleService,
      PermissionCatalogSynchronizer permissionCatalogSynchronizer,
      OwnerPermissionSynchronizer ownerPermissionSynchronizer) {
    this.platformBootstrapService = platformBootstrapService;
    this.tenantSchemaMigrationService = tenantSchemaMigrationService;
    this.platformSubscriptionLifecycleService = platformSubscriptionLifecycleService;
    this.permissionCatalogSynchronizer = permissionCatalogSynchronizer;
    this.ownerPermissionSynchronizer = ownerPermissionSynchronizer;
  }

  @Override
  public void run(ApplicationArguments args) {
    logger.info("Starting platform bootstrap runner...");

    platformBootstrapService.seedBasePlans();
    permissionCatalogSynchronizer.synchronize();
    platformBootstrapService.seedInitialPlatformSuperadmin();

    tenantSchemaMigrationService.migrateRegisteredTenantSchemas();
    ownerPermissionSynchronizer.synchronizeRegisteredTenants();
    platformSubscriptionLifecycleService.refreshExpiredSubscriptions();

    logger.info("Platform bootstrap runner completed successfully.");
  }
}
