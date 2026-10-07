package com.hrcoresystem.hrcore_api.modules.governance.notifications.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationChannel;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationDelivery;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationDeliveryStatus;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.repository.NotificationDeliveryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class NotificationDeliveryRepositoryAdapter implements NotificationDeliveryRepository {

    private final NotificationDeliveryJpaRepository repository;

    public NotificationDeliveryRepositoryAdapter(NotificationDeliveryJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public NotificationDelivery save(NotificationDelivery delivery) {
        return toDomain(repository.save(toEntity(delivery)));
    }

    @Override
    public long countByNotificationId(UUID notificationId) {
        return repository.countByNotificationId(notificationId);
    }

    @Override
    public List<NotificationDelivery> findByNotificationIdOrderByCreatedAtDesc(UUID notificationId) {
        return repository.findByNotificationIdOrderByCreatedAtDesc(notificationId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private NotificationDeliveryEntity toEntity(NotificationDelivery delivery) {
        NotificationDeliveryEntity entity = new NotificationDeliveryEntity();
        entity.setId(delivery.id());
        entity.setNotificationId(delivery.notificationId());
        entity.setDeviceId(delivery.deviceId());
        entity.setChannel(delivery.channel().name());
        entity.setProvider(delivery.provider());
        entity.setStatus(delivery.status().name());
        entity.setProviderMessageId(delivery.providerMessageId());
        entity.setErrorCode(delivery.errorCode());
        entity.setErrorMessage(delivery.errorMessage());
        entity.setAttemptedAt(delivery.attemptedAt());
        entity.setSentAt(delivery.sentAt());
        entity.setFailedAt(delivery.failedAt());
        entity.setCreatedAt(delivery.createdAt());
        entity.setUpdatedAt(delivery.updatedAt());
        return entity;
    }

    private NotificationDelivery toDomain(NotificationDeliveryEntity entity) {
        return new NotificationDelivery(
                entity.getId(),
                entity.getNotificationId(),
                entity.getDeviceId(),
                NotificationChannel.valueOf(entity.getChannel()),
                entity.getProvider(),
                NotificationDeliveryStatus.valueOf(entity.getStatus()),
                entity.getProviderMessageId(),
                entity.getErrorCode(),
                entity.getErrorMessage(),
                entity.getAttemptedAt(),
                entity.getSentAt(),
                entity.getFailedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
