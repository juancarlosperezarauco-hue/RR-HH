package com.hrcoresystem.hrcore_api.modules.governance.notifications.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto.NotificationPublishRequest;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto.PushNotificationMessage;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto.PushNotificationResult;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto.RegisterNotificationDeviceRequest;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto.UpsertNotificationPreferenceRequest;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.config.NotificationPushProperties;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.port.NotificationPublisherPort;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.port.PushNotificationProviderPort;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class NotificationApplicationService implements NotificationPublisherPort {

    private static final int MAX_INBOX_LIMIT = 100;

    private final NotificationRepository notificationRepository;
    private final NotificationDeviceRepository notificationDeviceRepository;
    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final NotificationTemplateRepository notificationTemplateRepository;
    private final PushNotificationProviderPort pushNotificationProviderPort;
    private final NotificationPushProperties notificationPushProperties;

    public NotificationApplicationService(
            NotificationRepository notificationRepository,
            NotificationDeviceRepository notificationDeviceRepository,
            NotificationDeliveryRepository notificationDeliveryRepository,
            NotificationPreferenceRepository notificationPreferenceRepository,
            NotificationTemplateRepository notificationTemplateRepository,
            PushNotificationProviderPort pushNotificationProviderPort,
            NotificationPushProperties notificationPushProperties
    ) {
        this.notificationRepository = notificationRepository;
        this.notificationDeviceRepository = notificationDeviceRepository;
        this.notificationDeliveryRepository = notificationDeliveryRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.notificationTemplateRepository = notificationTemplateRepository;
        this.pushNotificationProviderPort = pushNotificationProviderPort;
        this.notificationPushProperties = notificationPushProperties;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publish(NotificationPublishRequest request) {
        Notification notification = notificationRepository.save(
                new Notification(
                        null,
                        requireUserId(request.userId()),
                        require(request.type(), "notification type"),
                        require(request.category(), "notification category"),
                        require(request.priority(), "notification priority"),
                        requireText(request.title(), "title"),
                        requireText(request.body(), "body"),
                        request.data(),
                        request.imageUrl(),
                        request.actionUrl(),
                        null,
                        null,
                        null,
                        request.expiresAt(),
                        null,
                        null
                )
        );

        NotificationPreference preference = notificationPreferenceRepository
                .findByUserIdAndCategory(notification.userId(), notification.category())
                .orElseGet(() -> new NotificationPreference(
                        null,
                        notification.userId(),
                        notification.category(),
                        true,
                        true,
                        false,
                        false,
                        null,
                        null
                ));

        boolean pushEnabled = notificationPushProperties.isEnabled()
                && preference.pushEnabled()
                && notification.category() != NotificationCategory.ADMIN;
        List<NotificationDevice> devices = pushEnabled
                ? notificationDeviceRepository.findActiveByUserId(notification.userId())
                : List.of();

        if (!pushEnabled || devices.isEmpty()) {
            notificationDeliveryRepository.save(
                    new NotificationDelivery(
                            null,
                            notification.id(),
                            null,
                            NotificationChannel.IN_APP,
                            "IN_APP",
                            NotificationDeliveryStatus.SKIPPED,
                            null,
                            null,
                            pushEnabled ? "No active devices found" : "Push disabled by preference",
                            Instant.now(),
                            null,
                            null,
                            null,
                            null
                    )
            );
            return;
        }

        for (NotificationDevice device : devices) {
            NotificationDelivery delivery = notificationDeliveryRepository.save(
                    new NotificationDelivery(
                            null,
                            notification.id(),
                            device.id(),
                            NotificationChannel.PUSH,
                            pushProviderName(),
                            NotificationDeliveryStatus.PENDING,
                            null,
                            null,
                            null,
                            Instant.now(),
                            null,
                            null,
                            null,
                            null
                    )
            );

            PushNotificationResult result = safeSendPush(
                    new PushNotificationMessage(
                            notification.id(),
                            notification.userId(),
                            device.fcmToken(),
                            notification.title(),
                            notification.body(),
                            notification.data(),
                            notification.imageUrl(),
                            notification.actionUrl()
                    )
            );

            notificationDeliveryRepository.save(
                    new NotificationDelivery(
                            delivery.id(),
                            delivery.notificationId(),
                            delivery.deviceId(),
                            delivery.channel(),
                            delivery.provider(),
                            result.delivered() ? NotificationDeliveryStatus.SENT : NotificationDeliveryStatus.FAILED,
                            result.providerMessageId(),
                            result.errorCode(),
                            result.errorMessage(),
                            delivery.attemptedAt(),
                            result.delivered() ? Instant.now() : null,
                            result.delivered() ? null : Instant.now(),
                            delivery.createdAt(),
                            Instant.now()
                    )
            );
        }
    }

    @Transactional(readOnly = true)
    public List<Notification> listNotifications(UUID userId) {
        requireUserId(userId);
        return notificationRepository.findInboxByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Notification> listNotifications(UUID userId, int limit) {
        requireUserId(userId);
        int effectiveLimit = Math.min(Math.max(1, limit), MAX_INBOX_LIMIT);
        return notificationRepository.findInboxByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .limit(effectiveLimit)
                .toList();
    }

    @Transactional(readOnly = true)
    public Notification getNotification(UUID userId, UUID notificationId) {
        Notification notification = findOwnedNotification(userId, notificationId);
        if (notification == null) {
            throw new ResourceNotFoundException("Notification not found");
        }
        return notification;
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        requireUserId(userId);
        return notificationRepository.countUnreadInboxByUserId(userId);
    }

    @Transactional
    public Notification markAsRead(UUID userId, UUID notificationId) {
        Notification notification = findOwnedNotification(userId, notificationId);
        if (notification == null) {
            throw new ResourceNotFoundException("Notification not found");
        }
        if (notification.readAt() != null) {
            return notification;
        }
        return notificationRepository.save(new Notification(
                notification.id(),
                notification.userId(),
                notification.type(),
                notification.category(),
                notification.priority(),
                notification.title(),
                notification.body(),
                notification.data(),
                notification.imageUrl(),
                notification.actionUrl(),
                Instant.now(),
                notification.openedAt(),
                notification.archivedAt(),
                notification.expiresAt(),
                notification.createdAt(),
                notification.updatedAt()
        ));
    }

    @Transactional
    public Notification openNotification(UUID userId, UUID notificationId) {
        Notification notification = findOwnedNotification(userId, notificationId);
        if (notification == null) {
            throw new ResourceNotFoundException("Notification not found");
        }
        if (notification.openedAt() != null && notification.readAt() != null) {
            return notification;
        }
        Instant now = Instant.now();
        return notificationRepository.save(new Notification(
                notification.id(),
                notification.userId(),
                notification.type(),
                notification.category(),
                notification.priority(),
                notification.title(),
                notification.body(),
                notification.data(),
                notification.imageUrl(),
                notification.actionUrl(),
                notification.readAt() == null ? now : notification.readAt(),
                now,
                notification.archivedAt(),
                notification.expiresAt(),
                notification.createdAt(),
                notification.updatedAt()
        ));
    }

    @Transactional
    public List<Notification> markAllAsRead(UUID userId) {
        requireUserId(userId);
        Instant now = Instant.now();
        return notificationRepository.findInboxByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(notification -> notification.readAt() == null)
                .map(notification -> notificationRepository.save(new Notification(
                        notification.id(),
                        notification.userId(),
                        notification.type(),
                        notification.category(),
                        notification.priority(),
                        notification.title(),
                        notification.body(),
                        notification.data(),
                        notification.imageUrl(),
                        notification.actionUrl(),
                        now,
                        notification.openedAt(),
                        notification.archivedAt(),
                        notification.expiresAt(),
                        notification.createdAt(),
                        notification.updatedAt()
                )))
                .toList();
    }

    @Transactional
    public Notification archive(UUID userId, UUID notificationId) {
        Notification notification = findOwnedNotification(userId, notificationId);
        if (notification == null) {
            throw new ResourceNotFoundException("Notification not found");
        }
        Instant now = Instant.now();
        return notificationRepository.save(new Notification(
                notification.id(),
                notification.userId(),
                notification.type(),
                notification.category(),
                notification.priority(),
                notification.title(),
                notification.body(),
                notification.data(),
                notification.imageUrl(),
                notification.actionUrl(),
                notification.readAt() == null ? now : notification.readAt(),
                notification.openedAt(),
                now,
                notification.expiresAt(),
                notification.createdAt(),
                notification.updatedAt()
        ));
    }

    @Transactional(readOnly = true)
    public List<NotificationDevice> listDevices(UUID userId) {
        requireUserId(userId);
        return notificationDeviceRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public NotificationDevice deactivateDevice(UUID userId, UUID deviceId) {
        NotificationDevice device = findOwnedDevice(userId, deviceId);
        if (device == null) {
            throw new ResourceNotFoundException("Notification device not found");
        }
        return notificationDeviceRepository.save(new NotificationDevice(
                device.id(),
                device.userId(),
                device.deviceId(),
                device.fcmToken(),
                device.platform(),
                device.deviceName(),
                device.appVersion(),
                device.osVersion(),
                NotificationDeviceStatus.INACTIVE,
                Instant.now(),
                device.createdAt(),
                device.updatedAt()
        ));
    }

    @Transactional
    public NotificationDevice revokeDevice(UUID userId, UUID deviceId) {
        NotificationDevice device = findOwnedDevice(userId, deviceId);
        if (device == null) {
            throw new ResourceNotFoundException("Notification device not found");
        }
        return notificationDeviceRepository.save(new NotificationDevice(
                device.id(),
                device.userId(),
                device.deviceId(),
                device.fcmToken(),
                device.platform(),
                device.deviceName(),
                device.appVersion(),
                device.osVersion(),
                NotificationDeviceStatus.REVOKED,
                device.lastSeenAt(),
                device.createdAt(),
                device.updatedAt()
        ));
    }

    @Transactional(readOnly = true)
    public List<NotificationPreference> listPreferences(UUID userId) {
        requireUserId(userId);
        List<NotificationPreference> existing = notificationPreferenceRepository.findByUserIdOrderByCategory(userId);
        return java.util.Arrays.stream(NotificationCategory.values())
                .map(category -> existing.stream()
                        .filter(preference -> preference.category() == category)
                        .findFirst()
                        .orElseGet(() -> defaultPreference(userId, category)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationTemplate> listTemplates() {
        return notificationTemplateRepository.findAllOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public NotificationTemplate getTemplateById(UUID id) {
        if (id == null) {
            throw new BusinessException("Notification template id is required");
        }
        return notificationTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification template not found"));
    }

    @Transactional(readOnly = true)
    public List<NotificationDelivery> listDeliveriesByNotification(UUID notificationId) {
        if (notificationId == null) {
            throw new BusinessException("Notification id is required");
        }
        requireNotificationExists(notificationId);
        return notificationDeliveryRepository.findByNotificationIdOrderByCreatedAtDesc(notificationId);
    }

    @Transactional
    public NotificationDevice registerDevice(UUID userId, RegisterNotificationDeviceRequest request) {
        if (request == null) {
            throw new BusinessException("Notification device request is required");
        }
        if (userId == null) {
            throw new BusinessException("Notification device user id is required");
        }
        if (request.deviceId() == null || request.deviceId().isBlank()) {
            throw new BusinessException("Notification device id is required");
        }
        if (request.fcmToken() == null || request.fcmToken().isBlank()) {
            throw new BusinessException("Notification FCM token is required");
        }
        if (request.platform() == null) {
            throw new BusinessException("Notification platform is required");
        }

        NotificationDevice existing = notificationDeviceRepository
                .findByUserIdAndDeviceId(userId, request.deviceId())
                .or(() -> notificationDeviceRepository.findByFcmToken(request.fcmToken()))
                .orElse(null);

        NotificationDevice device = new NotificationDevice(
                existing == null ? null : existing.id(),
                userId,
                request.deviceId(),
                request.fcmToken(),
                request.platform(),
                request.deviceName(),
                request.appVersion(),
                request.osVersion(),
                NotificationDeviceStatus.ACTIVE,
                Instant.now(),
                existing == null ? null : existing.createdAt(),
                null
        );

        return notificationDeviceRepository.save(device);
    }

    @Transactional
    public NotificationPreference upsertPreference(UUID userId, UpsertNotificationPreferenceRequest request) {
        if (request == null) {
            throw new BusinessException("Notification preference request is required");
        }
        if (userId == null) {
            throw new BusinessException("Notification preference user id is required");
        }
        if (request.category() == null) {
            throw new BusinessException("Notification preference category is required");
        }

        NotificationPreference preference = notificationPreferenceRepository
                .findByUserIdAndCategory(userId, request.category())
                .map(existing -> new NotificationPreference(
                        existing.id(),
                        userId,
                        request.category(),
                        request.pushEnabled(),
                        request.inAppEnabled(),
                        request.emailEnabled(),
                        request.smsEnabled(),
                        existing.createdAt(),
                        null
                ))
                .orElseGet(() -> new NotificationPreference(
                        null,
                        userId,
                        request.category(),
                        request.pushEnabled(),
                        request.inAppEnabled(),
                        request.emailEnabled(),
                        request.smsEnabled(),
                        null,
                        null
                ));

        return notificationPreferenceRepository.save(preference);
    }

    private String pushProviderName() {
        return notificationPushProperties.getProvider() == null
                ? "UNKNOWN"
                : notificationPushProperties.getProvider().name();
    }

    private NotificationPreference defaultPreference(UUID userId, NotificationCategory category) {
        return new NotificationPreference(
                null,
                userId,
                category,
                true,
                true,
                false,
                false,
                null,
                null
        );
    }

    private Notification findOwnedNotification(UUID userId, UUID notificationId) {
        requireUserId(userId);
        if (notificationId == null) {
            return null;
        }
        return notificationRepository.findById(notificationId)
                .filter(notification -> Objects.equals(notification.userId(), userId))
                .orElse(null);
    }

    private NotificationDevice findOwnedDevice(UUID userId, UUID deviceId) {
        requireUserId(userId);
        if (deviceId == null) {
            return null;
        }
        return notificationDeviceRepository.findById(deviceId)
                .filter(device -> Objects.equals(device.userId(), userId))
                .orElse(null);
    }

    private PushNotificationResult safeSendPush(PushNotificationMessage message) {
        try {
            return pushNotificationProviderPort.send(message);
        } catch (Exception exception) {
            return PushNotificationResult.failed(
                    "PUSH_PROVIDER_ERROR",
                    exception.getMessage() == null ? "Push provider failed" : exception.getMessage()
            );
        }
    }

    private Notification requireNotificationExists(UUID notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
    }

    private <T> T require(T value, String fieldName) {
        if (value == null) {
            throw new BusinessException("Notification " + fieldName + " is required");
        }
        return value;
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("Notification " + fieldName + " is required");
        }
        return value;
    }

    private UUID requireUserId(UUID userId) {
        if (userId == null) {
            throw new BusinessException("Notification user id is required");
        }
        return userId;
    }
}
