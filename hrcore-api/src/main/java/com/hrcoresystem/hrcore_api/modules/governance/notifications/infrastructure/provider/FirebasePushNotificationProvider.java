package com.hrcoresystem.hrcore_api.modules.governance.notifications.infrastructure.provider;

import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.config.FirebasePushProperties;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.config.NotificationPushProperties;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.config.NotificationPushProviderType;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto.PushNotificationMessage;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto.PushNotificationResult;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.port.PushNotificationProviderPort;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class FirebasePushNotificationProvider implements PushNotificationProviderPort {

    private static final Logger log = LoggerFactory.getLogger(FirebasePushNotificationProvider.class);

    private final NotificationPushProperties notificationPushProperties;
    private final FirebasePushProperties firebasePushProperties;
    private final ObjectMapper objectMapper;

    private volatile FirebaseMessaging firebaseMessaging;
    private volatile String firebaseInitializationError;

    public FirebasePushNotificationProvider(
            NotificationPushProperties notificationPushProperties,
            FirebasePushProperties firebasePushProperties,
            ObjectMapper objectMapper
    ) {
        this.notificationPushProperties = notificationPushProperties;
        this.firebasePushProperties = firebasePushProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public PushNotificationResult send(PushNotificationMessage message) {
        if (message == null) {
            return PushNotificationResult.failed("INVALID_PUSH_MESSAGE", "Push message is required");
        }
        if (!notificationPushProperties.isEnabled() || !isFirebaseProvider()) {
            return PushNotificationResult.skipped();
        }
        if (message.deviceToken() == null || message.deviceToken().isBlank()) {
            return PushNotificationResult.failed("INVALID_DEVICE_TOKEN", "Device token is required");
        }

        FirebaseMessaging messaging = resolveFirebaseMessaging();
        if (messaging == null) {
            return PushNotificationResult.failed(
                    "FIREBASE_NOT_CONFIGURED",
                    firebaseInitializationError == null ? "Firebase push is not configured" : firebaseInitializationError
            );
        }

        try {
            Message firebaseMessage = buildFirebaseMessage(message);
            String messageId = messaging.send(firebaseMessage);
            log.info(
                    "Firebase push sent for notification {} to device token {}",
                    message.notificationId(),
                    maskToken(message.deviceToken())
            );
            return PushNotificationResult.delivered(messageId);
        } catch (Exception exception) {
            log.warn(
                    "Firebase push failed for notification {}: {}",
                    message.notificationId(),
                    exception.getMessage()
            );
            return PushNotificationResult.failed(
                    "FIREBASE_SEND_ERROR",
                    exception.getMessage() == null ? "Firebase push failed" : exception.getMessage()
            );
        }
    }

    private FirebaseMessaging resolveFirebaseMessaging() {
        if (firebaseMessaging != null) {
            return firebaseMessaging;
        }
        if (firebaseInitializationError != null) {
            return null;
        }
        synchronized (this) {
            if (firebaseMessaging != null) {
                return firebaseMessaging;
            }
            if (firebaseInitializationError != null) {
                return null;
            }
            try {
                firebaseMessaging = initializeFirebaseMessaging();
                return firebaseMessaging;
            } catch (Exception exception) {
                firebaseInitializationError = exception.getMessage() == null
                        ? "Firebase push initialization failed"
                        : exception.getMessage();
                log.warn("Firebase push initialization skipped: {}", firebaseInitializationError);
                return null;
            }
        }
    }

    private FirebaseMessaging initializeFirebaseMessaging() throws Exception {
        String projectId = firebasePushProperties.getProjectId();

        FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder();
        try (InputStream inputStream = openCredentialsStream()) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(inputStream);
            optionsBuilder.setCredentials(credentials);
        }
        if (projectId != null && !projectId.isBlank()) {
            optionsBuilder.setProjectId(projectId);
        }

        FirebaseOptions options = optionsBuilder.build();
        FirebaseApp app;
        try {
            app = FirebaseApp.getInstance();
        } catch (IllegalStateException exception) {
            app = FirebaseApp.initializeApp(options);
        }
        return FirebaseMessaging.getInstance(app);
    }

    private Message buildFirebaseMessage(PushNotificationMessage message) {
        com.google.firebase.messaging.Notification.Builder notificationBuilder = com.google.firebase.messaging.Notification.builder()
                .setTitle(safeValue(message.title()))
                .setBody(safeValue(message.body()));
        if (message.imageUrl() != null && !message.imageUrl().isBlank()) {
            notificationBuilder.setImage(message.imageUrl());
        }

        Message.Builder messageBuilder = Message.builder()
                .setToken(message.deviceToken())
                .setNotification(notificationBuilder.build())
                .putAllData(buildDataPayload(message));
        return messageBuilder.build();
    }

    private Map<String, String> buildDataPayload(PushNotificationMessage message) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("notificationId", valueOf(message.notificationId()));
        data.put("userId", valueOf(message.userId()));
        data.put("title", safeValue(message.title()));
        data.put("body", safeValue(message.body()));
        if (message.actionUrl() != null && !message.actionUrl().isBlank()) {
            data.put("actionUrl", message.actionUrl());
        }
        if (message.imageUrl() != null && !message.imageUrl().isBlank()) {
            data.put("imageUrl", message.imageUrl());
        }
        if (message.data() != null && !message.data().isNull()) {
            data.put("data", message.data().toString());
        }
        return data;
    }

    private InputStream openCredentialsStream() throws Exception {
        String privateKey = firebasePushProperties.getPrivateKey();
        if (privateKey == null || privateKey.isBlank()) {
            throw new IllegalStateException("Firebase private key is not configured");
        }
        Map<String, Object> credentials = new LinkedHashMap<>();
        credentials.put("type", safeValue(firebasePushProperties.getType()));
        credentials.put("project_id", safeValue(firebasePushProperties.getProjectId()));
        credentials.put("private_key_id", safeValue(firebasePushProperties.getPrivateKeyId()));
        credentials.put("private_key", privateKey.replace("\\n", "\n"));
        credentials.put("client_email", safeValue(firebasePushProperties.getClientEmail()));
        credentials.put("client_id", safeValue(firebasePushProperties.getClientId()));
        credentials.put("auth_uri", safeValue(firebasePushProperties.getAuthUri()));
        credentials.put("token_uri", safeValue(firebasePushProperties.getTokenUri()));
        credentials.put("auth_provider_x509_cert_url", safeValue(firebasePushProperties.getAuthProviderX509CertUrl()));
        credentials.put("client_x509_cert_url", safeValue(firebasePushProperties.getClientX509CertUrl()));
        credentials.put("universe_domain", safeValue(firebasePushProperties.getUniverseDomain()));

        byte[] json = objectMapper.writeValueAsBytes(credentials);
        return new ByteArrayInputStream(json);
    }

    private boolean isFirebaseProvider() {
        return notificationPushProperties.getProvider() == NotificationPushProviderType.FIREBASE;
    }

    private String valueOf(UUID value) {
        return value == null ? "" : value.toString();
    }

    private String safeValue(String value) {
        return value == null ? "" : value;
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "***";
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }
}
