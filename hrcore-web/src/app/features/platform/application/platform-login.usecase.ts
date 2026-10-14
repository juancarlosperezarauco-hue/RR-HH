import { Injectable, inject, signal, computed } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { PlatformService } from '../../../entities/platform/api/platform.service';
import { SessionCoordinatorService } from '../../../shared/lib/auth/session-coordinator.service';

export interface PlatformLoginState {
  status: 'idle' | 'loading' | 'success' | 'error';
  error: string | null;
}

@Injectable({ providedIn: 'root' })
export class PlatformLoginUseCase {
  private readonly platformService = inject(PlatformService);
  private readonly sessions = inject(SessionCoordinatorService);

  private readonly state = signal<PlatformLoginState>({ status: 'idle', error: null });

  readonly status = computed(() => this.state().status);
  readonly error = computed(() => this.state().error);

  async login(credentials: { email: string; password: string }): Promise<void> {
    this.state.set({ status: 'loading', error: null });

    try {
      const response = await firstValueFrom(this.platformService.login(credentials));

      if (response.success && response.data) {
        await this.sessions.commitLogin('PLATFORM', response.data);
        this.state.set({ status: 'success', error: null });
      } else {
        this.state.set({ status: 'error', error: response.message || 'Error al iniciar sesión' });
      }
    } catch (err: any) {
      const errorMsg = err.error?.message || err.message || 'Error al conectar con el servidor';
      this.state.set({ status: 'error', error: errorMsg });
    }
  }

  resetState(): void {
    this.state.set({ status: 'idle', error: null });
  }
}
