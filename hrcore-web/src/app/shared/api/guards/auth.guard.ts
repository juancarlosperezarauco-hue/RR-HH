import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { AuthStorageService } from '../../lib/storage/auth-storage.service';

export const authGuard: CanActivateFn = (
  _route: ActivatedRouteSnapshot,
  _state: RouterStateSnapshot
): boolean | UrlTree => {
  const authStorage = inject(AuthStorageService);
  const router = inject(Router);

  const hasTenantSession =
    !!authStorage.getToken()?.trim() &&
    !!authStorage.getTenantSlug()?.trim();

  if (!hasTenantSession) {
    return router.createUrlTree(['/login']);
  }

  return true;
};
