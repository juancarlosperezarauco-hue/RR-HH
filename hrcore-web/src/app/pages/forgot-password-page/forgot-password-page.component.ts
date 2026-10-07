import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ForgotPasswordUseCase } from '../../features/auth';

@Component({
  standalone: true,
  selector: 'app-forgot-password-page',
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  template: `
    <div class="min-h-screen bg-white px-4 py-10">
      <div class="mx-auto flex min-h-[calc(100vh-5rem)] w-full max-w-md items-center">
        <div class="w-full rounded-[28px] border border-[#C8E6C9] bg-white p-8 shadow-[0_18px_50px_rgba(27,94,32,0.08)]">
          <div class="mb-8 text-center">
            <div class="mx-auto mb-4 inline-flex h-12 w-12 items-center justify-center rounded-2xl bg-[#2E7D32] text-white">
              ?
            </div>
            <h1 class="text-2xl font-black tracking-tight text-[#1B5E20]">Recuperar contraseña</h1>
            <p class="mt-2 text-sm text-[#5F6F5F]">Ingresa tu tenant y tu correo. Te enviaremos un enlace de restablecimiento.</p>
          </div>

          <div *ngIf="useCase.status() === 'error' && useCase.error()" class="mb-6 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
            {{ useCase.error() }}
          </div>

          <div *ngIf="useCase.status() === 'success' && useCase.message()" class="mb-6 rounded-2xl border border-[#C8E6C9] bg-[#F1F8E9] p-4 text-sm text-[#1B5E20]">
            {{ useCase.message() }}
          </div>

          <form [formGroup]="form" (ngSubmit)="submit()" class="space-y-4">
            <div>
              <label class="mb-1 block text-sm font-semibold text-[#567157]" for="tenantSlug">Tenant</label>
              <input
                id="tenantSlug"
                type="text"
                formControlName="tenantSlug"
                autocomplete="off"
                class="w-full rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] px-4 py-3 text-sm text-[#1B5E20] outline-none transition-colors focus:border-[#2E7D32] focus:bg-white"
                placeholder="ej: sebastian">
              <span *ngIf="isInvalid('tenantSlug')" class="mt-1 text-xs text-red-600">El tenant es requerido.</span>
            </div>

            <div>
              <label class="mb-1 block text-sm font-semibold text-[#567157]" for="email">Correo electrónico</label>
              <input
                id="email"
                type="email"
                formControlName="email"
                class="w-full rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] px-4 py-3 text-sm text-[#1B5E20] outline-none transition-colors focus:border-[#2E7D32] focus:bg-white"
                placeholder="nombre@empresa.com">
              <span *ngIf="isInvalid('email')" class="mt-1 text-xs text-red-600">Ingresa un correo válido.</span>
            </div>

            <button
              type="submit"
              [disabled]="useCase.status() === 'loading'"
              class="flex w-full items-center justify-center rounded-full bg-[#2E7D32] px-4 py-3 font-semibold text-white transition-colors hover:bg-[#256428] disabled:cursor-not-allowed disabled:opacity-50">
              {{ useCase.status() === 'loading' ? 'Enviando...' : 'Enviar enlace' }}
            </button>
          </form>

          <div class="mt-6 flex items-center justify-between gap-3 text-sm">
            <a routerLink="/login" class="font-semibold text-[#2E7D32] hover:text-[#256428] hover:underline">Volver al login</a>
            <a routerLink="/onboarding" class="font-semibold text-[#2E7D32] hover:text-[#256428] hover:underline">Crear tenant</a>
          </div>
        </div>
      </div>
    </div>
  `
})
export class ForgotPasswordPageComponent implements OnInit {
  readonly useCase = inject(ForgotPasswordUseCase);
  private readonly fb = inject(FormBuilder);

  form = this.fb.group({
    tenantSlug: ['', [Validators.required, Validators.pattern(/^[a-z0-9-]+$/)]],
    email: ['', [Validators.required, Validators.email]]
  });

  ngOnInit(): void {
    this.useCase.resetState();
  }

  async submit(): Promise<void> {
    if (this.form.invalid || this.useCase.status() === 'loading') {
      this.form.markAllAsTouched();
      return;
    }

    const tenantSlug = String(this.form.value.tenantSlug ?? '').trim();
    const email = String(this.form.value.email ?? '').trim();
    await this.useCase.send({ tenantSlug, email });
  }

  isInvalid(fieldName: string): boolean {
    const field = this.form.get(fieldName);
    return !!field && field.invalid && (field.dirty || field.touched);
  }
}
