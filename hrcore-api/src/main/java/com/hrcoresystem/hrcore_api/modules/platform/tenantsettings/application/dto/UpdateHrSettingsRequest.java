package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto;
import jakarta.validation.constraints.NotNull;
public record UpdateHrSettingsRequest(
        @NotNull Boolean employeeWebPunchEnabled, @NotNull Boolean geolocationRequired,
        @NotNull Boolean breakPunchRequired, @NotNull Boolean dailyProcessingEnabled,
        @NotNull Boolean adjustmentApprovalRequired, @NotNull Boolean leaveSelfServiceEnabled,
        @NotNull Boolean leaveSupervisorApprovalEnabled) {}
