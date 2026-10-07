package com.hrcoresystem.hrcore_api.modules.governance.notifications.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.notifications.push")
public class NotificationPushProperties {

    private boolean enabled = false;
    private NotificationPushProviderType provider = NotificationPushProviderType.FIREBASE;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public NotificationPushProviderType getProvider() {
        return provider;
    }

    public void setProvider(NotificationPushProviderType provider) {
        this.provider = provider;
    }
}
