package com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.repository;

import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.Notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {

    Notification save(Notification notification);

    Optional<Notification> findById(UUID id);

    List<Notification> findInboxByUserIdOrderByCreatedAtDesc(UUID userId);

    long countUnreadInboxByUserId(UUID userId);
}
