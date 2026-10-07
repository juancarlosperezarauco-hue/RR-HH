package com.hrcoresystem.hrcore_api.modules.platform.superadmin.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.platform.superadmin.domain.model.PlatformSuperadmin;
import com.hrcoresystem.hrcore_api.modules.platform.superadmin.domain.repository.PlatformSuperadminRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PlatformSuperadminRepositoryAdapter implements PlatformSuperadminRepository {

    private final PlatformSuperadminJpaRepository jpaRepository;

    public PlatformSuperadminRepositoryAdapter(PlatformSuperadminJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PlatformSuperadmin save(PlatformSuperadmin superadmin) {
        PlatformSuperadminEntity entity = toEntity(superadmin);
        PlatformSuperadminEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<PlatformSuperadmin> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public List<PlatformSuperadmin> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    private PlatformSuperadminEntity toEntity(PlatformSuperadmin superadmin) {
        PlatformSuperadminEntity entity = new PlatformSuperadminEntity();
        entity.setId(superadmin.id());
        entity.setEmail(superadmin.email());
        entity.setPasswordHash(superadmin.passwordHash());
        entity.setFirstName(superadmin.firstName());
        entity.setLastName(superadmin.lastName());
        entity.setActive(superadmin.active());
        return entity;
    }

    private PlatformSuperadmin toDomain(PlatformSuperadminEntity entity) {
        return new PlatformSuperadmin(
                entity.getId(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
