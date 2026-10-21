export interface TenantDashboardResponse {
  metadata: TenantDashboardMetadata;
  metrics: TenantDashboardMetric[];
  sections: TenantDashboardSection[];
}

export interface TenantDashboardMetadata {
  generatedAt: string;
  timezone: string;
  tenantSlug: string;
  generatedBy: string | null;
  fromDate: string;
  toDate: string;
}

export interface TenantDashboardMetric {
  key: string;
  label: string;
  value: unknown;
  sourcePermission: string;
}

export interface TenantDashboardSection {
  key: string;
  title: string;
  sourcePermission: string;
  data: Record<string, unknown>;
}
