package com.hrcoresystem.hrcore_api.modules.platform.superadmin.domain.repository;

import com.hrcoresystem.hrcore_api.modules.platform.superadmin.domain.model.PlatformSuperadmin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlatformSuperadminRepository {

    PlatformSuperadmin save(PlatformSuperadmin superadmin);

    Optional<PlatformSuperadmin> findByEmail(String email);
    Optional<PlatformSuperadmin> findById(UUID id);

    List<PlatformSuperadmin> findAll();

    boolean existsByEmail(String email);
}
