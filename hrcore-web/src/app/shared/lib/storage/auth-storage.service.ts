import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class AuthStorageService {
  private readonly TOKEN_KEY = 'access_token';
  private readonly REFRESH_TOKEN_KEY = 'refresh_token';
  private readonly TENANT_SLUG_KEY = 'tenant_slug';

  saveToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  hasToken(): boolean {
    const token = this.getToken();
    return !!token && token.trim().length > 0;
  }

  saveRefreshToken(token: string): void {
    localStorage.setItem(this.REFRESH_TOKEN_KEY, token);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  hasRefreshToken(): boolean {
    const token = this.getRefreshToken();
    return !!token && token.trim().length > 0;
  }

  saveTenantSlug(slug: string): void {
    localStorage.setItem(this.TENANT_SLUG_KEY, slug);
  }

  getTenantSlug(): string | null {
    return localStorage.getItem(this.TENANT_SLUG_KEY);
  }

  hasTenantSlug(): boolean {
    const slug = this.getTenantSlug();
    return !!slug && slug.trim().length > 0;
  }

  hasValidTenantSession(): boolean {
    return this.hasToken() && this.hasTenantSlug();
  }

  getSessionKey(): string {
    const token = this.getToken() ?? '';
    const tenantSlug = this.getTenantSlug() ?? '';
    return `${token}|${tenantSlug}`;
  }

  clear(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    localStorage.removeItem(this.TENANT_SLUG_KEY);
  }
}
