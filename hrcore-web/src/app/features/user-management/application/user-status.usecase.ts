import { Injectable, inject, signal, computed } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { UserService } from '../../../entities/user';

export interface UserStatusState {
  status: 'idle' | 'loading' | 'success' | 'error';
  error: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class UserStatusUseCase {
  private readonly userService = inject(UserService);

  private readonly state = signal<UserStatusState>({
    status: 'idle',
    error: null
  });

  readonly status = computed(() => this.state().status);
  readonly error = computed(() => this.state().error);

  async toggleStatus(id: string, currentlyActive: boolean): Promise<boolean> {
    this.state.set({ status: 'loading', error: null });

    try {
      const request = currentlyActive 
        ? this.userService.deactivateUser(id)
        : this.userService.activateUser(id);

      const response = await firstValueFrom(request);

      if (response.success) {
        this.state.set({ status: 'success', error: null });
        return true;
      } else {
        this.state.set({ 
          status: 'error', 
          error: response.message || 'Error al cambiar estado del usuario' 
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
