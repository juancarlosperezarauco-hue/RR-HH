package com.hrcoresystem.hrcore_api.modules.governance.notifications.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationTemplateJpaRepository extends JpaRepository<NotificationTemplateEntity, UUID> {

    Optional<NotificationTemplateEntity> findByTypeAndChannel(String type, String channel);

    List<NotificationTemplateEntity> findAllByOrderByCreatedAtDesc();
}
