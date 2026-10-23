import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { LucideAngularModule } from 'lucide-angular';
import { AuthFacade } from '../../shared/lib/auth/auth.facade';
import { AuthStorageService } from '../../shared/lib/storage/auth-storage.service';
import { PlatformSuperadminMeUseCase } from '../../features/platform/application/platform-superadmin-me.usecase';
import { PermissionService } from '../../shared/lib/auth/permission.service';
import { TenantNavigationService } from '../../shared/lib/navigation/tenant-navigation.service';
import { TENANT_NAVIGATION } from '../../shared/lib/navigation/tenant-navigation.config';
import { NotificationsService } from '../../entities/notifications/api/notifications.service';
import { PushNotificationsService } from '../../entities/notifications/api/push-notifications.service';
import { NotificationDeviceResponse } from '../../entities/notifications/model/notifications.model';

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

      @if (!isPlatformRoute) {
        <section class="rounded-[24px] border border-border bg-card p-4 shadow-sm sm:p-6">
          <div class="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
            <div class="flex items-start gap-3">
              <div class="rounded-2xl bg-accent p-3 text-primary">
                <lucide-icon name="bell-ring" class="h-5 w-5"></lucide-icon>
              </div>
              <div>
                <h2 class="text-lg font-bold text-accent-foreground">Notificaciones push</h2>
                <p class="mt-1 max-w-2xl text-sm leading-5 text-muted-foreground">
                  Comprueba si este navegador puede recibir avisos inmediatos y administra sus dispositivos registrados.
                </p>
              </div>
            </div>
            <span class="inline-flex w-fit items-center gap-2 rounded-full border px-3 py-1 text-xs font-bold"
              [class.border-secondary/40]="pushStatus === 'active'"
              [class.bg-secondary/10]="pushStatus === 'active'"
              [class.text-secondary-foreground]="pushStatus === 'active'"
              [class.border-border]="pushStatus !== 'active'"
              [class.bg-muted]="pushStatus !== 'active'"
              [class.text-muted-foreground]="pushStatus !== 'active'">
              <span class="h-2 w-2 rounded-full" [class.bg-secondary]="pushStatus === 'active'" [class.bg-muted-foreground]="pushStatus !== 'active'"></span>
              {{ pushStatusLabel }}
            </span>
          </div>

          <div class="mt-5 grid gap-3 md:grid-cols-3">
            <div class="rounded-2xl border border-border bg-muted p-4">
              <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Permiso del navegador</p>
              <p class="mt-1 text-sm font-semibold text-accent-foreground">{{ browserPermissionLabel }}</p>
            </div>
            <div class="rounded-2xl border border-border bg-muted p-4">
              <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Dispositivos activos</p>
              <p class="mt-1 text-sm font-semibold text-accent-foreground">{{ activePushDevices.length }}</p>
            </div>
            <div class="rounded-2xl border border-border bg-muted p-4">
              <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Preferencia push</p>
              <p class="mt-1 text-sm font-semibold text-accent-foreground">{{ pushPreferenceEnabled === false ? 'Desactivada' : 'Activada' }}</p>
            </div>
          </div>

          <div class="mt-4 flex flex-wrap items-center gap-3">
            <button type="button" (click)="activatePush()" [disabled]="pushActionLoading"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full bg-accent-foreground px-4 py-2 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary disabled:cursor-not-allowed disabled:opacity-60">
              <lucide-icon name="bell-plus" class="h-4 w-4"></lucide-icon>
              {{ pushActionLoading ? 'Activando...' : 'Activar en este navegador' }}
            </button>
            <button type="button" (click)="loadPushStatus()" [disabled]="pushLoading"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-border bg-card px-4 py-2 text-sm font-semibold text-primary transition-colors hover:bg-accent disabled:cursor-not-allowed disabled:opacity-60">
              <lucide-icon name="refresh-cw" class="h-4 w-4"></lucide-icon>
              Actualizar estado
            </button>
          </div>

          @if (pushActionMessage) {
            <p class="mt-3 rounded-2xl border border-secondary/30 bg-secondary/10 px-4 py-3 text-sm font-medium text-accent-foreground">{{ pushActionMessage }}</p>
          }
          @if (pushActionError) {
            <p class="mt-3 rounded-2xl border border-destructive/30 bg-destructive/10 px-4 py-3 text-sm font-medium text-foreground">{{ pushActionError }}</p>
          }

          @if (pushDevices.length > 0) {
            <div class="mt-5 space-y-2">
              <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Dispositivos registrados</p>
              @for (device of pushDevices; track device.id) {
                <div class="flex flex-col gap-3 rounded-2xl border border-border bg-muted p-4 sm:flex-row sm:items-center sm:justify-between">
                  <div class="flex items-start gap-3">
                    <lucide-icon name="monitor" class="mt-0.5 h-4 w-4 text-primary"></lucide-icon>
                    <div>
                      <p class="text-sm font-semibold text-accent-foreground">{{ device.deviceName || 'Navegador web' }}</p>
                      <p class="mt-1 text-xs text-muted-foreground">{{ device.platform }} · {{ device.status === 'ACTIVE' ? 'Activo' : 'Inactivo' }}</p>
                    </div>
                  </div>
                  @if (device.status === 'ACTIVE') {
                    <button type="button" (click)="deactivatePushDevice(device)" [disabled]="pushActionLoading"
                      class="inline-flex cursor-pointer items-center gap-2 self-start rounded-full border border-border bg-card px-3 py-1.5 text-xs font-semibold text-primary transition-colors hover:bg-accent disabled:opacity-60 sm:self-auto">
                      Desactivar
                    </button>
                  }
                </div>
              }
            </div>
          }
        </section>
      }
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
  private readonly notificationsService = inject(NotificationsService);
  private readonly pushNotifications = inject(PushNotificationsService);

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
  pushDevices: NotificationDeviceResponse[] = [];
  pushPreferenceEnabled: boolean | null = null;
  pushLoading = false;
  pushActionLoading = false;
  pushActionMessage = '';
  pushActionError = '';
  displayName = '';
  displayEmail = '';
  displayRole = '';

  private preferencesStorageKey = '';

  get dashboardRoute(): string {
    return this.authFacade.getTenantLandingRoute();
  }

  get activePushDevices(): NotificationDeviceResponse[] {
    return this.pushDevices.filter(device => device.status === 'ACTIVE');
  }

  get pushStatus(): 'active' | 'inactive' {
    return this.activePushDevices.length > 0 && this.pushPreferenceEnabled !== false ? 'active' : 'inactive';
  }

  get pushStatusLabel(): string {
    if (this.pushLoading) return 'Consultando...';
    if (this.pushStatus === 'active') return 'Push activo';
    if (this.browserPermission === 'denied') return 'Permiso bloqueado';
    return 'Sin dispositivo activo';
  }

  get browserPermission(): NotificationPermission | 'unsupported' {
    return typeof Notification === 'undefined' ? 'unsupported' : Notification.permission;
  }

  get browserPermissionLabel(): string {
    if (this.browserPermission === 'granted') return 'Concedido';
    if (this.browserPermission === 'denied') return 'Bloqueado';
    if (this.browserPermission === 'unsupported') return 'No disponible';
    return 'Pendiente';
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
    if (!this.isPlatformRoute) await this.loadPushStatus();
    this.loading = false;
  }

  async loadPushStatus(): Promise<void> {
    this.pushLoading = true;
    this.pushActionError = '';
    try {
      const [devicesResponse, preferencesResponse] = await Promise.all([
        firstValueFrom(this.notificationsService.listDevices()),
        firstValueFrom(this.notificationsService.listPreferences())
      ]);
      this.pushDevices = devicesResponse.data?.content ?? [];
      const systemPreference = preferencesResponse.data?.content?.find(preference => preference.category === 'SYSTEM');
      this.pushPreferenceEnabled = systemPreference?.pushEnabled ?? true;
    } catch {
      this.pushActionError = 'No se pudo consultar el estado de las notificaciones push.';
    } finally {
      this.pushLoading = false;
    }
  }

  async activatePush(): Promise<void> {
    this.pushActionLoading = true;
    this.pushActionMessage = '';
    this.pushActionError = '';
    try {
      await this.pushNotifications.registerCurrentDevice();
      await this.loadPushStatus();
      this.pushActionMessage = this.pushStatus === 'active'
        ? 'Este navegador ya está registrado para recibir notificaciones.'
        : 'No se pudo registrar el navegador. Revisa el permiso de notificaciones.';
    } catch {
      this.pushActionError = 'No se pudo activar este navegador para notificaciones push.';
    } finally {
      this.pushActionLoading = false;
    }
  }

  async deactivatePushDevice(device: NotificationDeviceResponse): Promise<void> {
    this.pushActionLoading = true;
    this.pushActionMessage = '';
    this.pushActionError = '';
    try {
      await firstValueFrom(this.notificationsService.deactivateDevice(device.id));
      await this.loadPushStatus();
      this.pushActionMessage = 'Dispositivo desactivado.';
    } catch {
      this.pushActionError = 'No se pudo desactivar el dispositivo.';
    } finally {
      this.pushActionLoading = false;
    }
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
