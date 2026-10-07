package com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.repository;

import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationChannel;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationTemplate;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationTemplateRepository {

    NotificationTemplate save(NotificationTemplate template);

    Optional<NotificationTemplate> findById(UUID id);

    Optional<NotificationTemplate> findByTypeAndChannel(NotificationType type, NotificationChannel channel);

    List<NotificationTemplate> findAllOrderByCreatedAtDesc();
}
