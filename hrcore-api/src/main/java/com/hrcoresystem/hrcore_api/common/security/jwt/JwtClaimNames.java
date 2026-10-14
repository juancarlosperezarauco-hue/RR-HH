package com.hrcoresystem.hrcore_api.common.security.jwt;

public final class JwtClaimNames {

    public static final String TENANT = "tenant";
    public static final String SCOPE = "scope";
    public static final String EMAIL = "email";
    public static final String DISPLAY_NAME = "displayName";
    public static final String ROLES = "roles";
    public static final String PERMISSIONS = "permissions";
    public static final String TOKEN_TYPE = "type";
    public static final String SESSION_ID = "sid";
    public static final String SECURITY_VERSION = "sv";
    public static final String AUTHORIZATION_VERSION = "av";
    public static final String CATALOG_VERSION = "cv";
    public static final String REFRESH_GENERATION = "generation";
    public static final String TENANT_SCOPE = "TENANT";
    public static final String PLATFORM_SCOPE = "PLATFORM";

    public static final String ACCESS = "access";
    public static final String REFRESH = "refresh";

    private JwtClaimNames() {
    }
}
