import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { AuthFacade } from '../../shared/lib/auth/auth.facade';
import { AuthStorageService } from '../../shared/lib/storage/auth-storage.service';
import { PlatformSuperadminMeUseCase } from '../../features/platform/application/platform-superadmin-me.usecase';
import { PermissionService } from '../../shared/lib/auth/permission.service';

interface QuickAction {
  label: string;
  route: string;
  icon: string;
  description: string;
}

interface SettingOption {
  key: keyof SettingsFormValue;
  label: string;
  description: string;
}

interface SettingsFormValue {
  compactNavigation: boolean;
  rememberLastRoute: boolean;
  showActivityPreview: boolean;
  emailAlerts: boolean;
  pushNotifications: boolean;
  confirmSensitiveActions: boolean;
}

const DEFAULT_SETTINGS: SettingsFormValue = {
  compactNavigation: false,
  rememberLastRoute: true,
  showActivityPreview: true,
  emailAlerts: true,
  pushNotifications: true,
  confirmSensitiveActions: true
};

@Component({
  selector: 'app-settings-page',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule, LucideAngularModule],
  template: `
    <div class="space-y-6 p-4 sm:p-6 lg:p-8">
      <section class="rounded-[28px] border border-[#C8E6C9] bg-gradient-to-br from-white via-[#F7FBF3] to-[#EAF6EB] p-6 shadow-[0_18px_50px_rgba(27,94,32,0.08)]">
        <div class="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
          <div class="space-y-3">
            <div class="inline-flex items-center gap-2 rounded-full border border-[#C8E6C9] bg-white/80 px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-[#2E7D32]">
              <span class="h-2 w-2 rounded-full bg-[#4CAF50]"></span>
              {{ contextBadge }}
            </div>
            <div>
              <h1 class="text-3xl font-black tracking-tight text-[#1B5E20] sm:text-4xl">
                Configuraciones
              </h1>
              <p class="mt-2 max-w-3xl text-sm leading-6 text-[#5F6F5F]">
                {{ contextDescription }}
              </p>
            </div>
          </div>

          <div class="flex flex-wrap gap-3">
            @for (action of quickActions; track action.route) {
              <a
                [routerLink]="action.route"
                class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-[#C8E6C9] bg-white px-4 py-2 text-sm font-semibold text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
                <lucide-icon [name]="action.icon" class="h-4 w-4"></lucide-icon>
                {{ action.label }}
              </a>
            }
          </div>
        </div>
      </section>

      @if (loading && !displayEmail) {
        <section class="rounded-[24px] border border-[#C8E6C9] bg-white p-8 text-sm text-[#6B7D6C] shadow-sm">
          Cargando configuraciones...
        </section>
      }

      @if (errorMessage) {
        <section class="rounded-[24px] border border-[#F3C6C6] bg-[#FFF7F7] p-6 shadow-sm">
          <p class="text-sm font-semibold text-[#B42318]">{{ errorMessage }}</p>
        </section>
      }

      <section class="grid gap-6 xl:grid-cols-[0.95fr_1.05fr]">
        <div class="rounded-[24px] border border-[#C8E6C9] bg-white p-4 sm:p-6 shadow-sm">
          <div class="flex items-center gap-3">
            <div class="rounded-2xl bg-[#F1F8E9] p-3 text-[#2E7D32]">
              <lucide-icon [name]="contextIcon" class="h-5 w-5"></lucide-icon>
            </div>
            <div>
              <h2 class="text-lg font-bold text-[#1B5E20]">Cuenta activa</h2>
              <p class="text-sm text-[#6B7D6C]">{{ contextSubtitle }}</p>
            </div>
          </div>

          <div class="mt-6 space-y-4">
            <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
              <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Nombre</p>
              <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ displayName }}</p>
            </div>
            <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
              <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Correo</p>
              <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ displayEmail }}</p>
            </div>
            <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
              <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Rol</p>
              <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ displayRole }}</p>
            </div>
            <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
              <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Ámbito</p>
              <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ contextBadge }}</p>
            </div>
          </div>
        </div>

        <div class="rounded-[24px] border border-[#C8E6C9] bg-white p-4 sm:p-6 shadow-sm">
          <div class="flex items-center gap-3">
            <div class="rounded-2xl bg-[#F1F8E9] p-3 text-[#2E7D32]">
              <lucide-icon name="settings" class="h-5 w-5"></lucide-icon>
            </div>
            <div>
              <h2 class="text-lg font-bold text-[#1B5E20]">Preferencias</h2>
              <p class="text-sm text-[#6B7D6C]">Ajustes visuales y de comportamiento guardados localmente.</p>
            </div>
          </div>

          <form class="mt-6 space-y-3" [formGroup]="form" (ngSubmit)="save()">
            @for (option of settingOptions; track option.key) {
              <label class="flex items-start justify-between gap-4 rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <div>
                  <p class="text-sm font-semibold text-[#1B5E20]">{{ option.label }}</p>
                  <p class="mt-1 text-xs leading-5 text-[#6B7D6C]">{{ option.description }}</p>
                </div>
                <input
                  class="mt-1 h-5 w-5 rounded border-[#C8E6C9] text-[#2E7D32] focus:ring-[#2E7D32]"
                  type="checkbox"
                  [formControlName]="option.key">
              </label>
            }

            <div class="flex flex-wrap gap-3 pt-2">
              <button
                type="submit"
                class="inline-flex cursor-pointer items-center gap-2 rounded-full bg-[#1B5E20] px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-[#2E7D32]">
                <lucide-icon name="save" class="h-4 w-4"></lucide-icon>
                Guardar cambios
              </button>
              <button
                type="button"
                (click)="reset()"
                class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-[#C8E6C9] bg-white px-4 py-2 text-sm font-semibold text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
                <lucide-icon name="rotate-ccw" class="h-4 w-4"></lucide-icon>
                Restaurar
              </button>
            </div>

            @if (successMessage) {
              <div class="rounded-2xl border border-[#C8E6C9] bg-[#F1F8E9] px-4 py-3 text-sm font-medium text-[#1B5E20]">
                {{ successMessage }}
              </div>
            }

            @if (formError) {
              <div class="rounded-2xl border border-[#F3C6C6] bg-[#FFF7F7] px-4 py-3 text-sm font-medium text-[#B42318]">
                {{ formError }}
              </div>
            }
          </form>
        </div>
      </section>
    </div>
  `
})
export class SettingsPageComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly authFacade = inject(AuthFacade);
  private readonly authStorage = inject(AuthStorageService);
  private readonly platformSuperadminMeUseCase = inject(PlatformSuperadminMeUseCase);
  private readonly permissionService = inject(PermissionService);

  readonly isPlatformRoute = this.router.url.startsWith('/platform');

  readonly contextBadge = this.isPlatformRoute ? 'Plataforma superadmin' : 'Tenant y cliente';
  readonly contextDescription = this.isPlatformRoute
    ? 'Ajusta preferencias de trabajo para la plataforma, la auditoría y tu experiencia operativa.'
    : 'Personaliza tu experiencia dentro del tenant sin cambiar la paleta ni el patrón visual actual.';
  readonly contextSubtitle = this.isPlatformRoute
    ? 'Perfil y preferencias del superadmin de plataforma.'
    : 'Perfil y preferencias del usuario autenticado en el tenant.';
  readonly contextIcon = this.isPlatformRoute ? 'shield' : 'user-circle-2';

  readonly settingOptions: SettingOption[] = [
    {
      key: 'compactNavigation',
      label: 'Navegación compacta',
      description: 'Reduce el espacio visual del panel para priorizar la información operativa.'
    },
    {
      key: 'rememberLastRoute',
      label: 'Recordar última ruta',
      description: 'Al volver a entrar, intenta mantener el último módulo abierto.'
    },
    {
      key: 'showActivityPreview',
      label: 'Mostrar actividad reciente',
      description: 'Visualiza un resumen rápido de la actividad y los eventos recientes.'
    },
    {
      key: 'emailAlerts',
      label: 'Alertas por correo',
      description: 'Recibe resúmenes y avisos importantes en tu correo registrado.'
    },
    {
      key: 'pushNotifications',
      label: 'Notificaciones push',
      description: 'Activa avisos inmediatos en el navegador cuando haya eventos nuevos.'
    },
    {
      key: 'confirmSensitiveActions',
      label: 'Confirmar acciones críticas',
      description: 'Pide confirmación adicional antes de ejecutar cambios sensibles.'
    }
  ];

  get quickActions(): QuickAction[] {
    if (this.isPlatformRoute) {
      return [
        { label: 'Dashboard', route: '/platform/dashboard', icon: 'layout-dashboard', description: 'Resumen general de la plataforma' },
        { label: 'Mi perfil', route: '/platform/profile', icon: 'user-circle-2', description: 'Datos visibles de tu cuenta' },
        { label: 'Seguridad', route: '/platform/security', icon: 'lock', description: 'Cambio de contraseña y acceso' },
        { label: 'Respaldos', route: '/platform/backups', icon: 'database', description: 'Administración de backups' }
      ];
    }

    const quickActions: QuickAction[] = [
      { label: 'Dashboard', route: this.dashboardRoute, icon: 'layout-dashboard', description: 'Resumen principal del tenant' },
      { label: 'Usuarios', route: '/dashboard/users', icon: 'users', description: 'Usuarios del tenant' },
      { label: 'Roles y permisos', route: '/dashboard/roles', icon: 'key', description: 'Control de acceso del tenant' }
    ];

    if (this.canAccessSubscription()) {
      quickActions.push({
        label: 'Suscripción',
        route: '/dashboard/subscription',
        icon: 'credit-card',
        description: 'Plan y facturación'
      });
    }

    return quickActions;
  }

  readonly form = this.fb.nonNullable.group(DEFAULT_SETTINGS);

  loading = true;
  errorMessage = '';
  formError = '';
  successMessage = '';
  displayName = '';
  displayEmail = '';
  displayRole = '';

  private preferencesStorageKey = '';

  get dashboardRoute(): string {
    return this.authFacade.getTenantLandingRoute();
  }

  private canAccessSubscription(): boolean {
    return this.permissionService.hasPermission('billing.subscription.manage')
      || this.permissionService.hasRole('OWNER_ADMIN');
  }

  async ngOnInit(): Promise<void> {
    this.loading = true;
    this.formError = '';
    this.successMessage = '';

    if (this.isPlatformRoute) {
      await this.platformSuperadminMeUseCase.loadCurrentSuperadmin();
      const superadmin = this.platformSuperadminMeUseCase.superadmin();
      this.displayName = superadmin ? `${superadmin.firstName} ${superadmin.lastName}`.trim() : 'SuperAdmin';
      this.displayEmail = superadmin?.email || 'superadmin@hrcore.local';
      this.displayRole = 'Super Administrador';
      this.preferencesStorageKey = this.buildStorageKey('platform', this.displayEmail);
    } else {
      await this.authFacade.loadCurrentUser();
      const user = this.authFacade.currentUser();
      this.displayName = user ? `${user.firstName} ${user.lastName}`.trim() : 'Usuario';
      this.displayEmail = user?.email || 'usuario@hrcore.local';
      this.displayRole = this.authFacade.getTenantLandingRoute() === '/dashboard/summary' ? 'Owner Admin' : 'Usuario';
      this.preferencesStorageKey = this.buildStorageKey('tenant', this.displayEmail);
    }

    this.loadPreferences();
    this.loading = false;
  }

  save(): void {
    localStorage.setItem(this.preferencesStorageKey, JSON.stringify(this.form.getRawValue()));
    this.successMessage = 'Configuraciones guardadas localmente.';
    this.formError = '';
  }

  reset(): void {
    this.form.reset(DEFAULT_SETTINGS);
    localStorage.removeItem(this.preferencesStorageKey);
    this.successMessage = 'Configuraciones restauradas a valores por defecto.';
    this.formError = '';
  }

  private loadPreferences(): void {
    const stored = localStorage.getItem(this.preferencesStorageKey);
    if (!stored) {
      this.form.reset(DEFAULT_SETTINGS);
      return;
    }

    try {
      const parsed = JSON.parse(stored) as Partial<SettingsFormValue>;
      this.form.reset({
        compactNavigation: Boolean(parsed.compactNavigation ?? DEFAULT_SETTINGS.compactNavigation),
        rememberLastRoute: Boolean(parsed.rememberLastRoute ?? DEFAULT_SETTINGS.rememberLastRoute),
        showActivityPreview: Boolean(parsed.showActivityPreview ?? DEFAULT_SETTINGS.showActivityPreview),
        emailAlerts: Boolean(parsed.emailAlerts ?? DEFAULT_SETTINGS.emailAlerts),
        pushNotifications: Boolean(parsed.pushNotifications ?? DEFAULT_SETTINGS.pushNotifications),
        confirmSensitiveActions: Boolean(parsed.confirmSensitiveActions ?? DEFAULT_SETTINGS.confirmSensitiveActions)
      });
    } catch {
      this.form.reset(DEFAULT_SETTINGS);
    }
  }

  private buildStorageKey(scope: 'tenant' | 'platform', email: string): string {
    const tenantPart = scope === 'tenant' ? this.normalizeKey(this.authStorage.getTenantSlug() || 'tenant') : 'platform';
    return `hrcore-settings:${scope}:${tenantPart}:${this.normalizeKey(email)}`;
  }

  private normalizeKey(value: string): string {
    return encodeURIComponent(value.trim().toLowerCase().replace(/\s+/g, '-'));
  }
}
