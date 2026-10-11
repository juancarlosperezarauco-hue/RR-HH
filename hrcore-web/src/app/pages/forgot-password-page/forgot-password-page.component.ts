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
    <div class="min-h-screen bg-card px-4 py-10">
      <div class="mx-auto flex min-h-[calc(100vh-5rem)] w-full max-w-md items-center">
        <div class="w-full rounded-[28px] border border-border bg-card p-8 shadow-lg">
          <div class="mb-8 text-center">
            <div class="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-2xl border border-border bg-card p-2 shadow-sm">
              <img src="/logo.png" alt="HR Core" class="h-full w-full object-contain">
            </div>
            <h1 class="text-2xl font-black tracking-tight text-accent-foreground">Recuperar contraseña</h1>
            <p class="mt-2 text-sm text-muted-foreground">Ingresa tu tenant y tu correo. Te enviaremos un enlace de restablecimiento.</p>
          </div>

          <div *ngIf="useCase.status() === 'error' && useCase.error()" class="mb-6 rounded-2xl border border-destructive/20 bg-destructive/5 p-4 text-sm text-destructive">
            {{ useCase.error() }}
          </div>

          <div *ngIf="useCase.status() === 'success' && useCase.message()" class="mb-6 rounded-2xl border border-border bg-accent p-4 text-sm text-accent-foreground">
            {{ useCase.message() }}
          </div>

          <form [formGroup]="form" (ngSubmit)="submit()" class="space-y-4">
            <div>
              <label class="mb-1 block text-sm font-semibold text-muted-foreground" for="tenantSlug">Tenant</label>
              <input
                id="tenantSlug"
                type="text"
                formControlName="tenantSlug"
                autocomplete="off"
                class="w-full rounded-2xl border border-border bg-muted px-4 py-3 text-sm text-accent-foreground outline-none transition-colors focus:border-primary focus:bg-card"
                placeholder="empresa">
              <span *ngIf="isInvalid('tenantSlug')" class="mt-1 text-xs text-destructive">El tenant es requerido.</span>
            </div>

            <div>
              <label class="mb-1 block text-sm font-semibold text-muted-foreground" for="email">Correo electrónico</label>
              <input
                id="email"
                type="email"
                formControlName="email"
                class="w-full rounded-2xl border border-border bg-muted px-4 py-3 text-sm text-accent-foreground outline-none transition-colors focus:border-primary focus:bg-card"
                placeholder="nombre@empresa.com">
              <span *ngIf="isInvalid('email')" class="mt-1 text-xs text-destructive">Ingresa un correo válido.</span>
            </div>

            <button
              type="submit"
              [disabled]="useCase.status() === 'loading'"
              class="flex w-full items-center justify-center rounded-full bg-primary px-4 py-3 font-semibold text-primary-foreground transition-colors hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50">
              {{ useCase.status() === 'loading' ? 'Enviando...' : 'Enviar enlace' }}
            </button>
          </form>

          <div class="mt-6 flex items-center justify-between gap-3 text-sm">
            <a routerLink="/login" class="font-semibold text-primary hover:text-primary/90 hover:underline">Volver al login</a>
            <a routerLink="/onboarding" class="font-semibold text-primary hover:text-primary/90 hover:underline">Crear tenant</a>
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
