import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../../entities/auth/api/auth.service';
import { ResetPasswordRequest } from '../../../entities/auth/model/reset-password-request.model';

export interface ResetPasswordState {
  status: 'idle' | 'loading' | 'success' | 'error';
  error: string | null;
  message: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class ResetPasswordUseCase {
  private readonly authService = inject(AuthService);

  private readonly state = signal<ResetPasswordState>({
    status: 'idle',
    error: null,
    message: null
  });

  readonly status = computed(() => this.state().status);
  readonly error = computed(() => this.state().error);
  readonly message = computed(() => this.state().message);

  async reset(request: ResetPasswordRequest): Promise<void> {
    this.state.set({ status: 'loading', error: null, message: null });

    try {
      const response = await firstValueFrom(this.authService.resetPassword(request));
      console.log('reset password response', response);

      if (response.success) {
        this.state.set({
          status: 'success',
          error: null,
          message: response.message || 'Tu contraseña fue actualizada correctamente.'
        });
        return;
      }

      this.state.set({
        status: 'error',
        error: response.message || 'No fue posible restablecer la contraseña.',
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
