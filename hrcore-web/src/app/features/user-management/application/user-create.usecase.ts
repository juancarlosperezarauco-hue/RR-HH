import { Injectable, inject, signal, computed } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { UserService, CreateTenantUserRequest } from '../../../entities/user';

export interface UserCreateState {
  status: 'idle' | 'loading' | 'success' | 'error';
  error: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class UserCreateUseCase {
  private readonly userService = inject(UserService);

  private readonly state = signal<UserCreateState>({
    status: 'idle',
    error: null
  });

  readonly status = computed(() => this.state().status);
  readonly error = computed(() => this.state().error);

  async createUser(request: CreateTenantUserRequest): Promise<void> {
    this.state.set({ status: 'loading', error: null });

    try {
      const response = await firstValueFrom(this.userService.createUser(request));

      if (response.success && response.data) {
        this.state.set({ status: 'success', error: null });
      } else {
        this.state.set({ 
          status: 'error', 
          error: response.message || 'Error al crear el usuario' 
        });
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
