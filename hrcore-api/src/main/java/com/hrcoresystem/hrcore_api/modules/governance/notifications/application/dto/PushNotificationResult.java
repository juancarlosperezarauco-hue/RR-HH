package com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto;

public record PushNotificationResult(
        boolean delivered,
        String providerMessageId,
        String errorCode,
        String errorMessage
) {

    public static PushNotificationResult delivered(String providerMessageId) {
        return new PushNotificationResult(true, providerMessageId, null, null);
    }

    public static PushNotificationResult skipped() {
        return new PushNotificationResult(false, null, null, null);
    }

    public static PushNotificationResult failed(String errorCode, String errorMessage) {
        return new PushNotificationResult(false, null, errorCode, errorMessage);
    }
}
