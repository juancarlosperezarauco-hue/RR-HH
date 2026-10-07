package com.hrcoresystem.hrcore_api.modules.identity.auth.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConfigurationProperties(prefix = "app.storage.profile-photos")
public class ProfilePhotoStorageProperties {

    private String rootDir = "uploads/profile-photos";

    public String getRootDir() {
        return rootDir;
    }

    public void setRootDir(String rootDir) {
        this.rootDir = rootDir;
    }

    public boolean isConfigured() {
        return StringUtils.hasText(rootDir);
    }
}
