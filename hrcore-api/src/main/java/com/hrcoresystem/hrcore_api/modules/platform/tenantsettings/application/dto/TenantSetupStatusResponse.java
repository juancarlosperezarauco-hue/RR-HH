package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto;
import java.util.List;
public record TenantSetupStatusResponse(boolean configured, List<String> missingFields) {}
