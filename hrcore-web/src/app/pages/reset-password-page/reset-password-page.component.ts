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
    <div class="hrcore-brand-theme relative min-h-screen overflow-hidden bg-background px-4 py-8 sm:px-6 lg:px-8">
      <div class="pointer-events-none absolute -right-24 -top-24 h-72 w-72 rounded-full bg-primary/10 blur-3xl"></div>
      <div class="pointer-events-none absolute -bottom-32 -left-20 h-96 w-96 rounded-full bg-secondary/10 blur-3xl"></div>

      <div class="relative mx-auto flex min-h-[calc(100vh-4rem)] w-full max-w-5xl items-center justify-center">
        <div class="grid w-full overflow-hidden rounded-[32px] border border-border bg-card shadow-lg lg:grid-cols-[0.85fr_1.15fr]">
          <aside class="relative hidden overflow-hidden bg-sidebar p-10 text-sidebar-foreground lg:flex lg:flex-col lg:justify-between">
            <div class="absolute -right-20 -top-20 h-64 w-64 rounded-full border-[32px] border-secondary/20"></div>
            <div class="absolute -bottom-28 -left-20 h-72 w-72 rounded-full border-[40px] border-primary/30"></div>
            <div class="relative">
              <div class="mb-10 flex items-center gap-3">
                <div class="flex h-12 w-12 items-center justify-center rounded-2xl bg-card p-2 shadow-lg">
                  <img src="/logo.png" alt="HR Core" class="h-full w-full object-contain">
                </div>
                <div>
                  <p class="text-lg font-black tracking-tight">HR Core</p>
                  <p class="text-xs font-medium text-sidebar-foreground/70">Recursos Humanos</p>
                </div>
              </div>
              <span class="inline-flex items-center gap-2 rounded-full border border-sidebar-foreground/20 bg-sidebar-foreground/10 px-3 py-1.5 text-xs font-bold uppercase tracking-[0.14em] text-sidebar-foreground/80">
                <span class="h-1.5 w-1.5 rounded-full bg-secondary"></span>
                Seguridad de acceso
              </span>
              <h2 class="mt-6 max-w-sm text-3xl font-black leading-tight tracking-tight">Protege tu espacio de trabajo.</h2>
              <p class="mt-4 max-w-sm text-sm leading-6 text-sidebar-foreground/75">Actualiza tu contraseña y mantén segura la información de tu organización.</p>
            </div>
            <div class="relative mt-12 space-y-4 text-sm text-sidebar-foreground/80">
              <div class="flex items-center gap-3"><span class="flex h-8 w-8 items-center justify-center rounded-xl bg-sidebar-foreground/10 text-secondary">✓</span><span>Contraseña segura</span></div>
              <div class="flex items-center gap-3"><span class="flex h-8 w-8 items-center justify-center rounded-xl bg-sidebar-foreground/10 text-secondary">✓</span><span>Acceso protegido por tenant</span></div>
            </div>
          </aside>

          <main class="p-6 sm:p-10 lg:p-12">
            <div class="mb-8 flex items-center gap-3 lg:hidden">
              <img src="/logo.png" alt="HR Core" class="h-11 w-11 rounded-xl object-contain">
              <div><p class="font-black tracking-tight text-foreground">HR Core</p><p class="text-xs text-muted-foreground">Recursos Humanos</p></div>
            </div>

            <div class="mb-8">
              <div class="mb-5 flex h-14 w-14 items-center justify-center rounded-2xl bg-primary/10 text-2xl font-black text-primary">*</div>
              <p class="mb-2 text-xs font-bold uppercase tracking-[0.16em] text-primary">HR Core · Seguridad</p>
              <h1 class="text-3xl font-black tracking-tight text-foreground sm:text-4xl">Crea una nueva contraseña</h1>
              <p class="mt-3 max-w-lg text-sm leading-6 text-muted-foreground">Confirma una nueva clave para recuperar tu acceso al tenant <span class="font-bold text-foreground">{{ tenantSlug || '...' }}</span>.</p>
            </div>

            <div *ngIf="errorText" class="mb-6 rounded-2xl border border-destructive/20 bg-destructive/5 p-4 text-sm text-destructive">
              <p class="font-bold">Enlace no válido</p>
              <p class="mt-1 leading-5">{{ errorText }}</p>
            </div>

            <div *ngIf="useCase.status() === 'error' && useCase.error()" class="mb-6 rounded-2xl border border-destructive/20 bg-destructive/5 p-4 text-sm text-destructive">
              <p class="font-bold">No pudimos actualizar la contraseña</p>
              <p class="mt-1 leading-5">{{ useCase.error() }}</p>
            </div>

            <div *ngIf="useCase.status() === 'success' && useCase.message()" class="mb-6 rounded-2xl border border-secondary/30 bg-secondary/10 p-4 text-sm text-foreground">
              <p class="font-bold text-accent-foreground">Contraseña actualizada</p>
              <p class="mt-1 leading-5 text-muted-foreground">{{ useCase.message() }}</p>
            </div>

            <form [formGroup]="form" (ngSubmit)="submit()" class="space-y-5">
              <div>
                <label class="mb-2 block text-sm font-bold text-foreground" for="newPassword">Nueva contraseña</label>
                <input id="newPassword" type="password" formControlName="newPassword" class="w-full rounded-2xl border border-input bg-muted/30 px-4 py-3.5 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-primary focus:bg-card focus:ring-2 focus:ring-primary/15" placeholder="••••••••">
                <span *ngIf="isInvalid('newPassword')" class="mt-1.5 block text-xs text-destructive">La contraseña debe tener al menos 8 caracteres.</span>
              </div>

              <div>
                <label class="mb-2 block text-sm font-bold text-foreground" for="confirmPassword">Confirmar contraseña</label>
                <input id="confirmPassword" type="password" formControlName="confirmPassword" class="w-full rounded-2xl border border-input bg-muted/30 px-4 py-3.5 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-primary focus:bg-card focus:ring-2 focus:ring-primary/15" placeholder="••••••••">
                <span *ngIf="confirmPasswordMismatch()" class="mt-1.5 block text-xs text-destructive">Las contraseñas no coinciden.</span>
              </div>

              <button type="submit" [disabled]="useCase.status() === 'loading'" class="flex w-full items-center justify-center rounded-2xl bg-primary px-4 py-3.5 font-bold text-primary-foreground shadow-lg shadow-primary/20 transition-all hover:-translate-y-0.5 hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50">
                {{ useCase.status() === 'loading' ? 'Actualizando...' : 'Restablecer contraseña' }}
              </button>
            </form>

            <div class="mt-8 flex flex-wrap items-center justify-between gap-3 border-t border-border pt-5 text-sm">
              <a routerLink="/login" class="font-bold text-primary transition-colors hover:text-primary/80 hover:underline">Volver al login</a>
              <a routerLink="/forgot-password" class="font-semibold text-muted-foreground transition-colors hover:text-primary hover:underline">Solicitar otro enlace</a>
            </div>
          </main>
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
