import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { AuthFacade } from '../../lib/auth/auth.facade';

export const authGuard: CanActivateFn = (
  _route: ActivatedRouteSnapshot,
  _state: RouterStateSnapshot
): Promise<boolean | UrlTree> => {
  const authFacade = inject(AuthFacade);
  const router = inject(Router);
  return authFacade.ensureTenantIdentity().then(identity => {
    if (identity) return true;
    const reason = authFacade.status() === 'error' ? 'verification_unavailable' : undefined;
    return router.createUrlTree(['/login'], { queryParams: { ...(reason ? { reason } : {}), returnUrl: _state.url } });
  });
};
