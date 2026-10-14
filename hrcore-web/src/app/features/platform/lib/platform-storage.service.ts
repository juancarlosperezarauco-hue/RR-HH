import { Injectable } from '@angular/core';
import { AuthenticatedPlatformSuperadminResponse } from '../../../entities/platform/api/platform.service';
import { StoredAuthSession } from '../../../shared/lib/auth/auth-session.model';

@Injectable({ providedIn: 'root' })
export class PlatformStorageService {
  private readonly key = 'hrcore.auth.platform.session.v2';
  private readonly legacyKeys = ['platform_access_token', 'platform_refresh_token', 'platform_user'];
  constructor() { for (const key of this.legacyKeys) localStorage.removeItem(key); }
  saveSession(session: StoredAuthSession): void { if (session.scope !== 'PLATFORM' || session.tenantSlug !== null) throw new Error('Invalid platform session'); localStorage.setItem(this.key, JSON.stringify(session)); }
  getSession(): StoredAuthSession | null {
    const raw = localStorage.getItem(this.key); if (!raw) return null;
    try { const value = JSON.parse(raw) as Partial<StoredAuthSession>; if (value.scope !== 'PLATFORM' || value.tenantSlug !== null || typeof value.accessToken !== 'string' || !value.accessToken || typeof value.refreshToken !== 'string' || !value.refreshToken || typeof value.accessExpiresAt !== 'number' || typeof value.revision !== 'string' || typeof value.loginId !== 'string') throw new Error('Invalid session'); return value as StoredAuthSession; } catch { this.clearSession(); return null; }
  }
  clearSession(): void { localStorage.removeItem(this.key); }
  getAccessToken(): string | null { return this.getSession()?.accessToken ?? null; }
  getRefreshToken(): string | null { return this.getSession()?.refreshToken ?? null; }
  hasAccessToken(): boolean { return !!this.getAccessToken(); }
  hasRefreshToken(): boolean { return !!this.getRefreshToken(); }
  saveAccessToken(token: string): void { this.update({ accessToken: token }); }
  saveRefreshToken(token: string): void { this.update({ refreshToken: token }); }
  saveUser(_user: AuthenticatedPlatformSuperadminResponse): void { /* Identity is revalidated through /me. */ }
  getUser(): AuthenticatedPlatformSuperadminResponse | null { return null; }
  saveToken(token: string): void { this.saveAccessToken(token); }
  getToken(): string | null { return this.getAccessToken(); }
  clear(): void { this.clearSession(); }
  private update(update: Partial<StoredAuthSession>): void { const s = this.getSession(); if (s) this.saveSession({ ...s, ...update, revision: crypto.randomUUID() }); }
}
