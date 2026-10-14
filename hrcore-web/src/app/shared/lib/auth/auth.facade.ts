import { inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthStorageService } from '../storage/auth-storage.service';
import { AuthService } from '../../../entities/auth/api/auth.service';
import { SessionCoordinatorService } from './session-coordinator.service';
import { AuthenticatedTenantUserResponse } from '../../../entities/auth/model/authenticated-tenant-user-response.model';
import { TenantProfileResponse } from '../../../entities/auth/model/tenant-profile-response.model';

@Injectable({
  providedIn: 'root'
})
export class AuthFacade {
  private readonly authStorage = inject(AuthStorageService);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly sessions = inject(SessionCoordinatorService);
  readonly currentUser = signal<AuthenticatedTenantUserResponse | null>(null);
  readonly status = signal<'idle' | 'loading' | 'ready' | 'error'>('idle');
  private loadPromise: Promise<AuthenticatedTenantUserResponse | null> | null = null;

  constructor() {
    if (typeof window !== 'undefined') {
      window.addEventListener('storage', event => {
        if (event.key === 'hrcore.auth.tenant.session.v2') {
          this.currentUser.set(null);
          this.status.set('idle');
        }
      });
    }
    if (this.authStorage.hasValidTenantSession()) {
      void this.loadCurrentUser();
    }
  }

  syncCurrentUserProfile(profile: TenantProfileResponse | null): void {
    if (!profile) {
      return;
    }

    const currentUser = this.currentUser();
    if (!currentUser || currentUser.id !== profile.id || currentUser.tenantSlug !== profile.tenantSlug || this.status() !== 'ready') return;
    this.currentUser.set({
      ...currentUser,
      email: profile.email ?? currentUser.email,
      firstName: profile.firstName ?? currentUser.firstName,
      lastName: profile.lastName ?? currentUser.lastName,
      active: profile.active ?? currentUser.active,
      status: profile.status ?? currentUser.status,
      profilePhotoUrl: profile.profilePhotoAvailable ? profile.profilePhotoUrl : null,
      profilePhotoContentType: profile.profilePhotoAvailable ? profile.profilePhotoContentType : null,
      updatedAt: profile.updatedAt ?? currentUser.updatedAt ?? null
    });
  }

  /** Carga la información del usuario actual autenticado. */
  async loadCurrentUser(): Promise<AuthenticatedTenantUserResponse | null> {
    if (this.loadPromise) return this.loadPromise;
    this.loadPromise = this.loadCurrentUserInternal();
    try { return await this.loadPromise; } finally { this.loadPromise = null; }
  }

  async ensureTenantIdentity(): Promise<AuthenticatedTenantUserResponse | null> {
    if (!this.authStorage.hasValidTenantSession()) return null;
    if (this.currentUser() && this.currentUser()?.tenantSlug !== this.authStorage.getTenantSlug()) {
      this.currentUser.set(null);
      this.status.set('idle');
    }
    if (this.status() === 'ready' && this.currentUser()) return this.currentUser();
    return this.loadCurrentUser();
  }

  private async loadCurrentUserInternal(): Promise<AuthenticatedTenantUserResponse | null> {
    if (!this.authStorage.hasValidTenantSession()) {
      this.currentUser.set(null);
      this.status.set('idle');
      return null;
    }

    this.status.set('loading');

    try {
      const [meResult, profileResult] = await Promise.allSettled([
        this.sessions.loadIdentity('TENANT'),
        firstValueFrom(this.authService.getProfile())
      ]);

      if (meResult.status === 'fulfilled' && meResult.value) {
        const currentUser = meResult.value;
        const profile = profileResult.status === 'fulfilled' && profileResult.value.success ? profileResult.value.data : null;

        this.currentUser.set({
          ...currentUser,
          profilePhotoUrl: profile ? (profile.profilePhotoAvailable ? profile.profilePhotoUrl : null) : currentUser.profilePhotoUrl ?? null,
          profilePhotoContentType: profile ? (profile.profilePhotoAvailable ? profile.profilePhotoContentType : null) : currentUser.profilePhotoContentType ?? null,
          updatedAt: profile?.updatedAt ?? currentUser.updatedAt ?? null
        });
        this.status.set('ready');
        return currentUser;
      }

      this.currentUser.set(null);
      this.status.set('error');
      return null;
    } catch (err) {
      console.error('Error loading current user', err);
      this.currentUser.set(null);
      this.status.set('error');
      return null;
    }
  }

  getTenantLandingRoute(): string {
    return '/dashboard/home';
  }

  /** Cierra la sesión del usuario actual limpiando el almacenamiento y redirigiendo a la vista de login. */
  async logout(): Promise<void> {
    const result = await this.sessions.logout('TENANT');
    this.currentUser.set(null);
    this.status.set('idle');
    if (!result.remoteRevoked) this.status.set('error');
    await this.router.navigate(['/login'], { replaceUrl: true });
  }
}
