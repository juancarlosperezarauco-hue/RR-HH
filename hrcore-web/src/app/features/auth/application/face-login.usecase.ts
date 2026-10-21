import { Injectable, inject, signal, computed } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../../entities/auth/api/auth.service';
import { SessionCoordinatorService } from '../../../shared/lib/auth/session-coordinator.service';
import { FaceLoginRequest } from '../../../entities/auth/model/face-login-request.model';

export interface FaceLoginState {
  status: 'idle' | 'loading' | 'success' | 'error';
  error: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class FaceLoginUseCase {
  private readonly authService = inject(AuthService);
  private readonly sessions = inject(SessionCoordinatorService);

  private readonly state = signal<FaceLoginState>({
    status: 'idle',
    error: null
  });

  readonly status = computed(() => this.state().status);
  readonly error = computed(() => this.state().error);

  async login(request: FaceLoginRequest): Promise<void> {
    this.state.set({ status: 'loading', error: null });

    try {
      const response = await firstValueFrom(this.authService.faceLogin(request));
      if (response.success && response.data) {
        await this.sessions.commitLogin('TENANT', response.data, request.tenantSlug.trim());
        this.state.set({ status: 'success', error: null });
        return;
      }

      this.state.set({ status: 'error', error: response.message || 'Error desconocido al iniciar sesión con rostro' });
    } catch (err: any) {
      const serverMessage =
        err.error?.message ||
        (Array.isArray(err.error?.errors) && err.error.errors[0]) ||
        err.message ||
        '';

      if (
        err.status === 403 ||
        /desactivada|suspendida|not active|cuenta de empresa/i.test(serverMessage)
      ) {
        this.state.set({
          status: 'error',
          error: 'Tu cuenta de empresa fue desactivada por el momento. Comunícate con el superadministrador de la plataforma.'
        });
        return;
      }

      const errorMsg = serverMessage || 'Error al conectar con el servidor';
      this.state.set({ status: 'error', error: errorMsg });
    }
  }

  resetState(): void {
    this.state.set({ status: 'idle', error: null });
  }
}
