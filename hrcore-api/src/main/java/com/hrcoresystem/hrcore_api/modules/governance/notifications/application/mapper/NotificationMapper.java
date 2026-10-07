package com.hrcoresystem.hrcore_api.modules.governance.notifications.application.mapper;

import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto.NotificationDeviceResponse;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto.NotificationDeliveryResponse;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto.NotificationPreferenceResponse;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto.NotificationTemplateResponse;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto.NotificationResponse;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.*;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.id(),
                notification.userId(),
                notification.type().name(),
                notification.category().name(),
                notification.priority().name(),
                resolveStatus(notification),
                notification.title(),
                notification.body(),
                notification.data(),
                notification.imageUrl(),
                notification.actionUrl(),
                notification.readAt(),
                notification.openedAt(),
                notification.archivedAt(),
                notification.expiresAt(),
                notification.createdAt(),
                notification.updatedAt()
        );
    }

    public NotificationDeviceResponse toResponse(NotificationDevice device) {
        return new NotificationDeviceResponse(
                device.id(),
                device.userId(),
                device.deviceId(),
                device.platform().name(),
                device.deviceName(),
                device.appVersion(),
                device.osVersion(),
                device.status().name(),
                device.lastSeenAt(),
                device.createdAt(),
                device.updatedAt()
        );
    }

    public NotificationPreferenceResponse toResponse(NotificationPreference preference) {
        return new NotificationPreferenceResponse(
                preference.id(),
                preference.userId(),
                preference.category().name(),
                preference.pushEnabled(),
                preference.inAppEnabled(),
                preference.emailEnabled(),
                preference.smsEnabled(),
                preference.createdAt(),
                preference.updatedAt()
        );
    }

    public NotificationDeliveryResponse toResponse(NotificationDelivery delivery) {
        return new NotificationDeliveryResponse(
                delivery.id(),
                delivery.notificationId(),
                delivery.deviceId(),
                delivery.channel().name(),
                delivery.provider(),
                delivery.status().name(),
                delivery.providerMessageId(),
                delivery.errorCode(),
                delivery.errorMessage(),
                delivery.attemptedAt(),
                delivery.sentAt(),
                delivery.failedAt(),
                delivery.createdAt(),
                delivery.updatedAt()
        );
    }

    public NotificationTemplateResponse toResponse(NotificationTemplate template) {
        return new NotificationTemplateResponse(
                template.id(),
                template.type().name(),
                template.channel().name(),
                template.titleTemplate(),
                template.bodyTemplate(),
                template.active(),
                template.createdAt(),
                template.updatedAt()
        );
    }

    private String resolveStatus(Notification notification) {
        Instant now = Instant.now();
        if (notification.archivedAt() != null) {
            return NotificationStatus.ARCHIVED.name();
        }
        if (notification.expiresAt() != null && notification.expiresAt().isBefore(now)) {
            return NotificationStatus.EXPIRED.name();
        }
        if (notification.readAt() != null) {
            return NotificationStatus.READ.name();
        }
        return NotificationStatus.UNREAD.name();
    }
}
