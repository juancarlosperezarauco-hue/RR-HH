package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto;
public record HrSettingsResponse(boolean employeeWebPunchEnabled, boolean geolocationRequired,
 boolean breakPunchRequired, boolean dailyProcessingEnabled, boolean adjustmentApprovalRequired,
 boolean leaveSelfServiceEnabled, boolean leaveSupervisorApprovalEnabled) {}
