import { Injectable, inject, signal, computed } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { PlatformService } from '../../../entities/platform/api/platform.service';
import { PlatformStorageService } from '../lib/platform-storage.service';

export interface PlatformLogoutState {
  status: 'idle' | 'loading' | 'success' | 'error';
  error: string | null;
}

@Injectable({ providedIn: 'root' })
export class PlatformLogoutUseCase {
  private readonly platformService = inject(PlatformService);
  private readonly platformStorage = inject(PlatformStorageService);

  private readonly state = signal<PlatformLogoutState>({ status: 'idle', error: null });

  readonly status = computed(() => this.state().status);
  readonly error = computed(() => this.state().error);

  async logout(): Promise<void> {
    this.state.set({ status: 'loading', error: null });

    try {
      await firstValueFrom(this.platformService.logout());
      this.platformStorage.clearSession();
      this.state.set({ status: 'success', error: null });
    } catch (err: any) {
      this.platformStorage.clearSession();
      this.state.set({
        status: 'error',
        error: err.error?.message || err.message || 'Error al cerrar sesión'
      });
    }
  }

  resetState(): void {
    this.state.set({ status: 'idle', error: null });
  }
}
