export interface SuperadminDashboardResponse {
  metadata: DashboardMetadata;
  filters: DashboardFilters;
  comparisons: DashboardComparisons;
  summary: DashboardSummary;
  tenants: TenantsSection;
  subscriptions: SubscriptionsSection;
  plans: PlansSection;
  users: UsersSection;
  audit: AuditSection;
  alerts: AlertItem[];
  insights: InsightItem[];
  recentActivity: ActivityItem[];
}

export interface DashboardMetadata {
  generatedAt: string;
  timezone: string;
  dataCompleteness: string;
}

export interface DashboardFilters {
  period: string;
  year: number | null;
  month: number | null;
  date: string | null;
  from: string | null;
  to: string | null;
}

export interface DashboardComparisons {
  previousPeriod: PeriodRange;
  summary: ComparisonSummary;
}

export interface PeriodRange {
  from: string;
  to: string;
}

export interface ComparisonSummary {
  tenantsChangePercent: number | string;
  activeSubscriptionsChangePercent: number | string;
  newTenantsChangePercent: number | string;
  expiredSubscriptionsChangePercent: number | string;
}

export interface DashboardSummary {
  tenants: TenantsSummary;
  subscriptions: SubscriptionsSummary;
  plans: PlansSummary;
  users: UsersSummary;
  audit: AuditSummary;
  system: SystemSummary;
}

export interface TenantsSummary {
  total: number;
  active: number;
  inactive: number;
  newThisPeriod: number;
}

export interface SubscriptionsSummary {
  active: number;
  trial: number;
  expired: number;
  cancelled: number;
  expiringSoon: number;
}

export interface PlansSummary {
  total: number;
  active: number;
}

export interface UsersSummary {
  total: number;
  active: number;
  inactive: number;
  platformAdmins: number;
}

export interface AuditSummary {
  events: number;
  failedEvents: number;
}

export interface SystemSummary {
  registeredTenantSchemas: number;
  activeTenantSchemas: number;
  inactiveTenantSchemas: number;
}

export interface TenantsSection {
  byStatus: TenantStatusItem[];
  recent: TenantItem[];
  inactive: TenantItem[];
}

export interface TenantStatusItem {
  status: string;
  label: string;
  total: number;
}

export interface TenantItem {
  id: string;
  name: string;
  slug: string;
  schemaName: string;
  status: string;
  planCode: string | null;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface SubscriptionsSection {
  byStatus: SubscriptionStatusItem[];
  byPlan: SubscriptionPlanItem[];
  expiringSoon: SectionBucket<SubscriptionItem>;
  expired: SectionBucket<SubscriptionItem>;
}

export interface SubscriptionStatusItem {
  status: string;
  total: number;
}

export interface SubscriptionPlanItem {
  planCode: string;
  total: number;
}

export interface SectionBucket<T> {
  total: number;
  items: T[];
}

export interface SubscriptionItem {
  id: string;
  tenantId: string;
  tenantName: string | null;
  tenantSlug: string | null;
  planCode: string | null;
  status: string;
  expiresAt: string | null;
  expiredAt: string | null;
}

export interface PlansSection {
  items: PlanItem[];
}

export interface PlanItem {
  code: string;
  name: string;
  type: string;
  active: boolean;
  maxUsers: number;
  maxRoles: number;
  trialDays: number | null;
  tenants: number;
}

export interface UsersSection {
  total: number;
  active: number;
  inactive: number;
  platformAdmins: number;
}

export interface AuditSection {
  byEventType: AuditEventItem[];
  recent: SectionBucketRecent;
}

export interface AuditEventItem {
  eventType: string;
  total: number;
}

export interface SectionBucketRecent {
  total: number;
  items: AuditItem[];
}

export interface AuditItem {
  id: string;
  actorEmail: string | null;
  eventType: string | null;
  resourceType: string | null;
  resourceId: string | null;
  outcome: string | null;
  createdAt: string | null;
}

export interface AlertItem {
  id: string;
  type: string;
  severity: string;
  title: string;
  description: string;
  createdAt: string | null;
}

export interface InsightItem {
  type: string;
  severity: string;
  title: string;
  description: string;
}

export interface ActivityItem {
  id: string;
  type: string;
  title: string;
  description: string;
  actorName: string | null;
  createdAt: string | null;
}
