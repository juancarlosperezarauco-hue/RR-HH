import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthFacade } from '../../lib/auth/auth.facade';
import { CompanySetupStateService } from '../../lib/company/company-setup-state.service';
import { PermissionService } from '../../lib/auth/permission.service';

export const tenantSetupGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthFacade);
  const setup = inject(CompanySetupStateService);
  const permissions = inject(PermissionService);
  const router = inject(Router);
  return auth.ensureTenantIdentity().then(async identity => {
    if (!identity) return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
    const status = await setup.load();
    if (status?.configured || state.url.includes('/company-setup') || !permissions.isCurrentOwner()) return true;
    return router.createUrlTree(['/dashboard/company-setup']);
  });
};
