import { computed, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { TenantBillingService } from '../../../entities/billing';
import { AuthStorageService } from '../storage/auth-storage.service';

@Injectable({ providedIn: 'root' })
export class TenantPlanAccessService {
  private readonly billing = inject(TenantBillingService);
  private readonly storage = inject(AuthStorageService);
  private readonly planCode = signal<string | null>(null);
  private loadedSessionKey = '';
  private loadingSessionKey = '';
  private refreshScheduled = false;
  readonly ready = computed(() => this.planCode() !== null);

  constructor() { void this.load(); }

  async load(): Promise<void> {
    const sessionKey = this.storage.getSessionKey();
    if (!sessionKey) {
      this.loadedSessionKey = '';
      this.planCode.set(null);
      return;
    }
    if (this.loadingSessionKey === sessionKey) return;
    this.loadingSessionKey = sessionKey;
    this.planCode.set(null);
    try {
      const response = await firstValueFrom(this.billing.getCurrentSubscription());
      if (this.storage.getSessionKey() === sessionKey && response.success && response.data?.planCode) {
        this.planCode.set(response.data.planCode.toUpperCase());
        this.loadedSessionKey = sessionKey;
      }
    } catch { /* Permission-filtered users may not access billing; backend remains authoritative. */ }
    finally {
      if (this.loadingSessionKey === sessionKey) this.loadingSessionKey = '';
    }
  }

  includes(module: string | undefined): boolean {
    const currentSessionKey = this.storage.getSessionKey();
    if (currentSessionKey !== this.loadedSessionKey && this.loadingSessionKey !== currentSessionKey) {
      this.scheduleLoad();
    }
    if (!module || !this.ready()) return true;
    const included: Record<string, string[]> = {
      BASIC: ['users', 'departments', 'positions', 'employees', 'contracts', 'employee_files', 'tenantsettings', 'dashboard', 'notifications'],
      PRO: ['users', 'departments', 'positions', 'employees', 'contracts', 'employee_files', 'shifts', 'attendance', 'leaves', 'tenantsettings', 'dashboard', 'notifications'],
      PROFESSIONAL: ['users', 'departments', 'positions', 'employees', 'contracts', 'employee_files', 'shifts', 'attendance', 'leaves', 'tenantsettings', 'dashboard', 'notifications'],
      ENTERPRISE: ['users', 'departments', 'positions', 'employees', 'contracts', 'employee_files', 'shifts', 'attendance', 'leaves', 'incidents', 'hr.reports', 'backups', 'tenantsettings', 'dashboard', 'notifications'],
      DEMO: ['users', 'departments', 'positions', 'employees', 'contracts', 'employee_files', 'shifts', 'attendance', 'leaves', 'incidents', 'hr.reports', 'backups', 'tenantsettings', 'dashboard', 'notifications'],
      FREE: ['users', 'departments', 'positions', 'employees', 'contracts', 'employee_files', 'shifts', 'attendance', 'leaves', 'incidents', 'hr.reports', 'backups', 'tenantsettings', 'dashboard', 'notifications']
    };
    return (included[this.planCode() ?? ''] ?? included['BASIC']).includes(module);
  }

  private scheduleLoad(): void {
    if (this.refreshScheduled) return;
    this.refreshScheduled = true;
    queueMicrotask(() => {
      this.refreshScheduled = false;
      void this.load();
    });
  }
}
