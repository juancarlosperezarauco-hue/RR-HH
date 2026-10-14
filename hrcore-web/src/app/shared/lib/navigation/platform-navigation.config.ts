import { TenantNavigationItem } from './tenant-navigation.config';

export const PLATFORM_NAVIGATION: TenantNavigationItem[] = [
  { id: 'dashboard', label: 'Dashboard', route: '/platform/dashboard', icon: 'layout-dashboard' },
  { id: 'tenants', label: 'Tenants', route: '/platform/tenants', icon: 'building-2' },
  { id: 'plans', label: 'Planes', route: '/platform/plans', icon: 'credit-card' },
  { id: 'subscriptions', label: 'Suscripciones', route: '/platform/subscriptions', icon: 'dollar-sign' },
  { id: 'audit', label: 'Auditoría', route: '/platform/audit', icon: 'clipboard-list' },
  { id: 'backups', label: 'Respaldos', route: '/platform/backups', icon: 'archive' },
  { id: 'profile', label: 'Mi perfil', route: '/platform/profile', icon: 'user-circle-2' },
  { id: 'security', label: 'Seguridad', route: '/platform/security', icon: 'lock' },
  { id: 'settings', label: 'Preferencias', route: '/platform/settings', icon: 'settings' },
];
