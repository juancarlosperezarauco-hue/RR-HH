package com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.repository;

import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationDelivery;

import java.util.List;
import java.util.UUID;

public interface NotificationDeliveryRepository {

    NotificationDelivery save(NotificationDelivery delivery);

    long countByNotificationId(UUID notificationId);

    List<NotificationDelivery> findByNotificationIdOrderByCreatedAtDesc(UUID notificationId);
}
