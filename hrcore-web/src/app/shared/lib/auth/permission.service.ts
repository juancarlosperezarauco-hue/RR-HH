import { computed, inject, Injectable } from '@angular/core';
import { AuthFacade } from './auth.facade';

@Injectable({ providedIn: 'root' })
export class PermissionService {
  private readonly authFacade = inject(AuthFacade);
  private readonly identity = computed(() => this.authFacade.currentUser());
  private readonly permissions = computed<ReadonlySet<string>>(() => new Set(this.identity()?.permissions ?? []));
  private readonly roles = computed<ReadonlySet<string>>(() => new Set((this.identity()?.roles ?? []).map(role => role.toUpperCase())));
  hasPermission(permissionCode: string): boolean { return !!permissionCode?.trim() && !!this.identity() && this.permissions().has(permissionCode); }
  hasAnyPermission(...permissionCodes: string[]): boolean { return !!this.identity() && permissionCodes.length > 0 && permissionCodes.some(code => this.hasPermission(code)); }
  hasAllPermissions(permissionCodes: string[]): boolean { if (!this.identity()) return false; return permissionCodes.every(code => this.hasPermission(code)); }
  hasRole(roleName: string): boolean { return !!roleName?.trim() && !!this.identity() && this.roles().has(roleName.toUpperCase()); }
  isCurrentOwner(): boolean { return this.identity()?.scope === 'TENANT' && this.identity()?.owner === true; }
  getPermissions(): string[] { return this.identity() ? [...this.permissions()].sort() : []; }
  getRoles(): string[] { return this.identity() ? [...this.roles()].sort() : []; }
}
