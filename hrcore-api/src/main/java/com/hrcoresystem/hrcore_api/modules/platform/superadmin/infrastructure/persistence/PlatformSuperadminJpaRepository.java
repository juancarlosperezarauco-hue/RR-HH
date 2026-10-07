package com.hrcoresystem.hrcore_api.modules.platform.superadmin.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlatformSuperadminJpaRepository extends JpaRepository<PlatformSuperadminEntity, UUID> {

    Optional<PlatformSuperadminEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}