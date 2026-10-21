import { TENANT_ROUTE_PERMISSIONS } from './tenant-route-permissions';
import { TenantTextKey } from '../i18n/keys';

export type NavigationScope = 'TENANT' | 'PLATFORM';
export interface TenantNavigationItem {
  id: string;
  label: string;
  labelKey?: TenantTextKey;
  route: string;
  icon: string;
  permission?: string;
  ownerOnly?: boolean;
  hideForOwner?: boolean;
  /** Rutas propias de la cuenta, protegidas por sesión y no por un permiso de módulo. */
  authOnly?: boolean;
}

export const TENANT_NAVIGATION: TenantNavigationItem[] = [
  { id: 'home', labelKey: 'nav.home', label: 'Inicio', route: '/dashboard/home', icon: 'layout-dashboard', authOnly: true },
  { id: 'users', labelKey: 'nav.users', label: 'Usuarios', route: '/dashboard/users', icon: 'users', permission: TENANT_ROUTE_PERMISSIONS.users },
  { id: 'departments', labelKey: 'nav.departments', label: 'Departamentos', route: '/dashboard/departments', icon: 'building-2', permission: TENANT_ROUTE_PERMISSIONS.departments },
  { id: 'positions', labelKey: 'nav.positions', label: 'Cargos', route: '/dashboard/positions', icon: 'briefcase', permission: TENANT_ROUTE_PERMISSIONS.positions },
  { id: 'organization-chart', labelKey: 'nav.organization-chart', label: 'Organigrama', route: '/dashboard/organization-chart', icon: 'network', permission: TENANT_ROUTE_PERMISSIONS.departments },
  { id: 'my-team', labelKey: 'nav.my-team', label: 'Mi equipo', route: '/dashboard/my-team', icon: 'users-round', permission: TENANT_ROUTE_PERMISSIONS.team, hideForOwner: true },
  { id: 'employees', labelKey: 'nav.employees', label: 'Empleados', route: '/dashboard/employees', icon: 'user', permission: TENANT_ROUTE_PERMISSIONS.employees },
  { id: 'contracts', labelKey: 'nav.contracts', label: 'Contratos', route: '/dashboard/contracts', icon: 'file-text', permission: TENANT_ROUTE_PERMISSIONS.contracts },
  { id: 'dossiers', labelKey: 'nav.dossiers', label: 'Expedientes', route: '/dashboard/dossiers', icon: 'folder-open', permission: TENANT_ROUTE_PERMISSIONS.dossiers },
  { id: 'shifts', labelKey: 'nav.shifts', label: 'Turnos', route: '/dashboard/shifts', icon: 'clock', permission: TENANT_ROUTE_PERMISSIONS.shifts },
  { id: 'absences', labelKey: 'nav.absences', label: 'Ausencias', route: '/dashboard/absences', icon: 'calendar', permission: TENANT_ROUTE_PERMISSIONS.absences },
  { id: 'incidents', labelKey: 'nav.incidents', label: 'Incidencias', route: '/dashboard/incidents', icon: 'alert-triangle', permission: TENANT_ROUTE_PERMISSIONS.incidents },
  { id: 'attendance', labelKey: 'nav.attendance', label: 'Asistencia', route: '/dashboard/attendance', icon: 'clipboard-list', permission: TENANT_ROUTE_PERMISSIONS.attendance },
  { id: 'roles', labelKey: 'nav.roles', label: 'Roles', route: '/dashboard/roles', icon: 'key', permission: TENANT_ROUTE_PERMISSIONS.roles, ownerOnly: true },
  { id: 'permissions', labelKey: 'nav.permissions', label: 'Permisos', route: '/dashboard/permissions', icon: 'badge-check', permission: TENANT_ROUTE_PERMISSIONS.permissions, ownerOnly: true },
  { id: 'subscription', labelKey: 'nav.subscription', label: 'Suscripción', route: '/dashboard/subscription', icon: 'credit-card', permission: TENANT_ROUTE_PERMISSIONS.subscription, ownerOnly: true },
  { id: 'backups', labelKey: 'nav.backups', label: 'Respaldos', route: '/dashboard/backups', icon: 'archive', permission: TENANT_ROUTE_PERMISSIONS.backups },
  { id: 'notifications', labelKey: 'nav.notifications', label: 'Notificaciones', route: '/dashboard/notifications', icon: 'bell', authOnly: true },
  { id: 'profile', labelKey: 'nav.profile', label: 'Mi perfil', route: '/dashboard/profile', icon: 'user-circle-2', authOnly: true },
  { id: 'my-employment', labelKey: 'nav.my-employment', label: 'Mi información laboral', route: '/dashboard/my-employment', icon: 'user-circle-2', permission: TENANT_ROUTE_PERMISSIONS.selfEmployment, hideForOwner: true },
  { id: 'hr-reports', labelKey: 'nav.hr-reports', label: 'Reportes de RR. HH.', route: '/dashboard/hr-reports', icon: 'bar-chart-3', permission: TENANT_ROUTE_PERMISSIONS.reportsEmployees },
  { id: 'security', labelKey: 'nav.security', label: 'Seguridad', route: '/dashboard/security', icon: 'lock', authOnly: true },
  { id: 'settings', labelKey: 'nav.settings', label: 'Preferencias', route: '/dashboard/settings', icon: 'settings', authOnly: true },
  { id: 'company-settings', labelKey: 'nav.company-settings', label: 'Empresa', route: '/dashboard/company-settings', icon: 'building', permission: TENANT_ROUTE_PERMISSIONS.companySettings, ownerOnly: true },
  { id: 'appearance-settings', labelKey: 'nav.appearance-settings', label: 'Personalización del Sistema', route: '/dashboard/appearance-settings', icon: 'wand-2', permission: TENANT_ROUTE_PERMISSIONS.tenantSettings, ownerOnly: true },
  { id: 'audit', labelKey: 'nav.audit', label: 'Auditoría', route: '/dashboard/audit', icon: 'clipboard-list', permission: TENANT_ROUTE_PERMISSIONS.audit, ownerOnly: true },
];
