package com.hrcoresystem.hrcore_api.common.tenancy;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.tenancy")
public class TenancyProperties {

    private String headerName = "X-Tenant-Slug";
    private String publicSchema = "public";
    private String tenantSchemaPrefix = "tenant_";
    private List<String> publicPaths = new ArrayList<>();
    private List<String> globalPaths = new ArrayList<>();

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public String getPublicSchema() {
        return publicSchema;
    }

    public void setPublicSchema(String publicSchema) {
        this.publicSchema = publicSchema;
    }

    public String getTenantSchemaPrefix() {
        return tenantSchemaPrefix;
    }

    public void setTenantSchemaPrefix(String tenantSchemaPrefix) {
        this.tenantSchemaPrefix = tenantSchemaPrefix;
    }

    public List<String> getPublicPaths() {
        return publicPaths;
    }

    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = publicPaths;
    }

    public List<String> getGlobalPaths() {
        return globalPaths;
    }

    public void setGlobalPaths(List<String> globalPaths) {
        this.globalPaths = globalPaths;
    }

    public boolean isPublicPath(String requestPath) {
        return matches(requestPath, publicPaths);
    }

    public boolean isGlobalPath(String requestPath) {
        return matches(requestPath, globalPaths);
    }

    public boolean usesPublicSchema(String requestPath) {
        return isPublicPath(requestPath) || isGlobalPath(requestPath);
    }

    private boolean matches(String requestPath, List<String> patterns) {
        if (!StringUtils.hasText(requestPath)) {
            return false;
        }

        AntPathMatcher matcher = new AntPathMatcher();
        return patterns.stream().anyMatch(pattern -> matcher.match(pattern, requestPath));
    }
}