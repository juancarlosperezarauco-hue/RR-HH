package com.hrcoresystem.hrcore_api.modules.platform.auth.domain.model;

import java.util.List;

public final class PlatformAuthConstants {

    public static final String PLATFORM_TENANT_SLUG = "platform";
    public static final List<String> PLATFORM_ROLES = List.of("SUPERADMIN", "ADMIN");

    private PlatformAuthConstants() {
    }
}
