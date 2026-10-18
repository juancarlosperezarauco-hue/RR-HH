import { computed, inject, Injectable, signal } from '@angular/core';
import { CompanySettingsService, CompanySetupStatus } from '../../../entities/company';

@Injectable({ providedIn: 'root' })
export class CompanySetupStateService {
  private readonly company = inject(CompanySettingsService);
  readonly status = signal<CompanySetupStatus | null>(null);
  readonly loading = signal(false);
  readonly configured = computed(() => this.status()?.configured === true);

  async load(force = false): Promise<CompanySetupStatus | null> {
    if (!force && this.status()) return this.status();
    this.loading.set(true);
    try {
      const result = await new Promise<CompanySetupStatus>((resolve, reject) => this.company.getSetupStatus().subscribe({ next: r => resolve(r.data), error: reject }));
      this.status.set(result);
      return result;
    } catch { return null; } finally { this.loading.set(false); }
  }
  setConfigured(status: CompanySetupStatus) { this.status.set(status); }
}
