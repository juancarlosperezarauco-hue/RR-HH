import { inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthStorageService } from '../storage/auth-storage.service';
import { AuthService } from '../../../entities/auth/api/auth.service';
import { AuthenticatedTenantUserResponse } from '../../../entities/auth/model/authenticated-tenant-user-response.model';
import { TenantProfileResponse } from '../../../entities/auth/model/tenant-profile-response.model';

@Injectable({
  providedIn: 'root'
})
export class AuthFacade {
  private readonly authStorage = inject(AuthStorageService);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  readonly currentUser = signal<AuthenticatedTenantUserResponse | null>(null);
  readonly status = signal<'idle' | 'loading' | 'ready' | 'error'>('idle');

  constructor() {
    if (this.authStorage.hasValidTenantSession()) {
      void this.loadCurrentUser();
    }
  }

  syncCurrentUserProfile(profile: TenantProfileResponse | null): void {
    if (!profile) {
      return;
    }

    const currentUser = this.currentUser();
    this.currentUser.set({
      id: currentUser?.id ?? profile.id,
      email: profile.email ?? currentUser?.email ?? '',
      firstName: profile.firstName ?? currentUser?.firstName ?? '',
      lastName: profile.lastName ?? currentUser?.lastName ?? '',
      active: profile.active ?? currentUser?.active ?? true,
      status: profile.status ?? currentUser?.status ?? 'ACTIVE',
      tenantSlug: profile.tenantSlug ?? currentUser?.tenantSlug ?? '',
      roles: currentUser?.roles ?? [],
      profilePhotoUrl: profile.profilePhotoAvailable ? profile.profilePhotoUrl : null,
      profilePhotoContentType: profile.profilePhotoAvailable ? profile.profilePhotoContentType : null,
      updatedAt: profile.updatedAt ?? currentUser?.updatedAt ?? null
    });
  }

  /** Carga la información del usuario actual autenticado. */
  async loadCurrentUser(): Promise<void> {
    if (!this.authStorage.hasValidTenantSession()) {
      this.currentUser.set(null);
      this.status.set('idle');
      return;
    }

    this.status.set('loading');

    try {
      const [meResult, profileResult] = await Promise.allSettled([
        firstValueFrom(this.authService.getMe()),
        firstValueFrom(this.authService.getProfile())
      ]);

      if (meResult.status === 'fulfilled' && meResult.value.success && meResult.value.data) {
        const currentUser = meResult.value.data;
        const profile = profileResult.status === 'fulfilled' && profileResult.value.success ? profileResult.value.data : null;

        this.currentUser.set({
          ...currentUser,
          profilePhotoUrl: profile ? (profile.profilePhotoAvailable ? profile.profilePhotoUrl : null) : currentUser.profilePhotoUrl ?? null,
          profilePhotoContentType: profile ? (profile.profilePhotoAvailable ? profile.profilePhotoContentType : null) : currentUser.profilePhotoContentType ?? null,
          updatedAt: profile?.updatedAt ?? currentUser.updatedAt ?? null
        });
        this.status.set('ready');
        return;
      }

      this.currentUser.set(null);
      this.status.set('error');
    } catch (err) {
      console.error('Error loading current user', err);
      this.currentUser.set(null);
      this.status.set('error');
    }
  }

  getTenantLandingRoute(): string {
    return '/dashboard/users';
  }

  /** Cierra la sesión del usuario actual limpiando el almacenamiento y redirigiendo a la vista de login. */
  logout(): void {
    // Limpiar almacenamiento persistente
    this.authStorage.clear();
    
    // Limpiar el estado de usuario
    this.currentUser.set(null);
    this.status.set('idle');
    
    // Redirigir al inicio de sesión
    this.router.navigate(['/login']);
  }
}
