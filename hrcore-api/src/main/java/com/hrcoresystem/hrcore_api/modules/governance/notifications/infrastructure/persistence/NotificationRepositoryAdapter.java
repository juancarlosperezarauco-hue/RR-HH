package com.hrcoresystem.hrcore_api.modules.governance.notifications.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.Notification;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationCategory;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationPriority;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationType;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.repository.NotificationRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class NotificationRepositoryAdapter implements NotificationRepository {

    private final NotificationJpaRepository repository;

    public NotificationRepositoryAdapter(NotificationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Notification save(Notification notification) {
        return toDomain(repository.save(toEntity(notification)));
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Notification> findInboxByUserIdOrderByCreatedAtDesc(UUID userId) {
        return repository.findInboxByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countUnreadInboxByUserId(UUID userId) {
        return repository.countUnreadInboxByUserId(userId);
    }

    private NotificationEntity toEntity(Notification notification) {
        NotificationEntity entity = new NotificationEntity();
        entity.setId(notification.id());
        entity.setUserId(notification.userId());
        entity.setType(notification.type().name());
        entity.setCategory(notification.category().name());
        entity.setPriority(notification.priority().name());
        entity.setTitle(notification.title());
        entity.setBody(notification.body());
        entity.setData(notification.data());
        entity.setImageUrl(notification.imageUrl());
        entity.setActionUrl(notification.actionUrl());
        entity.setReadAt(notification.readAt());
        entity.setOpenedAt(notification.openedAt());
        entity.setArchivedAt(notification.archivedAt());
        entity.setExpiresAt(notification.expiresAt());
        entity.setCreatedAt(notification.createdAt());
        entity.setUpdatedAt(notification.updatedAt());
        return entity;
    }

    private Notification toDomain(NotificationEntity entity) {
        return new Notification(
                entity.getId(),
                entity.getUserId(),
                NotificationType.valueOf(entity.getType()),
                NotificationCategory.valueOf(entity.getCategory()),
                NotificationPriority.valueOf(entity.getPriority()),
                entity.getTitle(),
                entity.getBody(),
                entity.getData(),
                entity.getImageUrl(),
                entity.getActionUrl(),
                entity.getReadAt(),
                entity.getOpenedAt(),
                entity.getArchivedAt(),
                entity.getExpiresAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
