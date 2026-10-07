package com.hrcoresystem.hrcore_api.modules.platform.dashboard.application.dto;

public record TenantSummaryResponse(
        long totalUsers,
        int maxUsers,
        String activePlan,
        long trialDaysLeft
) {
}
