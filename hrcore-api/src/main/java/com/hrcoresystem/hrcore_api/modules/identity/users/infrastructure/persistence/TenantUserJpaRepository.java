package com.hrcoresystem.hrcore_api.modules.identity.users.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TenantUserJpaRepository extends JpaRepository<TenantUserEntity, UUID> {

    Optional<TenantUserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByActiveTrue();

    @Query("""
            select count(t)
            from TenantUserEntity t
            where t.active = true or t.status = :status
            """)
    long countByActiveTrueOrStatus(@Param("status") TenantUserStatus status);
}
