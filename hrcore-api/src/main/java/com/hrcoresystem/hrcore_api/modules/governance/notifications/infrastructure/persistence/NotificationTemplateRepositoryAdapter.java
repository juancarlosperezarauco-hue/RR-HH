package com.hrcoresystem.hrcore_api.modules.governance.notifications.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationChannel;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationTemplate;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationType;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.repository.NotificationTemplateRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class NotificationTemplateRepositoryAdapter implements NotificationTemplateRepository {

    private final NotificationTemplateJpaRepository repository;

    public NotificationTemplateRepositoryAdapter(NotificationTemplateJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public NotificationTemplate save(NotificationTemplate template) {
        return toDomain(repository.save(toEntity(template)));
    }

    @Override
    public Optional<NotificationTemplate> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<NotificationTemplate> findByTypeAndChannel(NotificationType type, NotificationChannel channel) {
        return repository.findByTypeAndChannel(type.name(), channel.name()).map(this::toDomain);
    }

    @Override
    public List<NotificationTemplate> findAllOrderByCreatedAtDesc() {
        return repository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private NotificationTemplateEntity toEntity(NotificationTemplate template) {
        NotificationTemplateEntity entity = new NotificationTemplateEntity();
        entity.setId(template.id());
        entity.setType(template.type().name());
        entity.setChannel(template.channel().name());
        entity.setTitleTemplate(template.titleTemplate());
        entity.setBodyTemplate(template.bodyTemplate());
        entity.setActive(template.active());
        entity.setCreatedAt(template.createdAt());
        entity.setUpdatedAt(template.updatedAt());
        return entity;
    }

    private NotificationTemplate toDomain(NotificationTemplateEntity entity) {
        return new NotificationTemplate(
                entity.getId(),
                NotificationType.valueOf(entity.getType()),
                NotificationChannel.valueOf(entity.getChannel()),
                entity.getTitleTemplate(),
                entity.getBodyTemplate(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
