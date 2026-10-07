package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantSettingJpaRepository extends JpaRepository<TenantSettingEntity, UUID> {

    Optional<TenantSettingEntity> findBySettingKey(String settingKey);
}