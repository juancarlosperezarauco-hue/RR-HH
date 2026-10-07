package com.hrcoresystem.hrcore_api.modules.governance.backups.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.governance.backups.domain.model.BackupScope;
import com.hrcoresystem.hrcore_api.modules.governance.backups.domain.model.BackupStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface BackupJobJpaRepository extends JpaRepository<BackupJobEntity, UUID> {

    List<BackupJobEntity> findAllByOrderByCreatedAtDesc();

    List<BackupJobEntity> findAllByTenantSlugOrderByCreatedAtDesc(String tenantSlug);

    boolean existsByTenantSlugAndStatusIn(String tenantSlug, Collection<BackupStatus> statuses);

    boolean existsByScopeAndStatusIn(BackupScope scope, Collection<BackupStatus> statuses);
}
