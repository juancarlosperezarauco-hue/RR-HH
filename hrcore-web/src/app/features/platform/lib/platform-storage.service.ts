import { Injectable } from '@angular/core';
import { AuthenticatedPlatformSuperadminResponse } from '../../../entities/platform/api/platform.service';

@Injectable({ providedIn: 'root' })
export class PlatformStorageService {
  private readonly TOKEN_KEY = 'platform_access_token';
  private readonly REFRESH_TOKEN_KEY = 'platform_refresh_token';
  private readonly USER_KEY = 'platform_user';

  saveAccessToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
  }

  getAccessToken(): string | null {
    const token = localStorage.getItem(this.TOKEN_KEY);
    return token && token.trim().length > 0 ? token : null;
  }

  hasAccessToken(): boolean {
    return this.getAccessToken() !== null;
  }

  saveRefreshToken(token: string): void {
    localStorage.setItem(this.REFRESH_TOKEN_KEY, token);
  }

  getRefreshToken(): string | null {
    const token = localStorage.getItem(this.REFRESH_TOKEN_KEY);
    return token && token.trim().length > 0 ? token : null;
  }

  hasRefreshToken(): boolean {
    return this.getRefreshToken() !== null;
  }

  saveUser(user: AuthenticatedPlatformSuperadminResponse): void {
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
  }

  getUser(): AuthenticatedPlatformSuperadminResponse | null {
    const user = localStorage.getItem(this.USER_KEY);
    if (!user) {
      return null;
    }

    try {
      return JSON.parse(user) as AuthenticatedPlatformSuperadminResponse;
    } catch {
      this.clearSession();
      return null;
    }
  }

  clearSession(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
  }

  saveToken(token: string): void {
    this.saveAccessToken(token);
  }

  getToken(): string | null {
    return this.getAccessToken();
  }

  clear(): void {
    this.clearSession();
  }
}
