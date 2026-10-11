import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { LucideAngularModule } from 'lucide-angular';
import { PlatformService } from '../../entities/platform/api/platform.service';
import { PlatformSuperadminMeUseCase } from '../../features/platform/application/platform-superadmin-me.usecase';

@Component({
  selector: 'app-platform-security-page',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule, LucideAngularModule],
  template: `
    <div class="space-y-6 p-4 sm:p-6 lg:p-8">
      <section class="rounded-[28px] border border-border bg-gradient-to-br from-card via-background to-muted p-6 shadow-lg">
        <div class="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
          <div class="space-y-3">
            <div class="inline-flex items-center gap-2 rounded-full border border-border bg-card/80 px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-primary">
              <span class="h-2 w-2 rounded-full bg-secondary"></span>
              Plataforma superadmin
            </div>
            <div>
              <h1 class="text-3xl font-black tracking-tight text-accent-foreground sm:text-4xl">
                Seguridad
              </h1>
              <p class="mt-2 max-w-3xl text-sm leading-6 text-muted-foreground">
                Cambia tu contraseña de acceso y mantiene protegida tu cuenta de plataforma.
              </p>
            </div>
          </div>

          <div class="flex flex-wrap gap-3">
            <a
              routerLink="/platform/profile"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-border bg-card px-4 py-2 text-sm font-semibold text-primary transition-colors hover:bg-accent">
              <lucide-icon name="user-circle-2" class="h-4 w-4"></lucide-icon>
              Mi perfil
            </a>
          </div>
        </div>
      </section>

      @if (status() === 'loading' && !superadmin()) {
        <section class="rounded-[24px] border border-border bg-card p-8 text-sm text-muted-foreground shadow-sm">
          Cargando información de seguridad...
        </section>
      }

      @if (error()) {
        <section class="rounded-[24px] border border-border bg-muted p-6 shadow-sm">
          <p class="text-sm font-semibold text-foreground">{{ error() }}</p>
        </section>
      }

      @if (superadmin(); as user) {
        <section class="grid gap-6 xl:grid-cols-[0.8fr_1.2fr]">
          <div class="rounded-[24px] border border-border bg-card p-4 sm:p-6 shadow-sm">
            <div class="flex items-center gap-3">
              <div class="rounded-2xl bg-accent p-3 text-primary">
                <lucide-icon name="shield" class="h-5 w-5"></lucide-icon>
              </div>
              <div>
                <h2 class="text-lg font-bold text-accent-foreground">Cuenta activa</h2>
                <p class="text-sm text-muted-foreground">Perfil autenticado en la plataforma.</p>
              </div>
            </div>

            <div class="mt-6 space-y-4">
              <div class="rounded-2xl border border-border bg-muted p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Nombre</p>
                <p class="mt-1 break-words text-sm font-semibold text-accent-foreground">{{ user.firstName }} {{ user.lastName }}</p>
              </div>
              <div class="rounded-2xl border border-border bg-muted p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Correo</p>
                <p class="mt-1 break-words text-sm font-semibold text-accent-foreground">{{ user.email }}</p>
              </div>
              <div class="rounded-2xl border border-border bg-muted p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Estado</p>
                <p class="mt-1 break-words text-sm font-semibold text-accent-foreground">{{ user.active ? 'Activo' : 'Inactivo' }}</p>
              </div>
            </div>
          </div>

          <div class="rounded-[24px] border border-border bg-card p-4 sm:p-6 shadow-sm">
            <div class="flex items-center gap-3">
              <div class="rounded-2xl bg-accent p-3 text-primary">
                <lucide-icon name="lock" class="h-5 w-5"></lucide-icon>
              </div>
              <div>
                <h2 class="text-lg font-bold text-accent-foreground">Cambiar contraseña</h2>
                <p class="text-sm text-muted-foreground">Usa una clave nueva segura para tu sesión de plataforma.</p>
              </div>
            </div>

            <form class="mt-6 grid gap-4 sm:grid-cols-2" [formGroup]="form" (ngSubmit)="submit()">
              <label class="sm:col-span-2">
                <span class="mb-1 block text-sm font-semibold text-primary">Contraseña actual</span>
                <div class="relative">
                  <input
                    [type]="showCurrentPassword ? 'text' : 'password'"
                    formControlName="currentPassword"
                    class="w-full rounded-2xl border border-border bg-card px-4 py-3 pr-10 text-sm text-accent-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-primary">
                  <button
                    type="button"
                    (click)="togglePassword('current')"
                    class="absolute inset-y-0 right-0 flex items-center pr-3 text-muted-foreground transition-colors hover:text-primary"
                    tabindex="-1">
                    <lucide-icon [name]="showCurrentPassword ? 'eye-off' : 'eye'" class="h-4 w-4"></lucide-icon>
                  </button>
                </div>
              </label>

              <label>
                <span class="mb-1 block text-sm font-semibold text-primary">Nueva contraseña</span>
                <div class="relative">
                  <input
                    [type]="showNewPassword ? 'text' : 'password'"
                    formControlName="newPassword"
                    class="w-full rounded-2xl border border-border bg-card px-4 py-3 pr-10 text-sm text-accent-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-primary">
                  <button
                    type="button"
                    (click)="togglePassword('new')"
                    class="absolute inset-y-0 right-0 flex items-center pr-3 text-muted-foreground transition-colors hover:text-primary"
                    tabindex="-1">
                    <lucide-icon [name]="showNewPassword ? 'eye-off' : 'eye'" class="h-4 w-4"></lucide-icon>
                  </button>
                </div>
              </label>

              <label>
                <span class="mb-1 block text-sm font-semibold text-primary">Confirmar contraseña</span>
                <div class="relative">
                  <input
                    [type]="showConfirmPassword ? 'text' : 'password'"
                    formControlName="confirmPassword"
                    class="w-full rounded-2xl border border-border bg-card px-4 py-3 pr-10 text-sm text-accent-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-primary">
                  <button
                    type="button"
                    (click)="togglePassword('confirm')"
                    class="absolute inset-y-0 right-0 flex items-center pr-3 text-muted-foreground transition-colors hover:text-primary"
                    tabindex="-1">
                    <lucide-icon [name]="showConfirmPassword ? 'eye-off' : 'eye'" class="h-4 w-4"></lucide-icon>
                  </button>
                </div>
              </label>

              <p class="sm:col-span-2 text-xs text-muted-foreground">
                La nueva contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula y un número.
              </p>

              <div class="sm:col-span-2 flex flex-wrap gap-3">
                <button
                  type="submit"
                  [disabled]="saving"
                  class="inline-flex cursor-pointer items-center gap-2 rounded-full bg-accent-foreground px-4 py-2 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary disabled:cursor-not-allowed disabled:opacity-60">
                  <lucide-icon name="save" class="h-4 w-4"></lucide-icon>
                  {{ saving ? 'Guardando...' : 'Actualizar contraseña' }}
                </button>
                <button
                  type="button"
                  (click)="reset()"
                  class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-border bg-card px-4 py-2 text-sm font-semibold text-primary transition-colors hover:bg-accent">
                  Restablecer
                </button>
              </div>

              @if (successMessage) {
                <div class="sm:col-span-2 rounded-2xl border border-border bg-accent px-4 py-3 text-sm font-medium text-accent-foreground">
                  {{ successMessage }}
                </div>
              }

              @if (formError) {
                <div class="sm:col-span-2 rounded-2xl border border-border bg-muted px-4 py-3 text-sm font-medium text-foreground">
                  {{ formError }}
                </div>
              }
            </form>
          </div>
        </section>
      }
    </div>
  `
})
export class PlatformSecurityPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly platformService = inject(PlatformService);
  private readonly superadminMeUseCase = inject(PlatformSuperadminMeUseCase);

  readonly status = this.superadminMeUseCase.status;
  readonly superadmin = this.superadminMeUseCase.superadmin;
  readonly error = this.superadminMeUseCase.error;

  saving = false;
  successMessage = '';
  formError = '';
  showCurrentPassword = false;
  showNewPassword = false;
  showConfirmPassword = false;

  readonly form = this.fb.nonNullable.group({
    currentPassword: ['', [Validators.required, Validators.minLength(8)]],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required, Validators.minLength(8)]]
  });

  ngOnInit(): void {
    void this.superadminMeUseCase.loadCurrentSuperadmin();
  }

  reset(): void {
    this.form.reset();
    this.successMessage = '';
    this.formError = '';
    this.showCurrentPassword = false;
    this.showNewPassword = false;
    this.showConfirmPassword = false;
  }

  async submit(): Promise<void> {
    this.successMessage = '';
    this.formError = '';

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.formError = 'Revisa los campos obligatorios antes de continuar.';
      return;
    }

    const { currentPassword, newPassword, confirmPassword } = this.form.getRawValue();
    if (newPassword !== confirmPassword) {
      this.formError = 'La nueva contraseña y la confirmación no coinciden.';
      return;
    }

    this.saving = true;
    try {
      const response = await firstValueFrom(this.platformService.changePassword({ currentPassword, newPassword }));
      if (response.success) {
        this.successMessage = response.message || 'Contraseña actualizada correctamente.';
        this.form.reset();
      } else {
        this.formError = response.message || 'No se pudo actualizar la contraseña.';
      }
    } catch (err: any) {
      this.formError = err.error?.message || err.message || 'Error al actualizar la contraseña.';
    } finally {
      this.saving = false;
    }
  }

  togglePassword(field: 'current' | 'new' | 'confirm'): void {
    if (field === 'current') {
      this.showCurrentPassword = !this.showCurrentPassword;
      return;
    }

    if (field === 'new') {
      this.showNewPassword = !this.showNewPassword;
      return;
    }

    this.showConfirmPassword = !this.showConfirmPassword;
  }
}
