package com.hrcoresystem.hrcore_api.modules.governance.notifications.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationCategory;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationPreference;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.repository.NotificationPreferenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class NotificationPreferenceRepositoryAdapter implements NotificationPreferenceRepository {

    private final NotificationPreferenceJpaRepository repository;

    public NotificationPreferenceRepositoryAdapter(NotificationPreferenceJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public NotificationPreference save(NotificationPreference preference) {
        return toDomain(repository.save(toEntity(preference)));
    }

    @Override
    public Optional<NotificationPreference> findByUserIdAndCategory(UUID userId, NotificationCategory category) {
        return repository.findByUserIdAndCategory(userId, category.name()).map(this::toDomain);
    }

    @Override
    public List<NotificationPreference> findByUserIdOrderByCategory(UUID userId) {
        return repository.findByUserIdOrderByCategory(userId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private NotificationPreferenceEntity toEntity(NotificationPreference preference) {
        NotificationPreferenceEntity entity = new NotificationPreferenceEntity();
        entity.setId(preference.id());
        entity.setUserId(preference.userId());
        entity.setCategory(preference.category().name());
        entity.setPushEnabled(preference.pushEnabled());
        entity.setInAppEnabled(preference.inAppEnabled());
        entity.setEmailEnabled(preference.emailEnabled());
        entity.setSmsEnabled(preference.smsEnabled());
        entity.setCreatedAt(preference.createdAt());
        entity.setUpdatedAt(preference.updatedAt());
        return entity;
    }

    private NotificationPreference toDomain(NotificationPreferenceEntity entity) {
        return new NotificationPreference(
                entity.getId(),
                entity.getUserId(),
                NotificationCategory.valueOf(entity.getCategory()),
                entity.isPushEnabled(),
                entity.isInAppEnabled(),
                entity.isEmailEnabled(),
                entity.isSmsEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
