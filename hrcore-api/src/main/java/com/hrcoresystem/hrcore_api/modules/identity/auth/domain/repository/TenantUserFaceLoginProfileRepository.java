package com.hrcoresystem.hrcore_api.modules.identity.auth.domain.repository;

import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.model.TenantUserFaceLoginProfile;

import java.util.Optional;
import java.util.UUID;

public interface TenantUserFaceLoginProfileRepository {

    TenantUserFaceLoginProfile save(TenantUserFaceLoginProfile profile);

    Optional<TenantUserFaceLoginProfile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}
