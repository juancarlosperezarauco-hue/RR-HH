import { computed, inject, Injectable } from '@angular/core';
import { AuthFacade } from '../auth/auth.facade';
import { PermissionService } from '../auth/permission.service';
import { TenantNavigationItem, TENANT_NAVIGATION } from './tenant-navigation.config';

@Injectable({ providedIn: 'root' })
export class TenantNavigationService {
  private readonly authFacade = inject(AuthFacade);
  private readonly permissions = inject(PermissionService);
  readonly visibleItems = computed(() => TENANT_NAVIGATION.filter(item => this.canAccess(item)));

  canAccess(item: TenantNavigationItem): boolean {
    if (this.authFacade.status() !== 'ready' || !this.authFacade.currentUser()) return false;
    if (item.ownerOnly && !this.permissions.isCurrentOwner()) return false;
    return !item.permission || this.permissions.hasPermission(item.permission);
  }
}
