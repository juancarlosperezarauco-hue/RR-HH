import { Injectable, inject, signal, computed } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { PageResponse, TenantUserResponse, UserService } from '../../../entities/user';

export interface UserListState {
  status: 'idle' | 'loading' | 'success' | 'error';
  page: PageResponse<TenantUserResponse> | null;
  error: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class UserListUseCase {
  private readonly userService = inject(UserService);

  private readonly state = signal<UserListState>({
    status: 'idle',
    page: null,
    error: null
  });

  // Selectores
  readonly status = computed(() => this.state().status);
  readonly page = computed(() => this.state().page);
  readonly data = computed(() => this.state().page?.content ?? []);
  readonly error = computed(() => this.state().error);

  async loadUsers(page = 0, size = 20): Promise<void> {
    this.state.set({ status: 'loading', page: this.state().page, error: null });

    try {
      const response = await firstValueFrom(this.userService.getUsers(page, size));

      if (response.success && response.data) {
        this.state.set({ 
          status: 'success', 
          page: response.data, 
          error: null 
        });
      } else {
        this.state.set({ 
          status: 'error', 
          page: null, 
          error: response.message || 'No se pudieron cargar los usuarios' 
        });
      }
    } catch (err: any) {
      const errorMsg = err.error?.message || err.message || 'Error al conectar con el servidor';
      this.state.set({ status: 'error', page: null, error: errorMsg });
    }
  }

  async reloadUsers(): Promise<void> {
    // Al recargar, mantenemos la data actual pero cambiamos el status para mostrar algún indicador si se desea
    // o simplemente actualizamos silenciosamente
    this.state.set({ status: 'loading', page: this.state().page, error: null });

    try {
      const response = await firstValueFrom(this.userService.getUsers(this.state().page?.number ?? 0, this.state().page?.size ?? 20));

      if (response.success && response.data) {
        this.state.set({ 
          status: 'success', 
          page: response.data, 
          error: null 
        });
      } else {
        this.state.set({ 
          status: 'error', 
          page: this.state().page, 
          error: response.message || 'No se pudieron recargar los usuarios' 
        });
      }
    } catch (err: any) {
      const errorMsg = err.error?.message || err.message || 'Error al conectar con el servidor';
      this.state.set({ status: 'error', page: this.state().page, error: errorMsg });
    }
  }

  resetState(): void {
    this.state.set({ status: 'idle', page: null, error: null });
  }
}
