package com.hrcoresystem.hrcore_api.modules.governance.notifications.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.notifications.password-reset")
public class PasswordResetNotificationProperties {

    private int expirationMinutes = 30;
    private String frontendUrlBase = "http://localhost:4200";
    private String resetUrlPath = "/reset-password";

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

    public String getResetUrlPath() {
        return resetUrlPath;
    }

    public void setResetUrlPath(String resetUrlPath) {
        this.resetUrlPath = resetUrlPath;
    }
}
