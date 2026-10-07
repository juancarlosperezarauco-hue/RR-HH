import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ResetPasswordUseCase } from '../../features/auth';

@Component({
  standalone: true,
  selector: 'app-reset-password-page',
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  template: `
    <div class="min-h-screen bg-white px-4 py-10">
      <div class="mx-auto flex min-h-[calc(100vh-5rem)] w-full max-w-md items-center">
        <div class="w-full rounded-[28px] border border-[#C8E6C9] bg-white p-8 shadow-[0_18px_50px_rgba(27,94,32,0.08)]">
          <div class="mb-8 text-center">
            <div class="mx-auto mb-4 inline-flex h-12 w-12 items-center justify-center rounded-2xl bg-[#2E7D32] text-white">
              *
            </div>
            <h1 class="text-2xl font-black tracking-tight text-[#1B5E20]">Crear nueva contraseña</h1>
            <p class="mt-2 text-sm text-[#5F6F5F]">Confirma tu nueva contraseña para el tenant <span class="font-semibold text-[#1B5E20]">{{ tenantSlug || '...' }}</span>.</p>
          </div>

          <div *ngIf="errorText" class="mb-6 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
            {{ errorText }}
          </div>

          <div *ngIf="useCase.status() === 'error' && useCase.error()" class="mb-6 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
            {{ useCase.error() }}
          </div>

          <div *ngIf="useCase.status() === 'success' && useCase.message()" class="mb-6 rounded-2xl border border-[#C8E6C9] bg-[#F1F8E9] p-4 text-sm text-[#1B5E20]">
            {{ useCase.message() }}
          </div>

          <form [formGroup]="form" (ngSubmit)="submit()" class="space-y-4">
            <div>
              <label class="mb-1 block text-sm font-semibold text-[#567157]" for="newPassword">Nueva contraseña</label>
              <input
                id="newPassword"
                type="password"
                formControlName="newPassword"
                class="w-full rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] px-4 py-3 text-sm text-[#1B5E20] outline-none transition-colors focus:border-[#2E7D32] focus:bg-white"
                placeholder="••••••••">
              <span *ngIf="isInvalid('newPassword')" class="mt-1 text-xs text-red-600">La contraseña debe tener al menos 8 caracteres.</span>
            </div>

            <div>
              <label class="mb-1 block text-sm font-semibold text-[#567157]" for="confirmPassword">Confirmar contraseña</label>
              <input
                id="confirmPassword"
                type="password"
                formControlName="confirmPassword"
                class="w-full rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] px-4 py-3 text-sm text-[#1B5E20] outline-none transition-colors focus:border-[#2E7D32] focus:bg-white"
                placeholder="••••••••">
              <span *ngIf="confirmPasswordMismatch()" class="mt-1 text-xs text-red-600">Las contraseñas no coinciden.</span>
            </div>

            <button
              type="submit"
              [disabled]="useCase.status() === 'loading'"
              class="flex w-full items-center justify-center rounded-full bg-[#2E7D32] px-4 py-3 font-semibold text-white transition-colors hover:bg-[#256428] disabled:cursor-not-allowed disabled:opacity-50">
              {{ useCase.status() === 'loading' ? 'Actualizando...' : 'Restablecer contraseña' }}
            </button>
          </form>

          <div class="mt-6 flex items-center justify-between gap-3 text-sm">
            <a routerLink="/login" class="font-semibold text-[#2E7D32] hover:text-[#256428] hover:underline">Volver al login</a>
            <a routerLink="/forgot-password" class="font-semibold text-[#2E7D32] hover:text-[#256428] hover:underline">Solicitar otro enlace</a>
          </div>
        </div>
      </div>
    </div>
  `
})
export class ResetPasswordPageComponent implements OnInit {
  readonly useCase = inject(ResetPasswordUseCase);
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  tenantSlug = '';
  token = '';
  errorText: string | null = null;

  form = this.fb.group({
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required]]
  });

  ngOnInit(): void {
    this.useCase.resetState();
    this.tenantSlug = (this.route.snapshot.queryParamMap.get('tenant') || '').trim();
    this.token = (this.route.snapshot.queryParamMap.get('token') || '').trim();

    if (!this.tenantSlug || !this.token) {
      this.errorText = 'El enlace de recuperación es inválido o incompleto.';
    }
  }

  async submit(): Promise<void> {
    if (this.errorText || this.form.invalid || this.useCase.status() === 'loading') {
      this.form.markAllAsTouched();
      return;
    }

    if (this.confirmPasswordMismatch()) {
      this.form.get('confirmPassword')?.markAsTouched();
      return;
    }

    const newPassword = String(this.form.value.newPassword ?? '');
    await this.useCase.reset({
      tenantSlug: this.tenantSlug,
      token: this.token,
      newPassword
    });

    if (this.useCase.status() === 'success') {
      await this.router.navigate(['/login'], { queryParams: { passwordReset: 'success' } });
    }
  }

  isInvalid(fieldName: string): boolean {
    const field = this.form.get(fieldName);
    return !!field && field.invalid && (field.dirty || field.touched);
  }

  confirmPasswordMismatch(): boolean {
    const newPassword = String(this.form.value.newPassword ?? '');
    const confirmPassword = String(this.form.value.confirmPassword ?? '');
    return !!newPassword && !!confirmPassword && newPassword !== confirmPassword;
  }
}
