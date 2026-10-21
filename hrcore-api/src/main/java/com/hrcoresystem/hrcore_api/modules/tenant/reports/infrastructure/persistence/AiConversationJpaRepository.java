package com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiConversationJpaRepository extends JpaRepository<AiConversationEntity, UUID> {
    Page<AiConversationEntity> findAllByOwnerUserIdOrderByUpdatedAtDescIdDesc(
            UUID ownerUserId, Pageable pageable);

    Optional<AiConversationEntity> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);
}
