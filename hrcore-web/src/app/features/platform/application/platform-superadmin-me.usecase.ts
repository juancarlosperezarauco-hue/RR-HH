// features/platform/application/platform-superadmin-me.usecase.ts
import { Injectable, inject, signal, computed } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuthenticatedPlatformSuperadminResponse, PlatformService } from '../../../entities/platform/api/platform.service';
import { PlatformStorageService } from '../lib/platform-storage.service';

export interface PlatformSuperadminState {
  status: 'idle' | 'loading' | 'success' | 'error';
  superadmin: AuthenticatedPlatformSuperadminResponse | null;
  error: string | null;
}

@Injectable({ providedIn: 'root' })
export class PlatformSuperadminMeUseCase {
  private platformService = inject(PlatformService);
  private platformStorage = inject(PlatformStorageService);

  private state = signal<PlatformSuperadminState>({
    status: 'idle',
    superadmin: null,
    error: null
  });

  readonly status = computed(() => this.state().status);
  readonly superadmin = computed(() => this.state().superadmin);
  readonly error = computed(() => this.state().error);

  async loadCurrentSuperadmin(): Promise<void> {
    this.state.set({ status: 'loading', superadmin: null, error: null });

    try {
      const response = await firstValueFrom(this.platformService.getSuperadminMe());
      if (response.success && response.data) {
        this.platformStorage.saveUser(response.data);
        this.state.set({ status: 'success', superadmin: response.data, error: null });
      } else {
        this.state.set({ status: 'error', superadmin: null, error: response.message });
      }
    } catch (err: any) {
      this.state.set({ status: 'error', superadmin: null, error: err.message || 'Error al cargar perfil' });
    }
  }

  resetState(): void {
    this.state.set({ status: 'idle', superadmin: null, error: null });
  }
}
