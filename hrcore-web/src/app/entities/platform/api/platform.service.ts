import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../shared/api/models/api-response.model';
import { SuperadminDashboardResponse } from './platform-dashboard.model';

export interface PageResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort?: unknown;
    offset?: number;
    unpaged?: boolean;
    paged?: boolean;
  };
  totalPages: number;
  totalElements: number;
  last: boolean;
  size: number;
  number: number;
  sort?: unknown;
  numberOfElements: number;
  first: boolean;
  empty: boolean;
}

export interface PlatformPlan {
  id: string;
  code: string;
  name: string;
  description: string;
  maxUsers: number;
  maxRoles: number;
  planType: 'DEMO' | 'PAID';
  trialDays: number | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePlanRequest {
  code: string;
  name: string;
  planType: 'DEMO' | 'PAID';
  description: string;
  maxUsers: number;
  maxRoles: number;
  trialDays?: number | null;
}

export interface AuditEvent {
  id: string;
  actorSubject: string;
  actorId: string | null;
  actorEmail: string | null;
  tenantSlug: string | null;
  eventType: string;
  resourceType: string;
  resourceId: string;
  eventDetails: string;
  ipAddress: string | null;
  userAgent: string | null;
  requestId: string | null;
  correlationId: string | null;
  source: string | null;
  outcome: string | null;
  beforeState: string | null;
  afterState: string | null;
  createdAt: string;
}


export interface PlatformSuperadmin {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PlatformTenant {
  id: string;
  name: string;
  slug: string;
  schemaName: string;
  status: string;
  planId: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTenantRequest {
  name: string;
  slug: string;
  planCode: string;
  adminEmail: string;      // ✅ Nuevo campo
  password: string;        // ✅ Nuevo campo
  firstName: string;       // ✅ Nuevo campo
  lastName: string;        // ✅ Nuevo campo
}
export interface PlatformSubscription {
  id: string;
  tenantId: string;
  tenantName: string;
  tenantSlug: string;
  planId: string;
  planCode: string;
  planName: string;
  planType: string;
  maxUsers: number;
  maxRoles: number;
  status: string;
  trial: boolean;
  currentSubscription: boolean;
  startedAt: string;
  expiresAt: string;
  remainingDays: number;
  createdAt: string;
  updatedAt: string;
}

export interface AssignSubscriptionRequest {
  tenantId: string;
  planCode: string;
  overrideTrialDays?: number;
}

export interface PlatformLoginRequest {
  email: string;
  password: string;
}

export interface PlatformRefreshTokenRequest {
  refreshToken: string;
}

export interface PlatformChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface PlatformAuthTokenResponse {
  tokenType: string;
  accessToken: string;
  refreshToken: string;
  accessExpiresInMs: number;
}

export interface AuthenticatedPlatformSuperadminResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  active: boolean;
  roles: string[];
  createdAt: string;
  updatedAt: string;
}

export interface PlatformBackup {
  id: string;
  operationType: string;
  scope: string;
  status: string;
  tenantId: string | null;
  tenantSlug: string | null;
  schemaName: string | null;
  sourceBackupId: string | null;
  preRestoreBackupId: string | null;
  fileName: string | null;
  storagePath: string | null;
  format: string | null;
  sizeBytes: number | null;
  checksumSha256: string | null;
  requestedBy: string | null;
  reason: string | null;
  failureReason: string | null;
  startedAt: string | null;
  finishedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateBackupRequest {
  reason?: string | null;
}

export interface RestoreBackupRequest {
  confirmationText: string;
  reason?: string | null;
}

@Injectable({ providedIn: 'root' })
export class PlatformService {
  private http = inject(HttpClient);
  private baseUrl = environment.apiUrl;

  private pageParams(page: number, size: number): HttpParams {
    return new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
  }

  login(credentials: PlatformLoginRequest): Observable<ApiResponse<PlatformAuthTokenResponse>> {
    return this.http.post<ApiResponse<PlatformAuthTokenResponse>>(`${this.baseUrl}/api/platform/auth/login`, credentials);
  }

  refreshToken(request: PlatformRefreshTokenRequest): Observable<ApiResponse<PlatformAuthTokenResponse>> {
    return this.http.post<ApiResponse<PlatformAuthTokenResponse>>(`${this.baseUrl}/api/platform/auth/refresh`, request);
  }

  getMe(): Observable<ApiResponse<AuthenticatedPlatformSuperadminResponse>> {
    return this.http.get<ApiResponse<AuthenticatedPlatformSuperadminResponse>>(
      `${this.baseUrl}/api/platform/auth/me`
    );
  }

  logout(): Observable<ApiResponse<{ status: string }>> {
    return this.http.post<ApiResponse<{ status: string }>>(`${this.baseUrl}/api/platform/auth/logout`, {});
  }

  changePassword(data: PlatformChangePasswordRequest): Observable<ApiResponse<{ status: string }>> {
    return this.http.post<ApiResponse<{ status: string }>>(`${this.baseUrl}/api/platform/auth/change-password`, data);
  }

  getSuperadminMe(): Observable<ApiResponse<AuthenticatedPlatformSuperadminResponse>> {
    return this.http.get<ApiResponse<AuthenticatedPlatformSuperadminResponse>>(
      `${this.baseUrl}/api/platform/superadmins/me`
    );
  }

  getDashboardSummary(): Observable<ApiResponse<SuperadminDashboardResponse>> {
    return this.http.get<ApiResponse<SuperadminDashboardResponse>>(
      `${this.baseUrl}/api/platform/dashboard/summary`
    );
  }

  getTenants(page = 0, size = 20): Observable<ApiResponse<PageResponse<PlatformTenant>>> {
    return this.http.get<ApiResponse<PageResponse<PlatformTenant>>>(
      `${this.baseUrl}/api/platform/tenants`,
      { params: this.pageParams(page, size) }
    );
  }

  getTenantById(id: string): Observable<ApiResponse<PlatformTenant>> {
    return this.http.get<ApiResponse<PlatformTenant>>(
      `${this.baseUrl}/api/platform/tenants/${id}`
    );
  }

  createTenant(request: CreateTenantRequest): Observable<ApiResponse<PlatformTenant>> {
    return this.http.post<ApiResponse<PlatformTenant>>(
      `${this.baseUrl}/api/platform/tenants`,
      request
    );
  }

  activateTenant(id: string): Observable<ApiResponse<PlatformTenant>> {
    return this.http.patch<ApiResponse<PlatformTenant>>(
      `${this.baseUrl}/api/platform/tenants/${id}/activate`,
      {}
    );
  }

  deactivateTenant(id: string): Observable<ApiResponse<PlatformTenant>> {
    return this.http.patch<ApiResponse<PlatformTenant>>(
      `${this.baseUrl}/api/platform/tenants/${id}/deactivate`,
      {}
    );
  }
  // ============================================================
  // SUBSCRIPTIONS (nuevos)
  // ============================================================
  
  getSubscriptions(page = 0, size = 20): Observable<ApiResponse<PageResponse<PlatformSubscription>>> {
    return this.http.get<ApiResponse<PageResponse<PlatformSubscription>>>(
      `${this.baseUrl}/api/platform/subscriptions`,
      { params: this.pageParams(page, size) }
    );
  }

  getSubscriptionById(id: string): Observable<ApiResponse<PlatformSubscription>> {
    return this.http.get<ApiResponse<PlatformSubscription>>(
      `${this.baseUrl}/api/platform/subscriptions/${id}`
    );
  }

  assignSubscription(request: AssignSubscriptionRequest): Observable<ApiResponse<PlatformSubscription>> {
    return this.http.post<ApiResponse<PlatformSubscription>>(
      `${this.baseUrl}/api/platform/subscriptions/assign`,
      request
    );
  }
  // ============================================================
  // PLANS
  // ============================================================
  
  getPlans(page = 0, size = 20): Observable<ApiResponse<PageResponse<PlatformPlan>>> {
    return this.http.get<ApiResponse<PageResponse<PlatformPlan>>>(
      `${this.baseUrl}/api/platform/plans`,
      { params: this.pageParams(page, size) }
    );
  }

  getPlanById(id: string): Observable<ApiResponse<PlatformPlan>> {
    return this.http.get<ApiResponse<PlatformPlan>>(
      `${this.baseUrl}/api/platform/plans/${id}`
    );
  }

  createPlan(request: CreatePlanRequest): Observable<ApiResponse<PlatformPlan>> {
    return this.http.post<ApiResponse<PlatformPlan>>(
      `${this.baseUrl}/api/platform/plans`,
      request
    );
  }

  activatePlan(id: string): Observable<ApiResponse<PlatformPlan>> {
    return this.http.patch<ApiResponse<PlatformPlan>>(
      `${this.baseUrl}/api/platform/plans/${id}/activate`,
      {}
    );
  }

  deactivatePlan(id: string): Observable<ApiResponse<PlatformPlan>> {
    return this.http.patch<ApiResponse<PlatformPlan>>(
      `${this.baseUrl}/api/platform/plans/${id}/deactivate`,
      {}
    );
  }

  // ============================================================
  // AUDIT
  // ============================================================
  
  getAuditEvents(page = 0, size = 20): Observable<ApiResponse<PageResponse<AuditEvent>>> {
    return this.http.get<ApiResponse<PageResponse<AuditEvent>>>(
      `${this.baseUrl}/api/platform/audit/events`,
      { params: this.pageParams(page, size) }
    );
  }

  getBackups(page = 0, size = 20): Observable<ApiResponse<PageResponse<PlatformBackup>>> {
    return this.http.get<ApiResponse<PageResponse<PlatformBackup>>>(
      `${this.baseUrl}/api/platform/backups`,
      { params: this.pageParams(page, size) }
    );
  }

  getTenantBackups(page = 0, size = 20): Observable<ApiResponse<PageResponse<PlatformBackup>>> {
    return this.http.get<ApiResponse<PageResponse<PlatformBackup>>>(
      `${this.baseUrl}/api/backups`,
      { params: this.pageParams(page, size) }
    );
  }

  getBackupById(id: string): Observable<ApiResponse<PlatformBackup>> {
    return this.http.get<ApiResponse<PlatformBackup>>(`${this.baseUrl}/api/platform/backups/${id}`);
  }

  getTenantBackupById(id: string): Observable<ApiResponse<PlatformBackup>> {
    return this.http.get<ApiResponse<PlatformBackup>>(`${this.baseUrl}/api/backups/${id}`);
  }

  createFullBackup(reason?: string | null): Observable<ApiResponse<PlatformBackup>> {
    return this.http.post<ApiResponse<PlatformBackup>>(`${this.baseUrl}/api/platform/backups/full`, {
      reason: reason ?? null
    });
  }

  createTenantSelfBackup(reason?: string | null): Observable<ApiResponse<PlatformBackup>> {
    return this.http.post<ApiResponse<PlatformBackup>>(`${this.baseUrl}/api/backups`, {
      reason: reason ?? null
    });
  }

  createTenantBackup(tenantId: string, reason?: string | null): Observable<ApiResponse<PlatformBackup>> {
    return this.http.post<ApiResponse<PlatformBackup>>(`${this.baseUrl}/api/platform/backups/tenants/${tenantId}`, {
      reason: reason ?? null
    });
  }

  restoreBackup(id: string, request: RestoreBackupRequest): Observable<ApiResponse<PlatformBackup>> {
    return this.http.post<ApiResponse<PlatformBackup>>(`${this.baseUrl}/api/platform/backups/${id}/restore`, request);
  }

  restoreTenantBackup(id: string, request: RestoreBackupRequest): Observable<ApiResponse<PlatformBackup>> {
    return this.http.post<ApiResponse<PlatformBackup>>(`${this.baseUrl}/api/backups/${id}/restore`, request);
  }

  restoreBackupFromFile(
    file: File,
    request: { confirmationText: string; reason?: string | null; scope: 'FULL_DATABASE' | 'TENANT_SCHEMA'; tenantId?: string | null }
  ): Observable<ApiResponse<PlatformBackup>> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('confirmationText', request.confirmationText);
    formData.append('scope', request.scope);

    if (request.reason !== undefined && request.reason !== null && request.reason !== '') {
      formData.append('reason', request.reason);
    }

    if (request.tenantId) {
      formData.append('tenantId', request.tenantId);
    }

    return this.http.post<ApiResponse<PlatformBackup>>(`${this.baseUrl}/api/platform/backups/restore/upload`, formData);
  }

  restoreTenantBackupFromFile(
    file: File,
    request: { confirmationText: string; reason?: string | null }
  ): Observable<ApiResponse<PlatformBackup>> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('confirmationText', request.confirmationText);

    if (request.reason !== undefined && request.reason !== null && request.reason !== '') {
      formData.append('reason', request.reason);
    }

    return this.http.post<ApiResponse<PlatformBackup>>(`${this.baseUrl}/api/backups/restore/upload`, formData);
  }

  downloadBackup(id: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/api/platform/backups/${id}/download`, { responseType: 'blob' });
  }

  downloadTenantBackup(id: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/api/backups/${id}/download`, { responseType: 'blob' });
  }
}
