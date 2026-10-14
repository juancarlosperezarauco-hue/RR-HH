import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { PermissionService } from '../../lib/auth/permission.service';
import { AuthFacade } from '../../lib/auth/auth.facade';

type TenantRouteScope = 'OWNER_ADMIN' | 'NON_OWNER';

export function tenantRoleGuard(requiredScope: TenantRouteScope, redirectTo: string): CanActivateFn {
  return (
    _route: ActivatedRouteSnapshot,
    _state: RouterStateSnapshot
  ): Promise<boolean | UrlTree> => {
    const permissionService = inject(PermissionService);
    const authFacade = inject(AuthFacade);
    const router = inject(Router);
    const safeRedirect = redirectTo.startsWith('/') ? redirectTo : '/dashboard/profile';
    return authFacade.ensureTenantIdentity().then(identity => {
      if (!identity) return router.createUrlTree(['/login']);
      const isOwner = permissionService.isCurrentOwner();
      return requiredScope === 'OWNER_ADMIN' ? (isOwner ? true : router.createUrlTree([safeRedirect])) : (!isOwner ? true : router.createUrlTree([safeRedirect]));
    });
  };
}
