import { inject, Injectable } from '@angular/core';
import { TenantUpgradeCheckoutPending } from '../model/tenant-billing.model';
import { AuthStorageService } from '../../../shared/lib/storage/auth-storage.service';

@Injectable({
  providedIn: 'root'
})
export class TenantUpgradeCheckoutStorageService {
  private readonly KEY = 'hrcore.tenant.upgrade.checkout.pending';
  private readonly authStorage = inject(AuthStorageService);

  save(data: TenantUpgradeCheckoutPending): void {
    localStorage.setItem(this.KEY, JSON.stringify(data));
  }

  get(): TenantUpgradeCheckoutPending | null {
    const raw = localStorage.getItem(this.KEY);

    if (!raw) {
      return null;
    }

    try {
      const pending = JSON.parse(raw) as TenantUpgradeCheckoutPending;
      if (pending.tenantSlug !== this.authStorage.getTenantSlug()) {
        this.clear();
        return null;
      }
      return pending;
    } catch {
      this.clear();
      return null;
    }
  }

  clear(): void {
    localStorage.removeItem(this.KEY);
  }
}
