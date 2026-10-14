package com.hrcoresystem.hrcore_api.common.security.authorization.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface TenantAuthorizationStateJpaRepository extends JpaRepository<TenantAuthorizationStateEntity, Short> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from TenantAuthorizationStateEntity e where e.id = :id")
    Optional<TenantAuthorizationStateEntity> findLockedById(@Param("id") Short id);

    @Modifying
    @Query("update TenantAuthorizationStateEntity e set e.authorizationVersion = e.authorizationVersion + 1, e.updatedAt = CURRENT_TIMESTAMP where e.id = 1")
    int incrementAuthorizationVersion();
}
