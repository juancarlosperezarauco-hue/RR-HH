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
      <section class="rounded-[28px] border border-[#C8E6C9] bg-gradient-to-br from-white via-[#F7FBF3] to-[#EAF6EB] p-6 shadow-[0_18px_50px_rgba(27,94,32,0.08)]">
        <div class="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
          <div class="space-y-3">
            <div class="inline-flex items-center gap-2 rounded-full border border-[#C8E6C9] bg-white/80 px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-[#2E7D32]">
              <span class="h-2 w-2 rounded-full bg-[#4CAF50]"></span>
              Plataforma superadmin
            </div>
            <div>
              <h1 class="text-3xl font-black tracking-tight text-[#1B5E20] sm:text-4xl">
                Seguridad
              </h1>
              <p class="mt-2 max-w-3xl text-sm leading-6 text-[#5F6F5F]">
                Cambia tu contraseña de acceso y mantiene protegida tu cuenta de plataforma.
              </p>
            </div>
          </div>

          <div class="flex flex-wrap gap-3">
            <a
              routerLink="/platform/profile"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-[#C8E6C9] bg-white px-4 py-2 text-sm font-semibold text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
              <lucide-icon name="user-circle-2" class="h-4 w-4"></lucide-icon>
              Mi perfil
            </a>
          </div>
        </div>
      </section>

      @if (status() === 'loading' && !superadmin()) {
        <section class="rounded-[24px] border border-[#C8E6C9] bg-white p-8 text-sm text-[#6B7D6C] shadow-sm">
          Cargando información de seguridad...
        </section>
      }

      @if (error()) {
        <section class="rounded-[24px] border border-[#F3C6C6] bg-[#FFF7F7] p-6 shadow-sm">
          <p class="text-sm font-semibold text-[#B42318]">{{ error() }}</p>
        </section>
      }

      @if (superadmin(); as user) {
        <section class="grid gap-6 xl:grid-cols-[0.8fr_1.2fr]">
          <div class="rounded-[24px] border border-[#C8E6C9] bg-white p-4 sm:p-6 shadow-sm">
            <div class="flex items-center gap-3">
              <div class="rounded-2xl bg-[#F1F8E9] p-3 text-[#2E7D32]">
                <lucide-icon name="shield" class="h-5 w-5"></lucide-icon>
              </div>
              <div>
                <h2 class="text-lg font-bold text-[#1B5E20]">Cuenta activa</h2>
                <p class="text-sm text-[#6B7D6C]">Perfil autenticado en la plataforma.</p>
              </div>
            </div>

            <div class="mt-6 space-y-4">
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Nombre</p>
                <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ user.firstName }} {{ user.lastName }}</p>
              </div>
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Correo</p>
                <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ user.email }}</p>
              </div>
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Estado</p>
                <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ user.active ? 'Activo' : 'Inactivo' }}</p>
              </div>
            </div>
          </div>

          <div class="rounded-[24px] border border-[#C8E6C9] bg-white p-4 sm:p-6 shadow-sm">
            <div class="flex items-center gap-3">
              <div class="rounded-2xl bg-[#F1F8E9] p-3 text-[#2E7D32]">
                <lucide-icon name="lock" class="h-5 w-5"></lucide-icon>
              </div>
              <div>
                <h2 class="text-lg font-bold text-[#1B5E20]">Cambiar contraseña</h2>
                <p class="text-sm text-[#6B7D6C]">Usa una clave nueva segura para tu sesión de plataforma.</p>
              </div>
            </div>

            <form class="mt-6 grid gap-4 sm:grid-cols-2" [formGroup]="form" (ngSubmit)="submit()">
              <label class="sm:col-span-2">
                <span class="mb-1 block text-sm font-semibold text-[#2E7D32]">Contraseña actual</span>
                <div class="relative">
                  <input
                    [type]="showCurrentPassword ? 'text' : 'password'"
                    formControlName="currentPassword"
                    class="w-full rounded-2xl border border-[#C8E6C9] bg-white px-4 py-3 pr-10 text-sm text-[#1B5E20] outline-none transition-colors placeholder:text-[#9AA99A] focus:border-[#2E7D32]">
                  <button
                    type="button"
                    (click)="togglePassword('current')"
                    class="absolute inset-y-0 right-0 flex items-center pr-3 text-[#6B7D6C] transition-colors hover:text-[#2E7D32]"
                    tabindex="-1">
                    <lucide-icon [name]="showCurrentPassword ? 'eye-off' : 'eye'" class="h-4 w-4"></lucide-icon>
                  </button>
                </div>
              </label>

              <label>
                <span class="mb-1 block text-sm font-semibold text-[#2E7D32]">Nueva contraseña</span>
                <div class="relative">
                  <input
                    [type]="showNewPassword ? 'text' : 'password'"
                    formControlName="newPassword"
                    class="w-full rounded-2xl border border-[#C8E6C9] bg-white px-4 py-3 pr-10 text-sm text-[#1B5E20] outline-none transition-colors placeholder:text-[#9AA99A] focus:border-[#2E7D32]">
                  <button
                    type="button"
                    (click)="togglePassword('new')"
                    class="absolute inset-y-0 right-0 flex items-center pr-3 text-[#6B7D6C] transition-colors hover:text-[#2E7D32]"
                    tabindex="-1">
                    <lucide-icon [name]="showNewPassword ? 'eye-off' : 'eye'" class="h-4 w-4"></lucide-icon>
                  </button>
                </div>
              </label>

              <label>
                <span class="mb-1 block text-sm font-semibold text-[#2E7D32]">Confirmar contraseña</span>
                <div class="relative">
                  <input
                    [type]="showConfirmPassword ? 'text' : 'password'"
                    formControlName="confirmPassword"
                    class="w-full rounded-2xl border border-[#C8E6C9] bg-white px-4 py-3 pr-10 text-sm text-[#1B5E20] outline-none transition-colors placeholder:text-[#9AA99A] focus:border-[#2E7D32]">
                  <button
                    type="button"
                    (click)="togglePassword('confirm')"
                    class="absolute inset-y-0 right-0 flex items-center pr-3 text-[#6B7D6C] transition-colors hover:text-[#2E7D32]"
                    tabindex="-1">
                    <lucide-icon [name]="showConfirmPassword ? 'eye-off' : 'eye'" class="h-4 w-4"></lucide-icon>
                  </button>
                </div>
              </label>

              <p class="sm:col-span-2 text-xs text-[#6B7D6C]">
                La nueva contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula y un número.
              </p>

              <div class="sm:col-span-2 flex flex-wrap gap-3">
                <button
                  type="submit"
                  [disabled]="saving"
                  class="inline-flex cursor-pointer items-center gap-2 rounded-full bg-[#1B5E20] px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-[#2E7D32] disabled:cursor-not-allowed disabled:opacity-60">
                  <lucide-icon name="save" class="h-4 w-4"></lucide-icon>
                  {{ saving ? 'Guardando...' : 'Actualizar contraseña' }}
                </button>
                <button
                  type="button"
                  (click)="reset()"
                  class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-[#C8E6C9] bg-white px-4 py-2 text-sm font-semibold text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
                  Restablecer
                </button>
              </div>

              @if (successMessage) {
                <div class="sm:col-span-2 rounded-2xl border border-[#C8E6C9] bg-[#F1F8E9] px-4 py-3 text-sm font-medium text-[#1B5E20]">
                  {{ successMessage }}
                </div>
              }

              @if (formError) {
                <div class="sm:col-span-2 rounded-2xl border border-[#F3C6C6] bg-[#FFF7F7] px-4 py-3 text-sm font-medium text-[#B42318]">
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
