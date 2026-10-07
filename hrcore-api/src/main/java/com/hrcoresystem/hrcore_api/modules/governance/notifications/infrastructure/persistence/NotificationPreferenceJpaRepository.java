package com.hrcoresystem.hrcore_api.modules.governance.notifications.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferenceJpaRepository extends JpaRepository<NotificationPreferenceEntity, UUID> {

    Optional<NotificationPreferenceEntity> findByUserIdAndCategory(UUID userId, String category);

    List<NotificationPreferenceEntity> findByUserIdOrderByCategory(UUID userId);
}
