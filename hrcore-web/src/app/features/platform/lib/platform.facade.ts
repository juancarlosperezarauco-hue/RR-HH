// features/platform/lib/platform.facade.ts
import { Injectable, inject, computed } from '@angular/core';
import { PlatformSuperadminMeUseCase } from '../application/platform-superadmin-me.usecase';

@Injectable({ providedIn: 'root' })
export class PlatformFacade {
  private superadminMeUseCase = inject(PlatformSuperadminMeUseCase);

  readonly currentSuperadmin = computed(() => this.superadminMeUseCase.superadmin());
  readonly isLoading = computed(() => this.superadminMeUseCase.status() === 'loading');

  loadCurrentSuperadmin(): void {
    this.superadminMeUseCase.loadCurrentSuperadmin();
  }
}