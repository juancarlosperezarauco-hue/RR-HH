import { Injectable } from '@angular/core';
import { StoredAuthSession } from '../auth/auth-session.model';

@Injectable({ providedIn: 'root' })
export class AuthStorageService {
  private readonly key = 'hrcore.auth.tenant.session.v2';
  private readonly legacyKeys = ['access_token', 'refresh_token', 'tenant_slug'];
  constructor() { for (const key of this.legacyKeys) localStorage.removeItem(key); }
  saveSession(session: StoredAuthSession): void {
    if (session.scope !== 'TENANT' || !session.tenantSlug || session.tenantSlug === 'platform') throw new Error('Invalid tenant session');
    localStorage.setItem(this.key, JSON.stringify(session));
  }
  getSession(): StoredAuthSession | null { return this.read(); }
  hasValidTenantSession(): boolean { return this.read() !== null; }
  clear(): void { localStorage.removeItem(this.key); }
  getToken(): string | null { return this.read()?.accessToken ?? null; }
  getRefreshToken(): string | null { return this.read()?.refreshToken ?? null; }
  getTenantSlug(): string | null { return this.read()?.tenantSlug ?? null; }
  hasToken(): boolean { return !!this.getToken(); }
  hasRefreshToken(): boolean { return !!this.getRefreshToken(); }
  hasTenantSlug(): boolean { return !!this.getTenantSlug(); }
  getSessionKey(): string { const s = this.read(); return s ? `${s.loginId}:${s.revision}` : ''; }
  saveToken(token: string): void { this.update({ accessToken: token }); }
  saveRefreshToken(token: string): void { this.update({ refreshToken: token }); }
  saveTenantSlug(slug: string): void { this.update({ tenantSlug: slug }); }
  private update(update: Partial<StoredAuthSession>): void { const s = this.read(); if (s) this.saveSession({ ...s, ...update, revision: crypto.randomUUID() }); }
  private read(): StoredAuthSession | null {
    const raw = localStorage.getItem(this.key); if (!raw) return null;
    try {
      const value = JSON.parse(raw) as Partial<StoredAuthSession>;
      if (value.scope !== 'TENANT' || typeof value.tenantSlug !== 'string' || !value.tenantSlug || value.tenantSlug === 'platform' ||
          typeof value.accessToken !== 'string' || !value.accessToken || typeof value.refreshToken !== 'string' || !value.refreshToken ||
          typeof value.accessExpiresAt !== 'number' || typeof value.revision !== 'string' || typeof value.loginId !== 'string') throw new Error('Invalid session');
      return value as StoredAuthSession;
    } catch { this.clear(); return null; }
  }
}
