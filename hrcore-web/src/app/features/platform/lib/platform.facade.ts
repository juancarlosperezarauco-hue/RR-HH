// features/platform/lib/platform.facade.ts
import { Injectable, inject, computed } from '@angular/core';
import { PlatformSuperadminMeUseCase } from '../application/platform-superadmin-me.usecase';
import { PlatformStorageService } from './platform-storage.service';
import { AuthenticatedPlatformSuperadminResponse } from '../../../entities/platform/api/platform.service';

@Injectable({ providedIn: 'root' })
export class PlatformFacade {
  private superadminMeUseCase = inject(PlatformSuperadminMeUseCase);
  private platformStorage = inject(PlatformStorageService);
  private loadPromise: Promise<AuthenticatedPlatformSuperadminResponse | null> | null = null;

  readonly currentSuperadmin = computed(() => this.superadminMeUseCase.superadmin());
  readonly isLoading = computed(() => this.superadminMeUseCase.status() === 'loading');

  loadCurrentSuperadmin(): void {
    void this.superadminMeUseCase.loadCurrentSuperadmin();
  }

  async ensurePlatformIdentity(): Promise<AuthenticatedPlatformSuperadminResponse | null> {
    if (!this.platformStorage.getSession()) return null;
    if (this.currentSuperadmin()) return this.currentSuperadmin();
    if (!this.loadPromise) this.loadPromise = this.superadminMeUseCase.loadCurrentSuperadmin().then(() => this.currentSuperadmin());
    try { return await this.loadPromise; } finally { this.loadPromise = null; }
  }
}
