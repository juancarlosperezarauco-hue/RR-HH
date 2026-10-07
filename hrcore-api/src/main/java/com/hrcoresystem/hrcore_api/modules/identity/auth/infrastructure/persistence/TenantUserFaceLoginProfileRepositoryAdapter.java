package com.hrcoresystem.hrcore_api.modules.identity.auth.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.model.TenantUserFaceLoginProfile;
import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.repository.TenantUserFaceLoginProfileRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class TenantUserFaceLoginProfileRepositoryAdapter implements TenantUserFaceLoginProfileRepository {

    private final TenantUserFaceLoginProfileJpaRepository jpaRepository;

    public TenantUserFaceLoginProfileRepositoryAdapter(
            TenantUserFaceLoginProfileJpaRepository jpaRepository
    ) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public TenantUserFaceLoginProfile save(TenantUserFaceLoginProfile profile) {
        TenantUserFaceLoginProfileEntity entity = toEntity(profile);
        TenantUserFaceLoginProfileEntity saved = jpaRepository.saveAndFlush(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<TenantUserFaceLoginProfile> findByUserId(UUID userId) {
        return jpaRepository.findById(userId).map(this::toDomain);
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return jpaRepository.existsById(userId);
    }

    @Override
    public void deleteByUserId(UUID userId) {
        jpaRepository.deleteById(userId);
    }

    private TenantUserFaceLoginProfileEntity toEntity(TenantUserFaceLoginProfile profile) {
        TenantUserFaceLoginProfileEntity entity = new TenantUserFaceLoginProfileEntity();
        entity.setUserId(profile.userId());
        entity.setFaceToken(profile.faceToken());
        entity.setFaceId(profile.faceId());
        entity.setProfilePhotoUrl(profile.profilePhotoUrl());
        entity.setProfilePhotoContentType(profile.profilePhotoContentType());
        entity.setEnabled(profile.enabled());
        entity.setEnrolledAt(profile.enrolledAt());
        entity.setUpdatedAt(profile.updatedAt());
        return entity;
    }

    private TenantUserFaceLoginProfile toDomain(TenantUserFaceLoginProfileEntity entity) {
        return new TenantUserFaceLoginProfile(
                entity.getUserId(),
                entity.getFaceToken(),
                entity.getFaceId(),
                entity.getProfilePhotoUrl(),
                entity.getProfilePhotoContentType(),
                entity.isEnabled(),
                entity.getEnrolledAt(),
                entity.getUpdatedAt()
        );
    }
}
