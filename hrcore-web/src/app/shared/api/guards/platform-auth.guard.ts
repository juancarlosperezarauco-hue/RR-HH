import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { PlatformFacade } from '../../../features/platform/lib/platform.facade';

export const platformAuthGuard: CanActivateFn = (
  _route: ActivatedRouteSnapshot,
  _state: RouterStateSnapshot
): Promise<boolean | UrlTree> => {
  const router = inject(Router);
  const platformFacade = inject(PlatformFacade);
  return platformFacade.ensurePlatformIdentity().then(identity => identity
    ? true
    : router.createUrlTree(['/platform/login'], { queryParams: { reason: 'verification_unavailable', returnUrl: _state.url } }));
};
