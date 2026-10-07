import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { PlatformStorageService } from '../../../features/platform/lib/platform-storage.service';

export const platformAuthGuard: CanActivateFn = (
  _route: ActivatedRouteSnapshot,
  _state: RouterStateSnapshot
): boolean | UrlTree => {
  const router = inject(Router);
  const platformStorage = inject(PlatformStorageService);

  const hasPlatformSession =
    !!platformStorage.getAccessToken()?.trim() ||
    !!platformStorage.getRefreshToken()?.trim();

  if (hasPlatformSession) {
    return true;
  }

  return router.createUrlTree(['/platform/login']);
};
