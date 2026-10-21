import { TestBed } from '@angular/core/testing';
import { ElementRef, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthFacade } from '../auth/auth.facade';
import { PermissionService } from '../auth/permission.service';
import { AuthStorageService } from '../storage/auth-storage.service';
import { PlatformFacade } from '../../../features/platform/lib/platform.facade';
import { TenantNavigationService } from '../navigation/tenant-navigation.service';
import { TENANT_NAVIGATION } from '../navigation/tenant-navigation.config';
import { SidebarComponent } from '../../../widgets/layoutAdmin/sidebar/sidebar.component';
import { HeaderComponent } from '../../../widgets/layoutAdmin/header/header.component';
import { NotificationsService } from '../../../entities/notifications';
import { TenantLanguageStateService } from './tenant-language-state.service';
import { companyLanguages } from '../../../entities/company/model/company-language-translations';
import { ptBR } from './catalog/pt-BR';
import { fr } from './catalog/fr';
import { de } from './catalog/de';
import { it as italian } from './catalog/it';
import { zhHans } from './catalog/zh-Hans';
import { zhHant } from './catalog/zh-Hant';
import { ja } from './catalog/ja';
import { ko } from './catalog/ko';
import { ru } from './catalog/ru';
import { ar } from './catalog/ar';
import { hi } from './catalog/hi';
import { tr } from './catalog/tr';
import { id } from './catalog/id';

describe('TENANT language state and shell', () => {
  let loginId = 'login-a';
  let tenantSlug = 'tenant-a';
  let state: TenantLanguageStateService;
  const authStatus = signal<'ready' | 'idle'>('ready');
  const user = signal({ tenantSlug: 'tenant-a', firstName: 'Tenant', lastName: 'User', roles: [] });

  beforeEach(() => {
    loginId = 'login-a';
    tenantSlug = 'tenant-a';
    authStatus.set('ready');
    user.set({ tenantSlug: 'tenant-a', firstName: 'Tenant', lastName: 'User', roles: [] });
    TestBed.configureTestingModule({ providers: [
      { provide: AuthStorageService, useValue: { getSession: () => ({ scope: 'TENANT', loginId, tenantSlug }) } },
      { provide: AuthFacade, useValue: { status: authStatus, currentUser: user } },
      { provide: PermissionService, useValue: { isCurrentOwner: () => true, hasPermission: () => true } },
      { provide: PlatformFacade, useValue: { currentSuperadmin: signal(null) } },
      { provide: Router, useValue: { url: '/dashboard/home' } },
      { provide: NotificationsService, useValue: {} },
      { provide: ElementRef, useValue: { nativeElement: { contains: () => false } } },
    ] });
    state = TestBed.inject(TenantLanguageStateService);
    state.enterLayout();
    state.activate('login-a:tenant-a');
  });

  it('uses Spanish while loading or missing and loads persisted English', () => {
    const revision = state.getRevision();
    expect(state.language()).toBe('es');
    expect(state.confirmFetched('login-a:tenant-a', revision, 'en')).toBeTrue();
    expect(state.language()).toBe('en');
    expect(state.t('shell.dashboard')).toBe('Dashboard');
  });

  it('maps every newly prepared code to its catalog and clears it on tenant change', () => {
    const additional = [
      ['pt-BR', ptBR], ['fr', fr], ['de', de], ['it', italian],
      ['zh-Hans', zhHans], ['zh-Hant', zhHant], ['ja', ja], ['ko', ko],
      ['ru', ru], ['ar', ar], ['hi', hi], ['tr', tr], ['id', id],
    ] as const;
    expect(companyLanguages.map(option => option.code)).toEqual([
      'es', 'en', ...additional.map(([code]) => code),
    ]);
    for (const [code, catalog] of additional) {
      expect(state.confirmFetched('login-a:tenant-a', state.getRevision(), code)).toBeTrue();
      expect(state.t('nav.home')).toBe(catalog['nav.home']);
      expect(state.t('appearance.title')).toBe(catalog['appearance.title']);
    }
    tenantSlug = 'tenant-b';
    expect(state.language()).toBe('es');
    state.activate('login-a:tenant-b');
    expect(state.confirmedForCurrentSession()).toBeNull();
    expect(state.confirmFetched('login-a:tenant-a', state.getRevision(), 'ar')).toBeFalse();
  });

  it('publishes only confirmed saves and rejects stale GETs and other sessions', () => {
    const oldRevision = state.getRevision();
    state.confirmFetched('login-a:tenant-a', oldRevision, 'es');
    expect(state.confirmSaved('login-a:tenant-a', 'en')).toBeTrue();
    expect(state.confirmFetched('login-a:tenant-a', oldRevision, 'es')).toBeFalse();
    expect(state.language()).toBe('en');
    tenantSlug = 'tenant-b';
    expect(state.confirmSaved('login-a:tenant-a', 'es')).toBeFalse();
    expect(state.confirmedForCurrentSession()).toBeNull();
    state.activate('login-a:tenant-b');
    expect(state.language()).toBe('es');
    expect(state.confirmFetched('login-a:tenant-a', oldRevision, 'en')).toBeFalse();
  });

  it('changes only TENANT labels, preserving routes, ids, permissions and PLATFORM text', () => {
    const navigation = TestBed.inject(TenantNavigationService);
    const sidebar = TestBed.runInInjectionContext(() => new SidebarComponent());
    const header = TestBed.runInInjectionContext(() => new HeaderComponent());
    sidebar.scope = 'TENANT';
    header.scope = 'TENANT';
    const before = navigation.visibleItems();
    expect(before.map(item => item.id)).toEqual(TENANT_NAVIGATION.map(item => item.id));
    state.confirmFetched('login-a:tenant-a', state.getRevision(), 'en');
    const after = navigation.visibleItems();
    expect(after.map(item => [item.id, item.route, item.permission, item.ownerOnly]))
      .toEqual(before.map(item => [item.id, item.route, item.permission, item.ownerOnly]));
    expect(after.find(item => item.id === 'company-settings')?.label).toBe('Company');
    expect(sidebar.navigationGroups.find(group => group.id === 'organization')?.label).toBe('Organization');
    expect(header.tenantMenuOptions.map(item => item.label)).toEqual(['Dashboard', 'My profile', 'Notifications']);
    expect(header.getRole()).toBe('Organization owner');
    sidebar.scope = 'PLATFORM';
    header.scope = 'PLATFORM';
    expect(sidebar.groupDefinitions.PLATFORM[0].label).toBe('Plataforma');
    expect(header.platformMenuOptions[0].label).toBe('Mi perfil');
    expect(header.getRole()).toBe('Super Administrador');
  });
});
