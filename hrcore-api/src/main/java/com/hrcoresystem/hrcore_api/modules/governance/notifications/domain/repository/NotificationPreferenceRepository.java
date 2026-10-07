package com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.repository;

import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationCategory;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationPreference;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferenceRepository {

    NotificationPreference save(NotificationPreference preference);

    Optional<NotificationPreference> findByUserIdAndCategory(UUID userId, NotificationCategory category);

    List<NotificationPreference> findByUserIdOrderByCategory(UUID userId);
}
