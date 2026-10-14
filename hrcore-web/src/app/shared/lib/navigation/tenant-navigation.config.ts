import { TENANT_ROUTE_PERMISSIONS } from './tenant-route-permissions';

export type NavigationScope = 'TENANT' | 'PLATFORM';
export interface TenantNavigationItem {
  id: string;
  label: string;
  route: string;
  icon: string;
  permission?: string;
  ownerOnly?: boolean;
}

export const TENANT_NAVIGATION: TenantNavigationItem[] = [
  { id: 'home', label: 'Inicio', route: '/dashboard/home', icon: 'layout-dashboard' },
  { id: 'users', label: 'Usuarios', route: '/dashboard/users', icon: 'users', permission: TENANT_ROUTE_PERMISSIONS.users },
  { id: 'employees', label: 'Empleados', route: '/dashboard/employees', icon: 'contact-round', permission: TENANT_ROUTE_PERMISSIONS.employees },
  { id: 'contracts', label: 'Contratos', route: '/dashboard/contracts', icon: 'file-text', permission: TENANT_ROUTE_PERMISSIONS.contracts },
  { id: 'departments', label: 'Departamentos', route: '/dashboard/departments', icon: 'building-2', permission: TENANT_ROUTE_PERMISSIONS.departments },
  { id: 'positions', label: 'Cargos', route: '/dashboard/positions', icon: 'briefcase', permission: TENANT_ROUTE_PERMISSIONS.positions },
  { id: 'roles', label: 'Roles', route: '/dashboard/roles', icon: 'key', permission: TENANT_ROUTE_PERMISSIONS.roles, ownerOnly: true },
  { id: 'permissions', label: 'Permisos', route: '/dashboard/permissions', icon: 'badge-check', permission: TENANT_ROUTE_PERMISSIONS.permissions, ownerOnly: true },
  { id: 'subscription', label: 'Suscripción', route: '/dashboard/subscription', icon: 'credit-card', permission: TENANT_ROUTE_PERMISSIONS.subscription, ownerOnly: true },
  { id: 'backups', label: 'Respaldos', route: '/dashboard/backups', icon: 'archive', permission: TENANT_ROUTE_PERMISSIONS.backups },
  { id: 'notifications', label: 'Notificaciones', route: '/dashboard/notifications', icon: 'bell' },
  { id: 'profile', label: 'Mi perfil', route: '/dashboard/profile', icon: 'user-circle-2' },
  { id: 'security', label: 'Seguridad', route: '/dashboard/security', icon: 'lock' },
  { id: 'settings', label: 'Preferencias', route: '/dashboard/settings', icon: 'settings' },
];
