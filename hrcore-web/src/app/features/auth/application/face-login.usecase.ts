import { Injectable, inject, signal, computed } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../../entities/auth/api/auth.service';
import { AuthStorageService } from '../../../shared/lib/storage/auth-storage.service';
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
  private readonly authStorage = inject(AuthStorageService);

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
      console.log('face login response', response);

      if (response.success && response.data) {
        this.authStorage.saveToken(response.data.accessToken);
        if (response.data.refreshToken) {
          this.authStorage.saveRefreshToken(response.data.refreshToken);
        }
        this.authStorage.saveTenantSlug(request.tenantSlug.trim());
        this.state.set({ status: 'success', error: null });
        return;
      }

      this.state.set({ status: 'error', error: response.message || 'Error desconocido al iniciar sesión con rostro' });
    } catch (err: any) {
      const errorMsg = err.error?.message || err.message || 'Error al conectar con el servidor';
      this.state.set({ status: 'error', error: errorMsg });
    }
  }

  resetState(): void {
    this.state.set({ status: 'idle', error: null });
  }
}
