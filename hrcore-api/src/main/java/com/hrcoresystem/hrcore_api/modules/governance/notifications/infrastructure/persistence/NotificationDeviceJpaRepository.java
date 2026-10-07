package com.hrcoresystem.hrcore_api.modules.governance.notifications.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface NotificationDeviceJpaRepository extends JpaRepository<NotificationDeviceEntity, UUID> {

    List<NotificationDeviceEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<NotificationDeviceEntity> findByUserIdAndStatus(UUID userId, String status);

    Optional<NotificationDeviceEntity> findByUserIdAndDeviceId(UUID userId, String deviceId);

    Optional<NotificationDeviceEntity> findByUserIdAndFcmToken(UUID userId, String fcmToken);

    Optional<NotificationDeviceEntity> findByFcmToken(String fcmToken);
}
