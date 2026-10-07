import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../../entities/auth/api/auth.service';
import { TenantProfileResponse, UpdateTenantProfileRequest } from '../../../entities/auth';
import { AuthFacade } from '../../../shared/lib/auth/auth.facade';

export interface TenantProfileState {
  status: 'idle' | 'loading' | 'success' | 'error' | 'saving';
  profile: TenantProfileResponse | null;
  error: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class ProfileUseCase {
  private readonly authService = inject(AuthService);
  private readonly authFacade = inject(AuthFacade);

  private readonly state = signal<TenantProfileState>({
    status: 'idle',
    profile: null,
    error: null
  });

  readonly status = computed(() => this.state().status);
  readonly profile = computed(() => this.state().profile);
  readonly error = computed(() => this.state().error);

  async loadProfile(): Promise<void> {
    this.state.update(current => ({ ...current, status: 'loading', error: null }));

    try {
      const response = await firstValueFrom(this.authService.getProfile());

      if (response.success && response.data) {
        this.authFacade.syncCurrentUserProfile(response.data);
        this.state.set({ status: 'success', profile: response.data, error: null });
        return;
      }

      this.state.set({ status: 'error', profile: null, error: response.message || 'Error al cargar perfil' });
    } catch (err: any) {
      this.state.set({ status: 'error', profile: null, error: err.error?.message || err.message || 'Error al cargar perfil' });
    }
  }

  async saveProfile(request: UpdateTenantProfileRequest): Promise<boolean> {
    this.state.update(current => ({ ...current, status: 'saving', error: null }));

    try {
      const response = await firstValueFrom(this.authService.updateProfile(request));
      if (response.success && response.data) {
        this.authFacade.syncCurrentUserProfile(response.data);
        this.state.set({ status: 'success', profile: response.data, error: null });
        return true;
      }

      this.state.update(current => ({ ...current, status: 'error', error: response.message || 'Error al guardar perfil' }));
      return false;
    } catch (err: any) {
      this.state.update(current => ({ ...current, status: 'error', error: err.error?.message || err.message || 'Error al guardar perfil' }));
      return false;
    }
  }

  async removeProfilePhoto(): Promise<boolean> {
    this.state.update(current => ({ ...current, status: 'saving', error: null }));

    try {
      const response = await firstValueFrom(this.authService.removeProfilePhoto());
      if (response.success && response.data) {
        this.authFacade.syncCurrentUserProfile(response.data);
        this.state.set({ status: 'success', profile: response.data, error: null });
        return true;
      }
      this.state.update(current => ({ ...current, status: 'error', error: response.message || 'No se pudo eliminar la foto' }));
      return false;
    } catch (err: any) {
      this.state.update(current => ({ ...current, status: 'error', error: err.error?.message || err.message || 'No se pudo eliminar la foto' }));
      return false;
    }
  }

  resetState(): void {
    this.state.set({ status: 'idle', profile: null, error: null });
  }
}
