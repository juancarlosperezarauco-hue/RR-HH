package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface TenantSettingJpaRepository extends JpaRepository<TenantSettingEntity, UUID> {

    Optional<TenantSettingEntity> findBySettingKey(String settingKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from TenantSettingEntity s where s.settingKey = :settingKey")
    Optional<TenantSettingEntity> findBySettingKeyForUpdate(@Param("settingKey") String settingKey);
}
