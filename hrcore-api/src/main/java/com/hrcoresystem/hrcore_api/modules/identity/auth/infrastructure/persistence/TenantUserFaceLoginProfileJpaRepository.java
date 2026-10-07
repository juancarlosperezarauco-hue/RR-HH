package com.hrcoresystem.hrcore_api.modules.identity.auth.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TenantUserFaceLoginProfileJpaRepository extends JpaRepository<TenantUserFaceLoginProfileEntity, UUID> {
}
