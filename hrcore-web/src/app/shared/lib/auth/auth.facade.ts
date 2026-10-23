import { inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthStorageService } from '../storage/auth-storage.service';
import { AuthService } from '../../../entities/auth/api/auth.service';
import { SessionCoordinatorService } from './session-coordinator.service';
import { AuthenticatedTenantUserResponse } from '../../../entities/auth/model/authenticated-tenant-user-response.model';
import { TenantProfileResponse } from '../../../entities/auth/model/tenant-profile-response.model';
import { PushNotificationsService } from '../../../entities/notifications';

@Injectable({
  providedIn: 'root'
})
export class AuthFacade {
  private readonly authStorage = inject(AuthStorageService);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly pushNotifications = inject(PushNotificationsService);
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
      // La identidad es la única fuente de verdad para autorizar la sesión.
      // El perfil se carga después, de forma opcional, para no competir con
      // /me durante la restauración de una sesión al recargar la página.
      const currentUser = await this.sessions.loadIdentity('TENANT');
      if (currentUser) {
        let profile: TenantProfileResponse | null = null;
        try {
          const profileResult = await firstValueFrom(this.authService.getProfile());
          profile = profileResult.success ? profileResult.data : null;
        } catch {
          // La identidad ya fue validada; un fallo del perfil no debe cerrar
          // sesión ni cancelar la navegación inicial.
        }

        this.currentUser.set({
          ...currentUser,
          profilePhotoUrl: profile ? (profile.profilePhotoAvailable ? profile.profilePhotoUrl : null) : currentUser.profilePhotoUrl ?? null,
          profilePhotoContentType: profile ? (profile.profilePhotoAvailable ? profile.profilePhotoContentType : null) : currentUser.profilePhotoContentType ?? null,
          updatedAt: profile?.updatedAt ?? currentUser.updatedAt ?? null
        });
        this.status.set('ready');
        void this.pushNotifications.initialize().catch(() => undefined);
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

  hasStoredTenantSession(): boolean {
    return this.authStorage.hasValidTenantSession();
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
