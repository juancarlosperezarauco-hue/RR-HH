package com.hrcoresystem.hrcore_api.modules.governance.notifications.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.notifications.account-activation")
public class AccountActivationNotificationProperties {

    private int expirationMinutes = 1440;
    private String frontendUrlBase = "http://localhost:4200";
    private String activationUrlPath = "/verify-email";

    public int getExpirationMinutes() {
        return expirationMinutes;
    }

    public void setExpirationMinutes(int expirationMinutes) {
        this.expirationMinutes = expirationMinutes;
    }

    public String getFrontendUrlBase() {
        return frontendUrlBase;
    }

    public void setFrontendUrlBase(String frontendUrlBase) {
        this.frontendUrlBase = frontendUrlBase;
    }

    public String getActivationUrlPath() {
        return activationUrlPath;
    }

    public void setActivationUrlPath(String activationUrlPath) {
        this.activationUrlPath = activationUrlPath;
    }
}
