export interface CompanySettings {
  companyName: string;
  legalName: string | null;
  timezone: string;
  currency: string;
  contactEmail: string | null;
  contactPhone: string | null;
  country: string | null;
  address: string | null;
  taxIdentifier: string | null;
}

export interface HrSettings {
  employeeWebPunchEnabled: boolean;
  geolocationRequired: boolean;
  breakPunchRequired: boolean;
  dailyProcessingEnabled: boolean;
  adjustmentApprovalRequired: boolean;
  leaveSelfServiceEnabled: boolean;
  leaveSupervisorApprovalEnabled: boolean;
}

export interface CompanySetupStatus {
  configured: boolean;
  missingFields: string[];
}
