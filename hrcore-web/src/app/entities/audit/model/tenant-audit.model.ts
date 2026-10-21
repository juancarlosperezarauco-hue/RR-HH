export interface TenantAuditEventResponse {
  id: string;
  actorSubject?: string | null;
  actorId?: string | null;
  actorEmail?: string | null;
  tenantSlug?: string | null;
  eventType: string;
  resourceType: string;
  resourceId: string;
  eventDetails?: string | null;
  ipAddress?: string | null;
  userAgent?: string | null;
  requestId?: string | null;
  correlationId?: string | null;
  source?: string | null;
  outcome?: string | null;
  beforeState?: string | null;
  afterState?: string | null;
  createdAt: string;
}

export interface TenantAuditFilters {
  startDate?: string | null;
  endDate?: string | null;
  actor?: string | null;
  module?: string | null;
  action?: string | null;
  resourceType?: string | null;
  search?: string | null;
}
