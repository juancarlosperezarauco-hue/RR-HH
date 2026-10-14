import { HttpBackend, HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { AuthService } from '../../../entities/auth/api/auth.service';
import { AuthTokenResponse } from '../../../entities/auth/model/auth-token-response.model';
import { AuthenticatedTenantUserResponse } from '../../../entities/auth/model/authenticated-tenant-user-response.model';
import { PlatformService, PlatformAuthTokenResponse, AuthenticatedPlatformSuperadminResponse } from '../../../entities/platform/api/platform.service';
import { AuthStorageService } from '../storage/auth-storage.service';
import { PlatformStorageService } from '../../../features/platform/lib/platform-storage.service';
import { AuthScope, StoredAuthSession } from './auth-session.model';
import { SessionRefreshLockService } from './session-refresh-lock.service';

@Injectable({ providedIn: 'root' })
export class SessionCoordinatorService {
  private readonly auth = inject(AuthService);
  private readonly platform = inject(PlatformService);
  private readonly tenantStorage = inject(AuthStorageService);
  private readonly platformStorage = inject(PlatformStorageService);
  private readonly locks = inject(SessionRefreshLockService);
  private readonly router = inject(Router);
  private readonly rawHttp = new HttpClient(inject(HttpBackend));
  private readonly inFlight = new Map<AuthScope, Promise<StoredAuthSession>>();
  private loggingOut = new Set<AuthScope>();

  async commitLogin(scope: 'TENANT', response: AuthTokenResponse, tenantSlug: string): Promise<AuthenticatedTenantUserResponse>;
  async commitLogin(scope: 'PLATFORM', response: PlatformAuthTokenResponse, tenantSlug?: null): Promise<AuthenticatedPlatformSuperadminResponse>;
  async commitLogin(scope: AuthScope, response: AuthTokenResponse | PlatformAuthTokenResponse, tenantSlug: string | null = null): Promise<any> {
    this.assertTokens(response);
    if (scope === 'TENANT' && (!tenantSlug || tenantSlug === 'platform')) throw new Error('Tenant is required');
    const session: StoredAuthSession = { scope, tenantSlug: scope === 'TENANT' ? tenantSlug : null, accessToken: response.accessToken, refreshToken: response.refreshToken, accessExpiresAt: Date.now() + response.accessExpiresInMs, loginId: crypto.randomUUID(), revision: crypto.randomUUID() };
    this.write(session);
    return scope === 'TENANT' ? this.loadIdentity('TENANT', session) : this.loadIdentity('PLATFORM', session);
  }

  async refreshSingleFlight(scope: AuthScope, snapshot: StoredAuthSession): Promise<StoredAuthSession> {
    const current = this.getSession(scope);
    if (!current || current.loginId !== snapshot.loginId) throw new Error('Session changed');
    const pending = this.inFlight.get(scope); if (pending) return pending;
    const operation = this.locks.run(scope, async () => {
      const latest = this.getSession(scope); if (!latest || latest.loginId !== snapshot.loginId) throw new Error('Session changed');
      if (latest.revision !== snapshot.revision && latest.accessToken !== snapshot.accessToken) return latest;
      const response = scope === 'TENANT'
        ? await firstValueFrom(this.rawHttp.post<any>(`${environment.apiUrl}/api/auth/refresh`, { refreshToken: latest.refreshToken }, { headers: new HttpHeaders({ 'X-Tenant-Slug': latest.tenantSlug! }) }))
        : await firstValueFrom(this.rawHttp.post<any>(`${environment.apiUrl}/api/platform/auth/refresh`, { refreshToken: latest.refreshToken }));
      const data = response?.success ? response.data : null; this.assertTokens(data);
      const after = this.getSession(scope); if (!after || after.loginId !== latest.loginId || after.revision !== latest.revision || after.refreshToken !== latest.refreshToken) throw new Error('Session changed');
      const next = { ...after, accessToken: data.accessToken, refreshToken: data.refreshToken, accessExpiresAt: Date.now() + data.accessExpiresInMs, revision: crypto.randomUUID() };
      this.write(next); return next;
    });
    this.inFlight.set(scope, operation); try { return await operation; } finally { this.inFlight.delete(scope); }
  }

  async loadIdentity(scope: 'TENANT', session?: StoredAuthSession | null): Promise<AuthenticatedTenantUserResponse>;
  async loadIdentity(scope: 'PLATFORM', session?: StoredAuthSession | null): Promise<AuthenticatedPlatformSuperadminResponse>;
  async loadIdentity(scope: AuthScope, session = this.getSession(scope)): Promise<any> {
    if (!session) throw new Error('No session');
    const result = scope === 'TENANT' ? await firstValueFrom(this.auth.getMe()) : await firstValueFrom(this.platform.getMe());
    if (!result.success || !result.data) throw new Error(result.message || 'Identity validation failed');
    const identity: any = result.data;
    if (identity.scope !== scope || (scope === 'TENANT' && identity.tenantSlug !== session.tenantSlug) || (scope === 'PLATFORM' && identity.tenantSlug !== 'platform')) throw new Error('Identity scope mismatch');
    return identity;
  }

  async logout(scope: AuthScope): Promise<{ remoteRevoked: boolean }> {
    const session = this.getSession(scope); this.loggingOut.add(scope);
    let remoteRevoked = false;
    try {
      if (session) {
        const response: any = scope === 'TENANT' ? await firstValueFrom(this.auth.logout()) : await firstValueFrom(this.platform.logout());
        remoteRevoked = response?.success === true;
      }
    } catch { remoteRevoked = false; } finally { this.loggingOut.delete(scope); if (!session || this.getSession(scope)?.loginId === session.loginId) this.clearSession(scope, session?.loginId); }
    return { remoteRevoked };
  }

  async closeCurrentSession(scope: AuthScope, sessionId: string): Promise<{ revokedRemotely: boolean; reason: 'confirmed' | 'network-error' | 'invalid-session' | 'other-error' }> {
    const initial = this.getSession(scope);
    if (!initial || !sessionId) return { revokedRemotely: false, reason: 'invalid-session' };
    return this.locks.run(scope, async () => {
      const current = this.getSession(scope);
      if (!current || current.loginId !== initial.loginId) return { revokedRemotely: false, reason: 'invalid-session' };
      const headers: Record<string, string> = { Authorization: `Bearer ${current.accessToken}` };
      if (scope === 'TENANT' && current.tenantSlug) headers['X-Tenant-Slug'] = current.tenantSlug;
      const endpoint = scope === 'TENANT' ? `${environment.apiUrl}/api/auth/sessions/${encodeURIComponent(sessionId)}` : `${environment.apiUrl}/api/platform/auth/sessions/${encodeURIComponent(sessionId)}`;
      try {
        const response: any = await firstValueFrom(this.rawHttp.delete(endpoint, { headers: new HttpHeaders(headers) }));
        if (!response?.success || !response.data?.status) return { revokedRemotely: false, reason: 'other-error' };
        if (this.getSession(scope)?.loginId === current.loginId) this.clearSession(scope, current.loginId);
        return { revokedRemotely: true, reason: 'confirmed' };
      } catch (error) {
        if (error instanceof HttpErrorResponse && error.status === 401) { this.clearSession(scope, current.loginId); return { revokedRemotely: false, reason: 'invalid-session' }; }
        return { revokedRemotely: false, reason: error instanceof HttpErrorResponse && error.status >= 500 ? 'network-error' : 'other-error' };
      }
    });
  }

  clearSession(scope: AuthScope, expectedLoginId?: string): void { const current = this.getSession(scope); if (expectedLoginId && current?.loginId !== expectedLoginId) return; scope === 'TENANT' ? this.tenantStorage.clear() : this.platformStorage.clearSession(); }
  getSession(scope: AuthScope): StoredAuthSession | null { return scope === 'TENANT' ? this.tenantStorage.getSession() : this.platformStorage.getSession(); }
  isLoggingOut(scope: AuthScope): boolean { return this.loggingOut.has(scope); }
  private write(session: StoredAuthSession): void { session.scope === 'TENANT' ? this.tenantStorage.saveSession(session) : this.platformStorage.saveSession(session); }
  private assertTokens(response: any): asserts response is AuthTokenResponse | PlatformAuthTokenResponse { if (!response || response.tokenType !== 'Bearer' || typeof response.accessToken !== 'string' || !response.accessToken || typeof response.refreshToken !== 'string' || !response.refreshToken || !Number.isInteger(response.accessExpiresInMs) || response.accessExpiresInMs <= 0) throw new Error('Invalid authentication response'); }
}
