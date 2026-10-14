import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { AuthFacade } from '../../shared/lib/auth/auth.facade';
import { AuthStorageService } from '../../shared/lib/storage/auth-storage.service';
import { PlatformSuperadminMeUseCase } from '../../features/platform/application/platform-superadmin-me.usecase';
import { PermissionService } from '../../shared/lib/auth/permission.service';
import { TenantNavigationService } from '../../shared/lib/navigation/tenant-navigation.service';
import { TENANT_NAVIGATION } from '../../shared/lib/navigation/tenant-navigation.config';

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
      <section class="rounded-[28px] border border-border bg-gradient-to-br from-card via-background to-muted p-6 shadow-lg">
        <div class="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
          <div class="space-y-3">
            <div class="inline-flex items-center gap-2 rounded-full border border-border bg-card/80 px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-primary">
              <span class="h-2 w-2 rounded-full bg-secondary"></span>
              {{ contextBadge }}
            </div>
            <div>
              <h1 class="text-3xl font-black tracking-tight text-accent-foreground sm:text-4xl">
                Configuraciones
              </h1>
              <p class="mt-2 max-w-3xl text-sm leading-6 text-muted-foreground">
                {{ contextDescription }}
              </p>
            </div>
          </div>

          <div class="flex flex-wrap gap-3">
            @for (action of quickActions; track action.route) {
              <a
                [routerLink]="action.route"
                class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-border bg-card px-4 py-2 text-sm font-semibold text-primary transition-colors hover:bg-accent">
                <lucide-icon [name]="action.icon" class="h-4 w-4"></lucide-icon>
                {{ action.label }}
              </a>
            }
          </div>
        </div>
      </section>

      @if (loading && !displayEmail) {
        <section class="rounded-[24px] border border-border bg-card p-8 text-sm text-muted-foreground shadow-sm">
          Cargando configuraciones...
        </section>
      }

      @if (errorMessage) {
        <section class="rounded-[24px] border border-border bg-muted p-6 shadow-sm">
          <p class="text-sm font-semibold text-foreground">{{ errorMessage }}</p>
        </section>
      }

      <section class="grid gap-6 xl:grid-cols-[0.95fr_1.05fr]">
        <div class="rounded-[24px] border border-border bg-card p-4 sm:p-6 shadow-sm">
          <div class="flex items-center gap-3">
            <div class="rounded-2xl bg-accent p-3 text-primary">
              <lucide-icon [name]="contextIcon" class="h-5 w-5"></lucide-icon>
            </div>
            <div>
              <h2 class="text-lg font-bold text-accent-foreground">Cuenta activa</h2>
              <p class="text-sm text-muted-foreground">{{ contextSubtitle }}</p>
            </div>
          </div>

          <div class="mt-6 space-y-4">
            <div class="rounded-2xl border border-border bg-muted p-4">
              <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Nombre</p>
              <p class="mt-1 break-words text-sm font-semibold text-accent-foreground">{{ displayName }}</p>
            </div>
            <div class="rounded-2xl border border-border bg-muted p-4">
              <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Correo</p>
              <p class="mt-1 break-words text-sm font-semibold text-accent-foreground">{{ displayEmail }}</p>
            </div>
            <div class="rounded-2xl border border-border bg-muted p-4">
              <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Rol</p>
              <p class="mt-1 break-words text-sm font-semibold text-accent-foreground">{{ displayRole }}</p>
            </div>
            <div class="rounded-2xl border border-border bg-muted p-4">
              <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Ámbito</p>
              <p class="mt-1 break-words text-sm font-semibold text-accent-foreground">{{ contextBadge }}</p>
            </div>
          </div>
        </div>

        <div class="rounded-[24px] border border-border bg-card p-4 sm:p-6 shadow-sm">
          <div class="flex items-center gap-3">
            <div class="rounded-2xl bg-accent p-3 text-primary">
              <lucide-icon name="settings" class="h-5 w-5"></lucide-icon>
            </div>
            <div>
              <h2 class="text-lg font-bold text-accent-foreground">Preferencias</h2>
              <p class="text-sm text-muted-foreground">Ajustes visuales y de comportamiento guardados localmente.</p>
            </div>
          </div>

          <form class="mt-6 space-y-3" [formGroup]="form" (ngSubmit)="save()">
            @for (option of settingOptions; track option.key) {
              <label class="flex items-start justify-between gap-4 rounded-2xl border border-border bg-muted p-4">
                <div>
                  <p class="text-sm font-semibold text-accent-foreground">{{ option.label }}</p>
                  <p class="mt-1 text-xs leading-5 text-muted-foreground">{{ option.description }}</p>
                </div>
                <input
                  class="mt-1 h-5 w-5 rounded border-border text-primary focus:ring-primary"
                  type="checkbox"
                  [formControlName]="option.key">
              </label>
            }

            <div class="flex flex-wrap gap-3 pt-2">
              <button
                type="submit"
                class="inline-flex cursor-pointer items-center gap-2 rounded-full bg-accent-foreground px-4 py-2 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary">
                <lucide-icon name="save" class="h-4 w-4"></lucide-icon>
                Guardar cambios
              </button>
              <button
                type="button"
                (click)="reset()"
                class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-border bg-card px-4 py-2 text-sm font-semibold text-primary transition-colors hover:bg-accent">
                <lucide-icon name="rotate-ccw" class="h-4 w-4"></lucide-icon>
                Restaurar
              </button>
            </div>

            @if (successMessage) {
              <div class="rounded-2xl border border-border bg-accent px-4 py-3 text-sm font-medium text-accent-foreground">
                {{ successMessage }}
              </div>
            }

            @if (formError) {
              <div class="rounded-2xl border border-border bg-muted px-4 py-3 text-sm font-medium text-foreground">
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
  private readonly tenantNavigation = inject(TenantNavigationService);

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

    const users = TENANT_NAVIGATION.find(item => item.id === 'users');
    const roles = TENANT_NAVIGATION.find(item => item.id === 'roles');
    return quickActions.filter(action => action.route === users?.route ? !!users && this.tenantNavigation.canAccess(users) : action.route === roles?.route ? !!roles && this.tenantNavigation.canAccess(roles) : true).concat(this.canAccessSubscription() ? [{ label: 'Suscripción', route: '/dashboard/subscription', icon: 'credit-card', description: 'Plan y facturación' }] : []);
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
    return this.permissionService.isCurrentOwner() && this.permissionService.hasPermission('billing.subscription.manage');
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
      this.displayRole = this.permissionService.isCurrentOwner() ? 'Owner Admin' : 'Usuario';
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
