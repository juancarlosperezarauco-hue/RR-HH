import { Routes } from '@angular/router';
import { authGuard, permissionGuard } from './shared/api';
import { platformAuthGuard } from './shared/api/guards/platform-auth.guard';
import { tenantRoleGuard } from './shared/api/guards/tenant-role.guard';
import { LandingPageComponent } from './pages/landing-page/landing-page.component';
import { PlatformLayoutComponent } from './shared/ui/layouts/platform-layout.component';

export const routes: Routes = [
  // ============================================================
  // PÚBLICAS
  // ============================================================
  
  // Landing page
  { path: '', component: LandingPageComponent },
  {
    path: 'prices',
    loadComponent: () => import('./pages/prices-page/prices-page.component').then(m => m.PricesPageComponent)
  },
  
  // Login de tenant
  { path: 'login', loadComponent: () => import('./pages/login-page/login-page.component').then(m => m.LoginPageComponent) },

  // Recuperación de contraseña
  { path: 'forgot-password', loadComponent: () => import('./pages/forgot-password-page/forgot-password-page.component').then(m => m.ForgotPasswordPageComponent) },
  { path: 'reset-password', loadComponent: () => import('./pages/reset-password-page/reset-password-page.component').then(m => m.ResetPasswordPageComponent) },

  // Activación de cuenta (confirmación de correo)
  { path: 'activate', loadComponent: () => import('./pages/activate-account-page/activate-account-page.component').then(m => m.ActivateAccountPageComponent) },
  { path: 'verify-email', loadComponent: () => import('./pages/activate-account-page/activate-account-page.component').then(m => m.ActivateAccountPageComponent) },
  { path: 'activate-account', loadComponent: () => import('./pages/activate-account-page/activate-account-page.component').then(m => m.ActivateAccountPageComponent) },

  // Registro público de tenant
  { path: 'onboarding', loadComponent: () => import('./pages/onboarding-page/onboarding-page.component').then(m => m.OnboardingPageComponent) },

  {
    path: 'billing/success',
    loadComponent: () => import('./pages/billing-success-page/billing-success-page.component').then(m => m.BillingSuccessPageComponent)
  },
  {
    path: 'billing/cancel',
    loadComponent: () => import('./pages/billing-cancel-page/billing-cancel-page.component').then(m => m.BillingCancelPageComponent)
  },
  
  // Login de SuperAdmin
  { path: 'platform/login', loadComponent: () => import('./pages/platform-login-page/platform-login-page.component').then(m => m.PlatformLoginPageComponent) },

  // ============================================================
  // TENANT (usuario normal)
  // ============================================================
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/dashboard-page/dashboard-page.component').then(m => m.DashboardPageComponent),
    children: [
      {
        path: 'users',
        canActivate: [permissionGuard('users.list')],
        loadComponent: () => import('./pages/users-page/users-page.component').then(m => m.UsersPageComponent)
      },
      {
        path: 'roles',
        canActivate: [permissionGuard('access.roles.read')],
        loadComponent: () => import('./pages/roles-page/roles-page.component').then(m => m.RolesPageComponent)
      },
      {
        path: 'permissions',
        canActivate: [permissionGuard('access.permissions.read')],
        loadComponent: () => import('./pages/permissions-page/permissions-page.component').then(m => m.PermissionsPageComponent)
      },
      { path: 'settings', loadComponent: () => import('./pages/settings-page/settings-page.component').then(m => m.SettingsPageComponent) },
      {
        path: 'subscription',
        canActivate: [permissionGuard('billing.subscription.manage')],
        loadComponent: () => import('./pages/subscription-settings-page/subscription-settings-page.component').then(m => m.SubscriptionSettingsPageComponent)
      },
      {
        path: 'subscription/success',
        canActivate: [permissionGuard('billing.subscription.manage')],
        loadComponent: () => import('./pages/subscription-upgrade-success-page/subscription-upgrade-success-page.component').then(m => m.SubscriptionUpgradeSuccessPageComponent)
      },
      {
        path: 'subscription/cancel',
        canActivate: [permissionGuard('billing.subscription.manage')],
        loadComponent: () => import('./pages/subscription-upgrade-cancel-page/subscription-upgrade-cancel-page.component').then(m => m.SubscriptionUpgradeCancelPageComponent)
      },
      { path: 'profile', loadComponent: () => import('./pages/profile-page/profile-page.component').then(m => m.ProfilePageComponent) },
      {
        path: 'backups',
        canActivate: [permissionGuard('backups.list')],
        loadComponent: () => import('./pages/backups-page/backups-page.component').then(m => m.BackupsPageComponent)
      },
      {
        path: 'notifications',
        loadComponent: () => import('./pages/notifications-page/notifications-page.component').then(m => m.NotificationsPageComponent)
      },
      { path: '', redirectTo: 'users', pathMatch: 'full' }
    ]
  },

  // ============================================================
  // PLATFORM (SuperAdmin)
  // ============================================================
  {
    path: 'platform',
    component: PlatformLayoutComponent,  // ✅ Layout con sidebar y header
    canActivate: [platformAuthGuard],
    children: [
      // ✅ Las páginas se cargan como componentes (ya que el layout ya está cargado)
      { path: 'dashboard', loadComponent: () => import('./pages/platform-dashboard-page/platform-dashboard-page.component').then(m => m.PlatformDashboardPageComponent) },
      {
        path: 'profile',
        loadComponent: () => import('./pages/platform-profile-page/platform-profile-page.component').then(m => m.PlatformProfilePageComponent)
      },
      {
        path: 'security',
        loadComponent: () => import('./pages/platform-security-page/platform-security-page.component').then(m => m.PlatformSecurityPageComponent)
      },
      {
        path: 'settings',
        loadComponent: () => import('./pages/settings-page/settings-page.component').then(m => m.SettingsPageComponent)
      },
      {
        path: 'subscription',
        canActivate: [permissionGuard('billing.subscription.manage')],
        loadComponent: () => import('./pages/subscription-settings-page/subscription-settings-page.component').then(m => m.SubscriptionSettingsPageComponent)
      },
      {
        path: 'subscription/success',
        canActivate: [permissionGuard('billing.subscription.manage')],
        loadComponent: () => import('./pages/subscription-upgrade-success-page/subscription-upgrade-success-page.component').then(m => m.SubscriptionUpgradeSuccessPageComponent)
      },
      {
        path: 'subscription/cancel',
        canActivate: [permissionGuard('billing.subscription.manage')],
        loadComponent: () => import('./pages/subscription-upgrade-cancel-page/subscription-upgrade-cancel-page.component').then(m => m.SubscriptionUpgradeCancelPageComponent)
      },
      {
        path: 'backups',
        loadComponent: () => import('./pages/platform-backups-page/platform-backups-page.component').then(m => m.PlatformBackupsPageComponent)
      },
      {
        path: 'plans',
        loadComponent: () => import('./pages/platform-plans-page/platform-plans-page.component').then(m => m.PlatformPlansPageComponent)
      },
      {
        path: 'audit',
        loadComponent: () => import('./pages/platform-audit-page/platform-audit-page.component').then(m => m.PlatformAuditPageComponent)
      },
      { path: 'tenants', loadComponent: () => import('./pages/platform-tenants-page/platform-tenants-page.component').then(m => m.PlatformTenantsPageComponent) },
      { path: 'subscriptions', loadComponent: () => import('./pages/platform-subscriptions-page/platform-subscriptions-page.component').then(m => m.PlatformSubscriptionsPageComponent) },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      // app.routes.ts - agregar dentro del children de platform
    ]
  },

  // ============================================================
  // FALLBACK
  // ============================================================
  { path: '**', redirectTo: '' }
];
