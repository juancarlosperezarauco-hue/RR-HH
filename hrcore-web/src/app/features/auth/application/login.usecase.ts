import { Injectable, inject, signal, computed } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { LoginRequest } from '../../../entities/auth/model/login-request.model';
import { AuthService } from '../../../entities/auth/api/auth.service';
import { AuthStorageService } from '../../../shared/lib/storage/auth-storage.service';

export interface LoginState {
  status: 'idle' | 'loading' | 'success' | 'error';
  error: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class LoginUseCase {
  private readonly authService = inject(AuthService);
  private readonly authStorage = inject(AuthStorageService);

  private readonly state = signal<LoginState>({
    status: 'idle',
    error: null
  });

  readonly status = computed(() => this.state().status);
  readonly error = computed(() => this.state().error);

  async login(request: LoginRequest): Promise<void> {
    this.state.set({ status: 'loading', error: null });

    try {
      const response = await firstValueFrom(this.authService.login(request));
      console.log('login response', response);

      if (response.success && response.data) {
        // Almacenar token, refresh y tenant slug en persistencia local centralizada
        this.authStorage.saveToken(response.data.accessToken);
        if (response.data.refreshToken) {
          this.authStorage.saveRefreshToken(response.data.refreshToken);
        }
        const tenantSlug = request.tenantSlug?.trim();
        if (tenantSlug) {
          this.authStorage.saveTenantSlug(tenantSlug);
        }

        this.state.set({ status: 'success', error: null });
        console.log("login success!!!")
      } else {
        this.state.set({ status: 'error', error: response.message || 'Error desconocido al iniciar sesión' });
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
