package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSetting;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.repository.TenantSettingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TenantSettingRepositoryAdapter implements TenantSettingRepository {

    private final TenantSettingJpaRepository jpaRepository;

    public TenantSettingRepositoryAdapter(TenantSettingJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<TenantSetting> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<TenantSetting> findByKey(String settingKey) {
        return jpaRepository.findBySettingKey(settingKey).map(this::toDomain);
    }

    @Override
    public TenantSetting save(TenantSetting tenantSetting) {
        TenantSettingEntity entity = toEntity(tenantSetting);
        TenantSettingEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    private TenantSettingEntity toEntity(TenantSetting tenantSetting) {
        TenantSettingEntity entity = new TenantSettingEntity();
        entity.setId(tenantSetting.id());
        entity.setSettingKey(tenantSetting.settingKey());
        entity.setSettingValue(tenantSetting.settingValue());
        return entity;
    }

    private TenantSetting toDomain(TenantSettingEntity entity) {
        return new TenantSetting(
                entity.getId(),
                entity.getSettingKey(),
                entity.getSettingValue(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}