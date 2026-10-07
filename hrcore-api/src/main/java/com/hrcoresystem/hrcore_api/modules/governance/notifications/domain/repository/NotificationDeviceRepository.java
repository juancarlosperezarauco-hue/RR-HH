package com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.repository;

import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationDevice;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationDeviceRepository {

    NotificationDevice save(NotificationDevice device);

    Optional<NotificationDevice> findById(UUID id);

    List<NotificationDevice> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<NotificationDevice> findByUserIdAndDeviceId(UUID userId, String deviceId);

    Optional<NotificationDevice> findByUserIdAndFcmToken(UUID userId, String fcmToken);

    Optional<NotificationDevice> findByFcmToken(String fcmToken);

    List<NotificationDevice> findActiveByUserId(UUID userId);
}
