import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { PermissionService } from '../../lib/auth/permission.service';
import { AuthFacade } from '../../lib/auth/auth.facade';

/**
 * Genera un CanActivateFn Guard que verifica que el usuario tenga
 * AL MENOS UNO de los permisos requeridos para acceder a la ruta.
 *
 * Uso en app.routes.ts:
 *   { path: 'accounts', canActivate: [permissionGuard('accounts.admin.read', 'accounts.create')], ... }
 *   { path: 'transactions', canActivate: [permissionGuard('transactions.admin.read')], ... }
 */
export function permissionGuard(...requiredPermissions: string[]): CanActivateFn {
  return (
    _route: ActivatedRouteSnapshot,
    _state: RouterStateSnapshot
  ): Promise<boolean | UrlTree> => {
    const permissionService = inject(PermissionService);
    const authFacade = inject(AuthFacade);
    const router = inject(Router);
    return authFacade.ensureTenantIdentity().then(identity => {
      if (!identity) return router.createUrlTree(['/login'], { queryParams: { returnUrl: _state.url } });
      return requiredPermissions.length > 0 && permissionService.hasAnyPermission(...requiredPermissions)
        ? true
        : router.createUrlTree(['/dashboard/access-denied'], { queryParams: { from: _state.url } });
    });
  };
}
