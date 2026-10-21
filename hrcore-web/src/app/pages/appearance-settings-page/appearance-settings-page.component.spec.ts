import { importProvidersFrom } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { AlertTriangle, Calendar, Check, CheckCircle, Eye, LayoutDashboard, LucideAngularModule, Monitor, Pencil, RotateCcw, Save, Users, Wand2 } from 'lucide-angular';
import { of, throwError } from 'rxjs';
import { AppearanceSettings, AppearanceSettingsService } from '../../entities/company/api/appearance-settings.service';
import { TenantAppearanceStateService } from '../../entities/company/model/tenant-appearance-state.service';
import { TenantLanguageStateService } from '../../shared/lib/i18n/tenant-language-state.service';
import { AuthStorageService } from '../../shared/lib/storage/auth-storage.service';
import { AppearanceSettingsPageComponent } from './appearance-settings-page.component';

describe('AppearanceSettingsPageComponent language', () => {
  const saved: AppearanceSettings = {
    preset: 'original', primary: '#1268c4', secondary: '#13a8e8', sidebar: '#082d55', mode: 'light',
  };
  const response = (data: AppearanceSettings, message = '') => ({ success: true, message, data, timestamp: '' });
  let service: jasmine.SpyObj<AppearanceSettingsService>;
  let appearanceState: jasmine.SpyObj<TenantAppearanceStateService>;
  let language: TenantLanguageStateService;

  beforeEach(() => {
    service = jasmine.createSpyObj<AppearanceSettingsService>('AppearanceSettingsService', ['get', 'save']);
    service.get.and.returnValue(of(response({ ...saved })));
    appearanceState = jasmine.createSpyObj<TenantAppearanceStateService>('TenantAppearanceStateService', ['currentSessionIdentity', 'confirmSaved']);
    appearanceState.currentSessionIdentity.and.returnValue('login-a:fragolitos');
    TestBed.configureTestingModule({ imports: [AppearanceSettingsPageComponent], providers: [
      importProvidersFrom(LucideAngularModule.pick({ AlertTriangle, Calendar, Check, CheckCircle, Eye, LayoutDashboard, Monitor, Pencil, RotateCcw, Save, Users, Wand2 })),
      { provide: AppearanceSettingsService, useValue: service },
      { provide: TenantAppearanceStateService, useValue: appearanceState },
      { provide: AuthStorageService, useValue: { getSession: () => ({ scope: 'TENANT', loginId: 'login-a', tenantSlug: 'fragolitos' }) } },
    ] });
    language = TestBed.inject(TenantLanguageStateService);
    language.enterLayout();
    language.activate('login-a:fragolitos');
  });

  it('renders the presets, palette and preview in Spanish and English without an extra request', () => {
    const fixture = TestBed.createComponent(AppearanceSettingsPageComponent);
    fixture.detectChanges();
    const page = fixture.componentInstance;
    expect(fixture.nativeElement.textContent).toContain('Elige un diseño');
    expect(fixture.nativeElement.textContent).toContain('Corporativo');
    page.selectPreset(page.presets[1]);
    page.onHexInput('primary', '#123456');
    page.setMode('dark');
    const draft = { ...page.colors };
    expect(language.confirmSaved('login-a:fragolitos', 'en')).toBeTrue();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Choose a design');
    expect(fixture.nativeElement.textContent).toContain('Corporate');
    expect(fixture.nativeElement.textContent).toContain('Your team at a glance');
    expect(page.selectedPresetId).toBe('corporate');
    expect(page.colors).toEqual(draft);
    expect(page.mode).toBe('dark');
    expect(page.colorDrafts.primary).toBe('#123456');
    expect(service.get).toHaveBeenCalledTimes(1);
    expect(service.save).not.toHaveBeenCalled();
  });

  it('keeps the exact appearance payload after changing language and applies only a confirmed save', () => {
    const fixture = TestBed.createComponent(AppearanceSettingsPageComponent);
    fixture.detectChanges();
    const page = fixture.componentInstance;
    page.onHexInput('primary', '#2563eb');
    service.save.and.returnValues(throwError(() => new Error('Simulated failure')), of(response({ ...saved, primary: '#2563eb' })));
    page.save();
    expect(appearanceState.confirmSaved).not.toHaveBeenCalled();
    language.confirmSaved('login-a:fragolitos', 'en');
    page.save();
    expect(service.save).toHaveBeenCalledTimes(2);
    expect(service.save.calls.argsFor(0)[0]).toEqual(service.save.calls.argsFor(1)[0]);
    expect(service.save.calls.argsFor(1)[0]).toEqual({ ...saved, primary: '#2563eb' });
    expect(appearanceState.confirmSaved).toHaveBeenCalledOnceWith('login-a:fragolitos', { ...saved, primary: '#2563eb' });
    expect(page.message(page.saveMessage)).toBe('Appearance saved successfully.');
  });

  it('keeps cancel and restore as local changes without saving', () => {
    const fixture = TestBed.createComponent(AppearanceSettingsPageComponent);
    fixture.detectChanges();
    const page = fixture.componentInstance;
    page.onHexInput('primary', '#2563eb');
    language.confirmSaved('login-a:fragolitos', 'en');
    page.cancel();
    expect(page.colors.primary).toBe(saved.primary);
    page.selectPreset(page.presets[2]);
    page.restoreOriginal();
    expect(page.selectedPresetId).toBe('original');
    expect(page.colors.primary).toBe(saved.primary);
    expect(service.save).not.toHaveBeenCalled();
  });

  it('reloads a saved appearance after recreation and reads a different tenant independently', () => {
    let identity = 'login-a:fragolitos';
    const stored: Record<string, AppearanceSettings> = {
      'login-a:fragolitos': { ...saved },
      'login-b:other-tenant': { ...saved, primary: '#654321' },
    };
    appearanceState.currentSessionIdentity.and.callFake(() => identity);
    service.get.and.callFake(() => of(response({ ...stored[identity] })));
    service.save.and.callFake(value => {
      stored[identity] = { ...value };
      return of(response({ ...value }));
    });
    const first = TestBed.createComponent(AppearanceSettingsPageComponent);
    first.detectChanges();
    first.componentInstance.onHexInput('primary', '#2563eb');
    first.componentInstance.save();
    expect(stored[identity].primary).toBe('#2563eb');
    first.destroy();

    const reopened = TestBed.createComponent(AppearanceSettingsPageComponent);
    reopened.detectChanges();
    expect(reopened.componentInstance.colors.primary).toBe('#2563eb');
    expect(reopened.componentInstance.canSave).toBeFalse();
    reopened.destroy();

    identity = 'login-b:other-tenant';
    const otherTenant = TestBed.createComponent(AppearanceSettingsPageComponent);
    otherTenant.detectChanges();
    expect(otherTenant.componentInstance.colors.primary).toBe('#654321');
    expect(stored['login-a:fragolitos'].primary).toBe('#2563eb');
    expect(service.save).toHaveBeenCalledTimes(1);
  });

  it('translates local validation messages reactively without changing HEX validation', async () => {
    const fixture = TestBed.createComponent(AppearanceSettingsPageComponent);
    fixture.detectChanges();
    const page = fixture.componentInstance;
    page.onBaseColorInput('invalid');
    fixture.detectChanges();
    expect(page.canSave).toBeFalse();
    expect(fixture.nativeElement.textContent).toContain('Ingresa un código HEX válido');
    await page.onLogoSelected({ target: { files: [new File([], 'empty.png', { type: 'image/png' })], value: 'x' } } as unknown as Event);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Ingresa un código HEX válido');
    language.confirmSaved('login-a:fragolitos', 'en');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Enter a valid HEX code');
    expect(fixture.nativeElement.textContent).toContain('The logo must be between 1 byte and 2 MB.');
    expect(page.baseColorDraft).toBe('invalid');
    expect(service.save).not.toHaveBeenCalled();
  });
});
