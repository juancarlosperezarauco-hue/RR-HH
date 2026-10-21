import { inject, Injectable, signal } from '@angular/core';
import { AuthStorageService } from '../../../shared/lib/storage/auth-storage.service';
import { AppearanceSettings } from '../api/appearance-settings.service';

@Injectable({ providedIn: 'root' })
export class TenantAppearanceStateService {
  private readonly authStorage = inject(AuthStorageService);
  private activeIdentity: string | null = null;
  private layoutActive = false;
  private layoutGeneration = 0;
  private revision = 0;
  private readonly value = signal<AppearanceSettings | null>(null);
  readonly confirmed = this.value.asReadonly();

  enterLayout(): number {
    this.layoutGeneration++;
    this.layoutActive = true;
    this.activeIdentity = null;
    this.revision++;
    this.value.set(null);
    return this.layoutGeneration;
  }

  activate(identity: string | null): boolean {
    if (identity === this.activeIdentity) return false;
    this.activeIdentity = identity;
    this.revision = 0;
    this.value.set(null);
    return true;
  }

  getRevision(): number {
    return this.revision;
  }

  currentSessionIdentity(): string | null {
    const session = this.authStorage.getSession();
    return session?.tenantSlug ? `${session.loginId}:${session.tenantSlug}` : null;
  }

  confirmedForCurrentSession(): AppearanceSettings | null {
    const appearance = this.value();
    return this.layoutActive && this.activeIdentity !== null
      && this.activeIdentity === this.currentSessionIdentity() ? appearance : null;
  }

  confirmFetched(identity: string, revision: number, appearance: AppearanceSettings): void {
    if (this.activeIdentity === identity && this.currentSessionIdentity() === identity && this.revision === revision) {
      this.value.set({ ...appearance });
    }
  }

  confirmSaved(identity: string | null, appearance: AppearanceSettings): boolean {
    if (!identity || !this.layoutActive || this.currentSessionIdentity() !== identity
      || (this.activeIdentity !== null && this.activeIdentity !== identity)) return false;
    this.activeIdentity = identity;
    this.revision++;
    this.value.set({ ...appearance });
    return true;
  }

  deactivate(layoutGeneration: number): void {
    if (this.layoutGeneration !== layoutGeneration) return;
    this.layoutActive = false;
    this.activeIdentity = null;
    this.revision++;
    this.value.set(null);
  }
}
