import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../../entities/auth/api/auth.service';
import { ForgotPasswordRequest } from '../../../entities/auth/model/forgot-password-request.model';

export interface ForgotPasswordState {
  status: 'idle' | 'loading' | 'success' | 'error';
  error: string | null;
  message: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class ForgotPasswordUseCase {
  private readonly authService = inject(AuthService);

  private readonly state = signal<ForgotPasswordState>({
    status: 'idle',
    error: null,
    message: null
  });

  readonly status = computed(() => this.state().status);
  readonly error = computed(() => this.state().error);
  readonly message = computed(() => this.state().message);

  async send(request: ForgotPasswordRequest): Promise<void> {
    this.state.set({ status: 'loading', error: null, message: null });

    try {
      const response = await firstValueFrom(this.authService.forgotPassword(request));
      console.log('forgot password response', response);

      if (response.success) {
        this.state.set({
          status: 'success',
          error: null,
          message: response.message || 'Revisa tu correo para continuar con el restablecimiento.'
        });
        return;
      }

      this.state.set({
        status: 'error',
        error: response.message || 'No fue posible solicitar el restablecimiento de contraseña.',
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

  resetState(): void {
    this.state.set({ status: 'idle', error: null, message: null });
  }
}
