import { Injectable } from '@angular/core';
import { TenantUpgradeCheckoutPending } from '../model/tenant-billing.model';

@Injectable({
  providedIn: 'root'
})
export class TenantUpgradeCheckoutStorageService {
  private readonly KEY = 'hrcore.tenant.upgrade.checkout.pending';

  save(data: TenantUpgradeCheckoutPending): void {
    localStorage.setItem(this.KEY, JSON.stringify(data));
  }

  get(): TenantUpgradeCheckoutPending | null {
    const raw = localStorage.getItem(this.KEY);

    if (!raw) {
      return null;
    }

    try {
      return JSON.parse(raw) as TenantUpgradeCheckoutPending;
    } catch {
      this.clear();
      return null;
    }
  }

  clear(): void {
    localStorage.removeItem(this.KEY);
  }
}
