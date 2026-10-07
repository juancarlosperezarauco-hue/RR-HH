import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { PermissionService } from '../../lib/auth/permission.service';

type TenantRouteScope = 'OWNER_ADMIN' | 'NON_OWNER';

export function tenantRoleGuard(requiredScope: TenantRouteScope, redirectTo: string): CanActivateFn {
  return (
    _route: ActivatedRouteSnapshot,
    _state: RouterStateSnapshot
  ): boolean | UrlTree => {
    const permissionService = inject(PermissionService);
    const router = inject(Router);
    const isOwnerAdmin = permissionService.hasRole('OWNER_ADMIN');

    if (requiredScope === 'OWNER_ADMIN') {
      return isOwnerAdmin ? true : router.createUrlTree([redirectTo]);
    }

    return isOwnerAdmin ? router.createUrlTree([redirectTo]) : true;
  };
}
