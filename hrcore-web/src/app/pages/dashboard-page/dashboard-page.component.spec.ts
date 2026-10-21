import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { AppearanceSettingsService } from '../../entities/company/api/appearance-settings.service';
import { TenantLanguageResponse, TenantLanguageService } from '../../entities/company/api/tenant-language.service';
import { TenantAppearanceStateService } from '../../entities/company/model/tenant-appearance-state.service';
import { AuthFacade } from '../../shared/lib/auth/auth.facade';
import { TenantLanguageStateService } from '../../shared/lib/i18n/tenant-language-state.service';
import { AuthStorageService } from '../../shared/lib/storage/auth-storage.service';
import { DashboardPageComponent } from './dashboard-page.component';

describe('DashboardPageComponent language activation', () => {
  it('loads once per session, keeps appearance loading, and rejects a late old response', () => {
    const status = signal<'ready' | 'loading'>('ready');
    const user = signal({ tenantSlug: 'tenant-a' });
    let loginId = 'login-a';
    let tenantSlug = 'tenant-a';
    const pending = new Subject<{ success: boolean; message: string; data: TenantLanguageResponse; timestamp: string }>();
    const languageApi = jasmine.createSpyObj<TenantLanguageService>('TenantLanguageService', ['get']);
    languageApi.get.and.returnValues(pending, of({ success: true, message: '', data: { language: 'en' }, timestamp: '' }));
    const appearanceApi = jasmine.createSpyObj<AppearanceSettingsService>('AppearanceSettingsService', ['get']);
    appearanceApi.get.and.returnValue(of({ success: true, message: '', data: { preset: 'original', primary: '#1268c4', secondary: '#13a8e8', sidebar: '#082d55', mode: 'light' }, timestamp: '' }));
    const appearanceState = jasmine.createSpyObj<TenantAppearanceStateService>('TenantAppearanceStateService', ['enterLayout', 'activate', 'getRevision', 'confirmFetched', 'deactivate', 'confirmedForCurrentSession']);
    appearanceState.enterLayout.and.returnValue(1);
    appearanceState.getRevision.and.returnValue(1);
    TestBed.configureTestingModule({ providers: [
      { provide: AuthFacade, useValue: { status, currentUser: user } },
      { provide: AuthStorageService, useValue: { getSession: () => ({ scope: 'TENANT', loginId, tenantSlug }) } },
      { provide: TenantLanguageService, useValue: languageApi },
      { provide: AppearanceSettingsService, useValue: appearanceApi },
      { provide: TenantAppearanceStateService, useValue: appearanceState },
    ] });
    const component = TestBed.runInInjectionContext(() => new DashboardPageComponent());
    const languageState = TestBed.inject(TenantLanguageStateService);
    TestBed.tick();
    expect(languageApi.get).toHaveBeenCalledTimes(1);
    expect(appearanceApi.get).toHaveBeenCalledTimes(1);
    status.set('loading');
    TestBed.tick();
    expect(languageApi.get).toHaveBeenCalledTimes(1);
    loginId = 'login-b';
    status.set('ready');
    TestBed.tick();
    expect(languageApi.get).toHaveBeenCalledTimes(2);
    expect(languageState.language()).toBe('en');
    pending.next({ success: true, message: '', data: { language: 'es' }, timestamp: '' });
    expect(languageState.language()).toBe('en');
    component.ngOnDestroy();
    expect(languageState.language()).toBe('es');
    expect(appearanceState.deactivate).toHaveBeenCalledOnceWith(1);
  });
});
