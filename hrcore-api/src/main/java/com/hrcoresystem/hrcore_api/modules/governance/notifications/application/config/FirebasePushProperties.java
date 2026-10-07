package com.hrcoresystem.hrcore_api.modules.governance.notifications.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.notifications.push.firebase")
public class FirebasePushProperties {

    private String type = "service_account";
    private String projectId = "hrcore-system-dev";
    private String privateKeyId = "";
    private String privateKey = "";
    private String clientEmail = "";
    private String clientId = "";
    private String authUri = "https://accounts.google.com/o/oauth2/auth";
    private String tokenUri = "https://oauth2.googleapis.com/token";
    private String authProviderX509CertUrl = "https://www.googleapis.com/oauth2/v1/certs";
    private String clientX509CertUrl = "";
    private String universeDomain = "googleapis.com";

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getPrivateKeyId() {
        return privateKeyId;
    }

    public void setPrivateKeyId(String privateKeyId) {
        this.privateKeyId = privateKeyId;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getClientEmail() {
        return clientEmail;
    }

    public void setClientEmail(String clientEmail) {
        this.clientEmail = clientEmail;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getAuthUri() {
        return authUri;
    }

    public void setAuthUri(String authUri) {
        this.authUri = authUri;
    }

    public String getTokenUri() {
        return tokenUri;
    }

    public void setTokenUri(String tokenUri) {
        this.tokenUri = tokenUri;
    }

    public String getAuthProviderX509CertUrl() {
        return authProviderX509CertUrl;
    }

    public void setAuthProviderX509CertUrl(String authProviderX509CertUrl) {
        this.authProviderX509CertUrl = authProviderX509CertUrl;
    }

    public String getClientX509CertUrl() {
        return clientX509CertUrl;
    }

    public void setClientX509CertUrl(String clientX509CertUrl) {
        this.clientX509CertUrl = clientX509CertUrl;
    }

    public String getUniverseDomain() {
        return universeDomain;
    }

    public void setUniverseDomain(String universeDomain) {
        this.universeDomain = universeDomain;
    }
}
