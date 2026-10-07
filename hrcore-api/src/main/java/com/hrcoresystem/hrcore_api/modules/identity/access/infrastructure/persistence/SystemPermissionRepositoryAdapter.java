package com.hrcoresystem.hrcore_api.modules.identity.access.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.SystemPermission;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.SystemPermissionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class SystemPermissionRepositoryAdapter implements SystemPermissionRepository {

    private final SystemPermissionJpaRepository jpaRepository;

    public SystemPermissionRepositoryAdapter(SystemPermissionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<SystemPermission> findAllActive() {
        return jpaRepository.findByActiveTrueOrderByModuleAscCodeAsc()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Set<String> findActiveCodes() {
        return jpaRepository.findByActiveTrueOrderByModuleAscCodeAsc()
                .stream()
                .map(SystemPermissionEntity::getCode)
                .collect(Collectors.toSet());
    }

    private SystemPermission toDomain(SystemPermissionEntity entity) {
        return new SystemPermission(
                entity.getId(),
                entity.getCode(),
                entity.getModule(),
                entity.getDescription(),
                entity.isActive(),
                entity.getCreatedAt()
        );
    }
}