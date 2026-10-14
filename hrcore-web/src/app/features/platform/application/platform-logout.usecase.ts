import { Injectable, inject, signal, computed } from '@angular/core';
import { SessionCoordinatorService } from '../../../shared/lib/auth/session-coordinator.service';
import { PlatformSuperadminMeUseCase } from './platform-superadmin-me.usecase';

export interface PlatformLogoutState {
  status: 'idle' | 'loading' | 'success' | 'error';
  error: string | null;
}

@Injectable({ providedIn: 'root' })
export class PlatformLogoutUseCase {
  private readonly sessions = inject(SessionCoordinatorService);
  private readonly superadminMe = inject(PlatformSuperadminMeUseCase);

  private readonly state = signal<PlatformLogoutState>({ status: 'idle', error: null });

  readonly status = computed(() => this.state().status);
  readonly error = computed(() => this.state().error);

  async logout(): Promise<void> {
    this.state.set({ status: 'loading', error: null });

    try {
      const result = await this.sessions.logout('PLATFORM');
      this.superadminMe.resetState();
      this.state.set({ status: 'success', error: result.remoteRevoked ? null : 'La sesión se cerró, pero no se pudo confirmar la revocación en el servidor.' });
    } catch (err: any) {
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
