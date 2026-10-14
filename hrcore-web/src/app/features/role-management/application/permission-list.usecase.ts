import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AccessService, PageResponse, SystemPermissionResponse } from '../../../entities/access';

export interface PermissionListState { status: 'idle' | 'loading' | 'success' | 'error'; page: PageResponse<SystemPermissionResponse> | null; error: string | null; }

@Injectable({ providedIn: 'root' })
export class PermissionListUseCase {
  private readonly accessService = inject(AccessService);
  private readonly state = signal<PermissionListState>({ status: 'idle', page: null, error: null });
  readonly status = computed(() => this.state().status);
  readonly page = computed(() => this.state().page);
  readonly data = computed(() => this.state().page?.content ?? []);
  readonly tenantPermissions = computed(() => this.data().filter(permission => permission.scope === 'TENANT'));
  readonly assignablePermissions = computed(() => this.tenantPermissions().filter(permission => permission.customRoleAssignable));
  readonly reservedPermissions = computed(() => this.tenantPermissions().filter(permission => !permission.customRoleAssignable));
  readonly error = computed(() => this.state().error);

  async loadPermissions(_page = 0, size = 200, force = false): Promise<void> {
    if (!force && this.status() === 'success') return;
    this.state.set({ status: 'loading', page: null, error: null });
    try {
      const content: SystemPermissionResponse[] = [];
      let page = 0;
      let last = false;
      let lastPage: PageResponse<SystemPermissionResponse> | null = null;
      while (!last) {
        const response = await firstValueFrom(this.accessService.getPermissions(page, size));
        if (!response.success || !response.data) throw new Error(response.message || 'No se pudieron cargar los permisos');
        if (response.data.content.some(permission => permission.scope !== 'TENANT' && permission.scope !== 'PLATFORM')) throw new Error('El catálogo devolvió un ámbito inválido');
        content.push(...response.data.content);
        lastPage = response.data;
        last = response.data.last || response.data.content.length === 0;
        page += 1;
      }
      if (!lastPage) throw new Error('El catálogo de permisos está vacío o incompleto');
      this.state.set({ status: 'success', page: { ...lastPage, content, number: 0, first: true, last: true, totalElements: content.length, totalPages: 1, numberOfElements: content.length }, error: null });
    } catch (err: any) {
      this.state.set({ status: 'error', page: null, error: err.error?.message || err.message || 'Error al conectar con el servidor' });
    }
  }

  invalidate(): void { this.state.set({ status: 'idle', page: null, error: null }); }
}
