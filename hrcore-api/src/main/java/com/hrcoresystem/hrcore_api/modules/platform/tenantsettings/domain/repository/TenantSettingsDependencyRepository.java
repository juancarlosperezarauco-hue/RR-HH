package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.repository;

public interface TenantSettingsDependencyRepository {
    boolean hasOperationalDateDependencies();
    boolean hasContracts();
}
