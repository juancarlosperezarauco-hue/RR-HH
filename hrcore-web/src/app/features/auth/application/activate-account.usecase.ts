import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../../entities/auth/api/auth.service';
import { ActivateAccountRequest } from '../../../entities/auth/model/activate-account-request.model';
import { ResendActivationRequest } from '../../../entities/auth/model/resend-activation-request.model';

export interface ActivateAccountState {
  status: 'idle' | 'loading' | 'success' | 'error';
  error: string | null;
  message: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class ActivateAccountUseCase {
  private readonly authService = inject(AuthService);

  private readonly state = signal<ActivateAccountState>({
    status: 'idle',
    error: null,
    message: null
  });

  readonly status = computed(() => this.state().status);
  readonly error = computed(() => this.state().error);
  readonly message = computed(() => this.state().message);

  async activate(request: ActivateAccountRequest): Promise<void> {
    this.state.set({ status: 'loading', error: null, message: null });

    try {
      const response = await firstValueFrom(this.authService.activateAccount(request));

      if (response.success) {
        this.state.set({
          status: 'success',
          error: null,
          message: response.message || 'Tu cuenta fue activada correctamente.'
        });
        return;
      }

      this.state.set({
        status: 'error',
        error: response.message || 'No fue posible activar la cuenta.',
        message: null
      });
    } catch (err: any) {
      const errorMsg = err.error?.message || err.message || 'Error al conectar con el servidor';
      this.state.set({
        status: 'error',
        error: errorMsg,
        message: null
      });
    }
  }

  async resend(request: ResendActivationRequest): Promise<void> {
    this.state.set({ status: 'loading', error: null, message: null });

    try {
      const response = await firstValueFrom(this.authService.resendActivation(request));

      this.state.set({
        status: 'success',
        error: null,
        message: response.message || 'Si la cuenta está pendiente, enviamos un nuevo enlace de activación.'
      });
    } catch (err: any) {
      const errorMsg = err.error?.message || err.message || 'Error al conectar con el servidor';
      this.state.set({
        status: 'error',
        error: errorMsg,
        message: null
      });
    }
  }

  resetState(): void {
    this.state.set({ status: 'idle', error: null, message: null });
  }
}
