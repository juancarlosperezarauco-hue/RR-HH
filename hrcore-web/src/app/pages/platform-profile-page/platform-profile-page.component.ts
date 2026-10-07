import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { PlatformSuperadminMeUseCase } from '../../features/platform/application/platform-superadmin-me.usecase';

@Component({
  selector: 'app-platform-profile-page',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule],
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
                Mi perfil
              </h1>
              <p class="mt-2 max-w-3xl text-sm leading-6 text-[#5F6F5F]">
                Consulta los datos visibles de tu cuenta de plataforma y accede rápidamente a seguridad.
              </p>
            </div>
          </div>

          <div class="flex flex-wrap gap-3">
            <button
              type="button"
              (click)="reload()"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-[#C8E6C9] bg-white px-4 py-2 text-sm font-semibold text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
              <lucide-icon name="refresh-ccw" class="h-4 w-4"></lucide-icon>
              Recargar
            </button>
            <a
              routerLink="/platform/security"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full bg-[#1B5E20] px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-[#2E7D32]">
              <lucide-icon name="lock" class="h-4 w-4"></lucide-icon>
              Seguridad
            </a>
          </div>
        </div>
      </section>

      @if (status() === 'loading' && !superadmin()) {
        <section class="rounded-[24px] border border-[#C8E6C9] bg-white p-8 text-sm text-[#6B7D6C] shadow-sm">
          Cargando información del perfil...
        </section>
      }

      @if (error()) {
        <section class="rounded-[24px] border border-[#F3C6C6] bg-[#FFF7F7] p-6 shadow-sm">
          <p class="text-sm font-semibold text-[#B42318]">{{ error() }}</p>
        </section>
      }

      @if (superadmin(); as user) {
        <section class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <div class="rounded-2xl border border-[#C8E6C9] bg-white p-5 shadow-sm">
            <p class="text-sm font-semibold text-[#567157]">Nombre completo</p>
            <p class="mt-4 text-xl font-black text-[#1B5E20]">{{ displayName(user) }}</p>
            <p class="mt-2 text-xs text-[#6B7D6C]">Identidad visible en plataforma</p>
          </div>

          <div class="rounded-2xl border border-[#C8E6C9] bg-white p-5 shadow-sm">
            <p class="text-sm font-semibold text-[#567157]">Correo</p>
            <p class="mt-4 break-words text-xl font-black text-[#1B5E20]">{{ user.email }}</p>
            <p class="mt-2 text-xs text-[#6B7D6C]">Cuenta principal de acceso</p>
          </div>

          <div class="rounded-2xl border border-[#C8E6C9] bg-white p-5 shadow-sm">
            <p class="text-sm font-semibold text-[#567157]">Estado</p>
            <p class="mt-4 text-xl font-black text-[#1B5E20]">{{ user.active ? 'Activo' : 'Inactivo' }}</p>
            <p class="mt-2 text-xs text-[#6B7D6C]">Permiso de acceso a plataforma</p>
          </div>

          <!-- Que pero si no hay roles que carajo -->
          <!-- <div class="rounded-2xl border border-[#C8E6C9] bg-white p-5 shadow-sm">
            <p class="text-sm font-semibold text-[#567157]">Roles</p>
            <p class="mt-4 text-xl font-black text-[#1B5E20]">{{ user.roles.length }}</p>
            <p class="mt-2 text-xs text-[#6B7D6C]">Roles activos asignados</p>
          </div> -->
        </section>

        <section class="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
          <div class="rounded-[24px] border border-[#C8E6C9] bg-white p-4 sm:p-6 shadow-sm">
            <div class="mb-4 flex items-center justify-between gap-3">
              <div>
                <h2 class="text-lg font-bold text-[#1B5E20]">Datos del superadmin</h2>
                <p class="text-sm text-[#6B7D6C]">Información técnica y de sesión expuesta por el backend.</p>
              </div>
            </div>

            <div class="grid gap-4 sm:grid-cols-2">
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">ID</p>
                <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ user.id }}</p>
              </div>
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Nombre</p>
                <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ user.firstName }}</p>
              </div>
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Apellido</p>
                <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ user.lastName }}</p>
              </div>
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Correo</p>
                <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ user.email }}</p>
              </div>
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Creado</p>
                <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ formatDate(user.createdAt) }}</p>
              </div>
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Actualizado</p>
                <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ formatDate(user.updatedAt) }}</p>
              </div>
            </div>
          </div>

          <div class="space-y-6">
            <div class="rounded-[24px] border border-[#C8E6C9] bg-white p-4 sm:p-6 shadow-sm">
              <div class="flex items-center gap-3">
                <div class="rounded-2xl bg-[#F1F8E9] p-3 text-[#2E7D32]">
                  <lucide-icon name="badge-check" class="h-5 w-5"></lucide-icon>
                </div>
                <div>
                  <h3 class="text-base font-bold text-[#1B5E20]">Permisos</h3>
                  <p class="text-sm text-[#6B7D6C]">Roles y accesos actuales de plataforma.</p>
                </div>
              </div>

              <div class="mt-4 flex flex-wrap gap-2">
                @for (role of user.roles; track role) {
                  <span class="rounded-full border border-[#C8E6C9] bg-[#F1F8E9] px-3 py-1 text-xs font-semibold text-[#2E7D32]">
                    {{ role }}
                  </span>
                }
              </div>
            </div>

            <div class="rounded-[24px] border border-[#C8E6C9] bg-white p-4 sm:p-6 shadow-sm">
              <div class="flex items-center gap-3">
                <div class="rounded-2xl bg-[#F1F8E9] p-3 text-[#2E7D32]">
                  <lucide-icon name="lock" class="h-5 w-5"></lucide-icon>
                </div>
                <div>
                  <h3 class="text-base font-bold text-[#1B5E20]">Seguridad</h3>
                  <p class="text-sm text-[#6B7D6C]">Cambia la contraseña desde la sección dedicada.</p>
                </div>
              </div>

              <a
                routerLink="/platform/security"
                class="mt-4 inline-flex cursor-pointer items-center gap-2 rounded-full bg-[#1B5E20] px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-[#2E7D32]">
                <lucide-icon name="lock" class="h-4 w-4"></lucide-icon>
                Ir a seguridad
              </a>
            </div>
          </div>
        </section>
      }
    </div>
  `
})
export class PlatformProfilePageComponent implements OnInit {
  private readonly superadminMeUseCase = inject(PlatformSuperadminMeUseCase);

  readonly status = this.superadminMeUseCase.status;
  readonly superadmin = this.superadminMeUseCase.superadmin;
  readonly error = this.superadminMeUseCase.error;

  ngOnInit(): void {
    void this.superadminMeUseCase.loadCurrentSuperadmin();
  }

  reload(): void {
    void this.superadminMeUseCase.loadCurrentSuperadmin();
  }

  displayName(user: { firstName: string; lastName: string }): string {
    return `${user.firstName} ${user.lastName}`.trim();
  }

  formatDate(value: string | null | undefined): string {
    if (!value) {
      return 'N/A';
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return 'N/A';
    }

    return new Intl.DateTimeFormat('es-BO', {
      timeZone: 'America/La_Paz',
      dateStyle: 'medium',
      timeStyle: 'short'
    }).format(date);
  }
}
