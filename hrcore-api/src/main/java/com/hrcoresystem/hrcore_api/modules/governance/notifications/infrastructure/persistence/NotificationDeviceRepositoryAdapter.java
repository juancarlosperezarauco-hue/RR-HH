package com.hrcoresystem.hrcore_api.modules.governance.notifications.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationDevice;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationDeviceStatus;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationPlatform;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.repository.NotificationDeviceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class NotificationDeviceRepositoryAdapter implements NotificationDeviceRepository {

    private final NotificationDeviceJpaRepository repository;

    public NotificationDeviceRepositoryAdapter(NotificationDeviceJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public NotificationDevice save(NotificationDevice device) {
        return toDomain(repository.save(toEntity(device)));
    }

    @Override
    public Optional<NotificationDevice> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<NotificationDevice> findByUserIdOrderByCreatedAtDesc(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<NotificationDevice> findByUserIdAndDeviceId(UUID userId, String deviceId) {
        return repository.findByUserIdAndDeviceId(userId, deviceId).map(this::toDomain);
    }

    @Override
    public Optional<NotificationDevice> findByUserIdAndFcmToken(UUID userId, String fcmToken) {
        return repository.findByUserIdAndFcmToken(userId, fcmToken).map(this::toDomain);
    }

    @Override
    public Optional<NotificationDevice> findByFcmToken(String fcmToken) {
        return repository.findByFcmToken(fcmToken).map(this::toDomain);
    }

    @Override
    public List<NotificationDevice> findActiveByUserId(UUID userId) {
        return repository.findByUserIdAndStatus(userId, NotificationDeviceStatus.ACTIVE.name())
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private NotificationDeviceEntity toEntity(NotificationDevice device) {
        NotificationDeviceEntity entity = new NotificationDeviceEntity();
        entity.setId(device.id());
        entity.setUserId(device.userId());
        entity.setDeviceId(device.deviceId());
        entity.setFcmToken(device.fcmToken());
        entity.setPlatform(device.platform().name());
        entity.setDeviceName(device.deviceName());
        entity.setAppVersion(device.appVersion());
        entity.setOsVersion(device.osVersion());
        entity.setStatus(device.status().name());
        entity.setLastSeenAt(device.lastSeenAt());
        entity.setCreatedAt(device.createdAt());
        entity.setUpdatedAt(device.updatedAt());
        return entity;
    }

    private NotificationDevice toDomain(NotificationDeviceEntity entity) {
        return new NotificationDevice(
                entity.getId(),
                entity.getUserId(),
                entity.getDeviceId(),
                entity.getFcmToken(),
                NotificationPlatform.valueOf(entity.getPlatform()),
                entity.getDeviceName(),
                entity.getAppVersion(),
                entity.getOsVersion(),
                NotificationDeviceStatus.valueOf(entity.getStatus()),
                entity.getLastSeenAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
