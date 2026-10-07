import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ActivateAccountUseCase } from '../../features/auth';

@Component({
  standalone: true,
  selector: 'app-activate-account-page',
  imports: [CommonModule, RouterModule],
  template: `
    <div class="min-h-screen bg-white px-4 py-10">
      <div class="mx-auto flex min-h-[calc(100vh-5rem)] w-full max-w-md items-center">
        <div class="w-full rounded-[28px] border border-[#C8E6C9] bg-white p-8 shadow-[0_18px_50px_rgba(27,94,32,0.08)]">
          <div class="mb-8 text-center">
            <div class="mx-auto mb-4 inline-flex h-12 w-12 items-center justify-center rounded-2xl bg-[#2E7D32] text-white">
              ✓
            </div>
            <h1 class="text-2xl font-black tracking-tight text-[#1B5E20]">Activación de cuenta</h1>
            <p class="mt-2 text-sm text-[#5F6F5F]">Tenant <span class="font-semibold text-[#1B5E20]">{{ tenantSlug || '...' }}</span></p>
          </div>

          <div *ngIf="useCase.status() === 'loading'" class="mb-6 rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4 text-center text-sm text-[#567157]">
            Activando tu cuenta...
          </div>

          <div *ngIf="errorText || (useCase.status() === 'error' && useCase.error())" class="mb-6 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
            {{ errorText || useCase.error() }}
          </div>

          <div *ngIf="useCase.status() === 'success' && useCase.message()" class="mb-6 rounded-2xl border border-[#C8E6C9] bg-[#F1F8E9] p-4 text-sm text-[#1B5E20]">
            {{ useCase.message() }}
          </div>

          <div class="space-y-3">
            <a
              *ngIf="useCase.status() === 'success'"
              routerLink="/login"
              [queryParams]="{ accountActivated: 'success' }"
              class="flex w-full items-center justify-center rounded-full bg-[#2E7D32] px-4 py-3 font-semibold text-white transition-colors hover:bg-[#256428]">
              Ir al login
            </a>

            <button
              *ngIf="useCase.status() === 'error' && !errorText"
              type="button"
              [disabled]="useCase.status() === 'loading'"
              (click)="retry()"
              class="flex w-full items-center justify-center rounded-full border border-[#2E7D32] px-4 py-3 font-semibold text-[#2E7D32] transition-colors hover:bg-[#F1F8E9] disabled:cursor-not-allowed disabled:opacity-50">
              Reintentar
            </button>
          </div>

          <div class="mt-6 flex items-center justify-between gap-3 text-sm">
            <a routerLink="/login" class="font-semibold text-[#2E7D32] hover:text-[#256428] hover:underline">Volver al login</a>
            <a routerLink="/forgot-password" class="font-semibold text-[#2E7D32] hover:text-[#256428] hover:underline">¿Olvidaste tu contraseña?</a>
          </div>
        </div>
      </div>
    </div>
  `
})
export class ActivateAccountPageComponent implements OnInit {
  readonly useCase = inject(ActivateAccountUseCase);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  tenantSlug = '';
  token = '';
  errorText: string | null = null;

  ngOnInit(): void {
    this.useCase.resetState();
    this.tenantSlug = (this.route.snapshot.queryParamMap.get('tenant') || '').trim();
    this.token = (this.route.snapshot.queryParamMap.get('token') || '').trim();

    if (!this.tenantSlug || !this.token) {
      this.errorText = 'El enlace de activación es inválido o incompleto.';
      return;
    }

    void this.activate();
  }

  async activate(): Promise<void> {
    await this.useCase.activate({
      tenantSlug: this.tenantSlug,
      token: this.token
    });
  }

  retry(): void {
    this.errorText = null;
    void this.activate();
  }
}
