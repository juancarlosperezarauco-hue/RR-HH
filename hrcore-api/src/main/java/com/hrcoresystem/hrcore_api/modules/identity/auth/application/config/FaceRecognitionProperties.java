package com.hrcoresystem.hrcore_api.modules.identity.auth.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConfigurationProperties(prefix = "app.auth.face-recognition")
public class FaceRecognitionProperties {

    private boolean enabled = true;
    private String apiKey = "";
    private String apiSecret = "";
    private String baseUrl = "https://api-us.faceplusplus.com/facepp/v3";
    private String fallbackBaseUrl = "";
    private double confidenceThreshold = 80.0;
    private int timeoutMs = 15000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getFallbackBaseUrl() {
        return fallbackBaseUrl;
    }

    public void setFallbackBaseUrl(String fallbackBaseUrl) {
        this.fallbackBaseUrl = fallbackBaseUrl;
    }

    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public void setConfidenceThreshold(double confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public boolean isConfigured() {
        return enabled
                && StringUtils.hasText(apiKey)
                && StringUtils.hasText(apiSecret);
    }
}
