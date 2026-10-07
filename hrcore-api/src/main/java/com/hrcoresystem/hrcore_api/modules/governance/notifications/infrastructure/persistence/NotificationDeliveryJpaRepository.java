package com.hrcoresystem.hrcore_api.modules.governance.notifications.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationDeliveryJpaRepository extends JpaRepository<NotificationDeliveryEntity, UUID> {

    long countByNotificationId(UUID notificationId);

    List<NotificationDeliveryEntity> findByNotificationIdOrderByCreatedAtDesc(UUID notificationId);
}
