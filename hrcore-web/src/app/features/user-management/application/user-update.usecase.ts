import { Injectable, inject, signal, computed } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { UserService, UpdateTenantUserRequest } from '../../../entities/user';

export interface UserUpdateState {
  status: 'idle' | 'loading' | 'success' | 'error';
  error: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class UserUpdateUseCase {
  private readonly userService = inject(UserService);

  private readonly state = signal<UserUpdateState>({
    status: 'idle',
    error: null
  });

  readonly status = computed(() => this.state().status);
  readonly error = computed(() => this.state().error);

  async updateUser(id: string, request: UpdateTenantUserRequest): Promise<boolean> {
    this.state.set({ status: 'loading', error: null });

    try {
      const response = await firstValueFrom(this.userService.updateUser(id, request));

      if (response.success) {
        this.state.set({ status: 'success', error: null });
        return true;
      } else {
        this.state.set({ 
          status: 'error', 
          error: response.message || 'Error al actualizar el usuario' 
        });
        return false;
      }
    } catch (err: any) {
      const errorMsg = err.error?.message || err.message || 'Error al conectar con el servidor';
      this.state.set({ status: 'error', error: errorMsg });
      return false;
    }
  }

  resetState(): void {
    this.state.set({ status: 'idle', error: null });
  }
}
