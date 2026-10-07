package com.hrcoresystem.hrcore_api.modules.governance.notifications.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, UUID> {

    @Query(value = """
            SELECT *
            FROM tenant_notifications n
            WHERE n.user_id = :userId
              AND n.archived_at IS NULL
              AND (n.expires_at IS NULL OR n.expires_at > CURRENT_TIMESTAMP)
            ORDER BY n.created_at DESC
            """, nativeQuery = true)
    List<NotificationEntity> findInboxByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);

    @Query(value = """
            SELECT COUNT(*)
            FROM tenant_notifications n
            WHERE n.user_id = :userId
              AND n.read_at IS NULL
              AND n.archived_at IS NULL
              AND (n.expires_at IS NULL OR n.expires_at > CURRENT_TIMESTAMP)
            """, nativeQuery = true)
    long countUnreadInboxByUserId(@Param("userId") UUID userId);
}
