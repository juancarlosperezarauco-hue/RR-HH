import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { HttpErrorResponse } from '@angular/common/http';
import { of, Subject, throwError } from 'rxjs';
import { CompanySettings, CompanySettingsService } from '../../entities/company';
import { TenantLanguageResponse, TenantLanguageService } from '../../entities/company/api/tenant-language.service';
import { TenantRegionalFormats, TenantRegionalFormatsService } from '../../entities/company/api/tenant-regional-formats.service';
import { RegionalCountrySettingsService } from '../../entities/company/api/regional-country-settings.service';
import { TenantLanguageStateService } from '../../shared/lib/i18n/tenant-language-state.service';
import { AuthStorageService } from '../../shared/lib/storage/auth-storage.service';
import { ToastService } from '../../shared/ui/toast/toast.service';
import { CompanySettingsPageComponent } from './company-settings-page.component';
import { RegionalCountrySettingsComponent } from './regional-country-settings.component';

describe('CompanySettingsPageComponent language', () => {
  let component: CompanySettingsPageComponent;
  let language: jasmine.SpyObj<TenantLanguageService>;
  let company: jasmine.SpyObj<CompanySettingsService>;
  let regional: jasmine.SpyObj<TenantRegionalFormatsService>;
  let countryRegional: jasmine.SpyObj<RegionalCountrySettingsService>;
  let state: TenantLanguageStateService;
  let sessionKey = 'session-1';
  let tenantSlug = 'tenant-a';

  beforeEach(() => {
    sessionKey = 'session-1';
    tenantSlug = 'tenant-a';
    language = jasmine.createSpyObj<TenantLanguageService>('TenantLanguageService', ['get', 'save', 'supported']);
    language.supported.and.returnValue(of(['es', 'en']));
    regional = jasmine.createSpyObj<TenantRegionalFormatsService>('TenantRegionalFormatsService', ['get', 'save']);
    regional.get.and.returnValue(of({ success: true, message: '', data: { dateFormat: 'DMY', numberFormat: 'DECIMAL_COMMA' }, timestamp: '' }));
    regional.save.and.returnValue(of({ success: true, message: '', data: { dateFormat: 'YMD', numberFormat: 'DECIMAL_POINT' }, timestamp: '' }));
    countryRegional = jasmine.createSpyObj<RegionalCountrySettingsService>('RegionalCountrySettingsService',
      ['holidays', 'saveHolidays', 'workday', 'saveWorkday', 'rules', 'saveRules']);
    countryRegional.holidays.and.returnValue(of({ holidays: [{ date: '2026-12-25', name: 'Feriado de prueba' }] }));
    countryRegional.workday.and.returnValue(of({ days: ['MONDAY'], start: '09:00', end: '18:00', breakStart: '13:00', breakEnd: '14:00', dailyMinutes: 480, weeklyMinutes: 480 }));
    countryRegional.rules.and.returnValue(of({ contractReminderDays: 30, shiftBufferMinutes: null, attendanceToleranceMinutes: null, absenceNoticeHours: null }));
    company = jasmine.createSpyObj<CompanySettingsService>('CompanySettingsService', ['getSettings', 'getHrSettings', 'getSetupStatus', 'updateSettings', 'updateHrSettings']);
    company.getSettings.and.returnValue(of({ success: true, message: '', timestamp: '', data: {
      companyName: 'Fragole', legalName: null, timezone: 'America/La_Paz', currency: 'BOB',
      contactEmail: null, contactPhone: null, country: 'BO', address: null, taxIdentifier: null,
    } }));
    company.getHrSettings.and.returnValue(of({ success: true, message: '', timestamp: '', data: {
      employeeWebPunchEnabled: true, geolocationRequired: false, breakPunchRequired: false,
      dailyProcessingEnabled: true, adjustmentApprovalRequired: true,
      leaveSelfServiceEnabled: true, leaveSupervisorApprovalEnabled: true,
    } }));
    company.getSetupStatus.and.returnValue(of({ success: true, message: '', timestamp: '', data: { configured: true, missingFields: [] } }));
    language.save.and.returnValue(of({ success: true, message: '', data: { language: 'en' }, timestamp: '' }));
    TestBed.configureTestingModule({ imports: [CompanySettingsPageComponent], providers: [
      { provide: TenantLanguageService, useValue: language },
      { provide: TenantRegionalFormatsService, useValue: regional },
      { provide: RegionalCountrySettingsService, useValue: countryRegional },
      { provide: CompanySettingsService, useValue: company },
      { provide: AuthStorageService, useValue: { getSession: () => ({ scope: 'TENANT', loginId: sessionKey, tenantSlug }) } },
      { provide: ToastService, useValue: { error: jasmine.createSpy(), success: jasmine.createSpy() } },
    ] });
    state = TestBed.inject(TenantLanguageStateService);
    state.enterLayout();
    state.activate('session-1:tenant-a');
    component = TestBed.runInInjectionContext(() => new CompanySettingsPageComponent());
  });

  function load(languageCode: 'es' | 'en'): void {
    state.confirmFetched('session-1:tenant-a', state.getRevision(), languageCode);
    TestBed.tick();
  }

  it('renders sections 05 and 06 once inside Empresa and loads country data independently', async () => {
    load('es');
    const fixture = TestBed.createComponent(CompanySettingsPageComponent);
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;
    expect(root.querySelectorAll('app-regional-country-settings').length).toBe(1);
    expect(root.textContent).toContain('Formatos regionales');
    expect(root.textContent).toContain('Configuración regional por país');
    expect(root.querySelectorAll('select[name="dateFormat"]').length).toBe(1);
    expect(root.querySelectorAll('select[name="numberFormat"]').length).toBe(1);
    expect(root.textContent).toContain('Día / Mes / Año (20/10/2026)');
    expect(root.textContent).toContain('Coma decimal (12.345,67)');
    expect(root.querySelectorAll('select[name="country"]').length).toBe(1);
    expect(root.querySelectorAll('select[name="regionalCountry"]').length).toBe(1);
    const child = root.querySelector('app-regional-country-settings')!;
    const countrySelect = child.querySelector<HTMLSelectElement>('select[name="regionalCountry"]')!;
    countrySelect.value = 'AR';
    countrySelect.dispatchEvent(new Event('change'));
    TestBed.tick();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(countryRegional.holidays).toHaveBeenCalledOnceWith('AR');
    expect(countryRegional.workday).toHaveBeenCalledOnceWith('AR');
    expect(countryRegional.rules).toHaveBeenCalledOnceWith('AR');
    expect(child.textContent).toContain('Feriado de prueba');
    expect(child.querySelectorAll('input[name="contractReminderDays"]').length).toBe(1);
    const regionalComponent = fixture.debugElement.query(By.directive(RegionalCountrySettingsComponent))
      .componentInstance as RegionalCountrySettingsComponent;
    expect(regionalComponent.rulesDraft.contractReminderDays).toBe(30);
    expect(child.querySelector<HTMLInputElement>('input[name="contractReminderDays"]')?.value).toBe('30');
    expect(fixture.componentInstance.company.country).toBe('BO');
    expect(company.updateSettings).not.toHaveBeenCalled();
    expect(countryRegional.saveHolidays).not.toHaveBeenCalled();
    expect(countryRegional.saveWorkday).not.toHaveBeenCalled();
    expect(countryRegional.saveRules).not.toHaveBeenCalled();
    for (const title of ['Identidad', 'Localización operativa', 'Políticas de trabajo', 'Idioma del sistema']) {
      expect(root.textContent).toContain(title);
    }
    expect(state.confirmSaved('session-1:tenant-a', 'en')).toBeTrue();
    fixture.detectChanges();
    expect(root.textContent).toContain('Regional formats');
    expect(root.textContent).toContain('Regional settings by country');
    expect(root.textContent).toContain('Company holidays');
    expect(root.textContent).toContain('Day / Month / Year (20/10/2026)');
    expect(root.textContent).toContain('Decimal comma (12.345,67)');
  });

  it('reloads saved formats after component recreation without leaking them to another tenant', () => {
    const stored: Record<string, TenantRegionalFormats> = {
      'tenant-a': { dateFormat: 'DMY', numberFormat: 'DECIMAL_COMMA' },
      'tenant-b': { dateFormat: 'MDY', numberFormat: 'DECIMAL_COMMA' },
    };
    regional.get.and.callFake(() => of({ success: true, message: '', timestamp: '', data: { ...stored[tenantSlug] } }));
    regional.save.and.callFake(value => {
      stored[tenantSlug] = { ...value };
      return of({ success: true, message: '', timestamp: '', data: { ...value } });
    });
    load('es');
    const first = TestBed.createComponent(CompanySettingsPageComponent);
    first.detectChanges();
    first.componentInstance.formatsDraft = { dateFormat: 'YMD', numberFormat: 'DECIMAL_POINT' };
    first.componentInstance.saveRegionalFormats();
    expect(stored['tenant-a']).toEqual({ dateFormat: 'YMD', numberFormat: 'DECIMAL_POINT' });
    first.destroy();

    const reopened = TestBed.createComponent(CompanySettingsPageComponent);
    reopened.detectChanges();
    expect(reopened.componentInstance.formatsConfirmed).toEqual(stored['tenant-a']);
    reopened.componentInstance.formatsDraft = { dateFormat: 'DMY', numberFormat: 'DECIMAL_COMMA' };
    reopened.componentInstance.cancelRegionalFormats();
    expect(reopened.componentInstance.formatsDraft).toEqual(stored['tenant-a']);

    sessionKey = 'session-2';
    tenantSlug = 'tenant-b';
    state.activate('session-2:tenant-b');
    TestBed.tick();
    expect(reopened.componentInstance.formatsConfirmed).toEqual(stored['tenant-b']);
    expect(stored['tenant-a']).toEqual({ dateFormat: 'YMD', numberFormat: 'DECIMAL_POINT' });
    expect(regional.save).toHaveBeenCalledTimes(1);
  });

  it('clears company data on session change and ignores a late GET from the old tenant', () => {
    load('es');
    const fixture = TestBed.createComponent(CompanySettingsPageComponent);
    fixture.detectChanges();
    expect(fixture.componentInstance.company.companyName).toBe('Fragole');
    const pending = new Subject<{ success: boolean; message: string; timestamp: string; data: CompanySettings }>();
    company.getSettings.and.returnValue(pending);
    sessionKey = 'session-2';
    tenantSlug = 'tenant-b';
    state.activate('session-2:tenant-b');
    TestBed.tick();
    expect(fixture.componentInstance.company.companyName).toBe('');
    expect(fixture.componentInstance.loading).toBeTrue();

    company.getSettings.and.returnValue(of({ success: true, message: '', timestamp: '', data: {
      companyName: 'New tenant', legalName: null, timezone: 'America/Lima', currency: 'PEN',
      contactEmail: null, contactPhone: null, country: 'PE', address: null, taxIdentifier: null,
    } }));
    sessionKey = 'session-3';
    tenantSlug = 'tenant-c';
    state.activate('session-3:tenant-c');
    TestBed.tick();
    expect(fixture.componentInstance.company.companyName).toBe('New tenant');
    pending.next({ success: true, message: '', timestamp: '', data: {
      companyName: 'Old tenant', legalName: null, timezone: 'America/La_Paz', currency: 'BOB',
      contactEmail: null, contactPhone: null, country: 'BO', address: null, taxIdentifier: null,
    } });
    pending.complete();
    expect(fixture.componentInstance.company.companyName).toBe('New tenant');
  });

  it('uses the dashboard GET and applies English only after the isolated PUT succeeds', () => {
    load('es');
    expect(language.get).not.toHaveBeenCalled();
    expect(component.t('title')).toBe('Empresa');
    component.languageDraft = 'en';
    component.company.companyName = 'Unsaved company name';
    expect(component.t('title')).toBe('Empresa');
    component.saveLanguage();
    TestBed.tick();
    expect(language.save).toHaveBeenCalledOnceWith('en');
    expect(company.updateSettings).not.toHaveBeenCalled();
    expect(company.updateHrSettings).not.toHaveBeenCalled();
    expect(component.company.companyName).toBe('Unsaved company name');
    expect(state.language()).toBe('en');
    expect(component.t('title')).toBe('Company');
  });

  it('keeps the confirmed language when PUT fails and cancels the draft', () => {
    load('es');
    language.save.and.returnValue(throwError(() => new Error('network')));
    component.languageDraft = 'en';
    component.saveLanguage();
    expect(state.language()).toBe('es');
    expect(component.languageDraft).toBe('en');
    component.cancelLanguage();
    expect(component.languageDraft).toBe('es');
  });

  it('ignores a PUT response from an earlier session', () => {
    load('es');
    const pending = new Subject<{ success: boolean; message: string; data: TenantLanguageResponse; timestamp: string }>();
    language.save.and.returnValue(pending);
    component.languageDraft = 'en';
    component.saveLanguage();
    sessionKey = 'session-2';
    pending.next({ success: true, message: '', data: { language: 'en' }, timestamp: '' });
    expect(state.confirmedForCurrentSession()).toBeNull();
    expect(component.t('title')).toBe('Empresa');
  });

  it('uses persisted English after reload and leaves an unsaved draft local', () => {
    load('en');
    expect(component.t('title')).toBe('Company');
    component.languageDraft = 'es';
    expect(component.t('title')).toBe('Company');
    expect(language.save).not.toHaveBeenCalled();
  });

  it('shows all language choices but never sends an unsupported code to the active API', () => {
    load('es');
    expect(component.languageOptions.length).toBe(15);
    expect(component.languageOptions.find(option => option.code === 'zh-Hant')?.name).toBe('繁體中文');
    expect(component.canSaveLanguage('es')).toBeTrue();
    expect(component.canSaveLanguage('en')).toBeTrue();
    expect(component.canSaveLanguage('pt-BR')).toBeFalse();
    component.languageDraft = 'pt-BR';
    component.saveLanguage();
    expect(language.save).not.toHaveBeenCalled();
    expect(state.language()).toBe('es');
  });

  it('unlocks a new language only when the current tenant API reports support', () => {
    load('es');
    expect(component.canSaveLanguage('pt-BR')).toBeFalse();
    language.supported.and.returnValue(of(['es', 'en', 'pt-BR']));
    sessionKey = 'session-2';
    state.activate('session-2:tenant-a');
    TestBed.tick();
    expect(component.canSaveLanguage('pt-BR')).toBeTrue();
    expect(component.canSaveLanguage('ja')).toBeFalse();
  });

  it('saves date and number formats independently and keeps company fields unchanged', () => {
    load('es');
    expect(component.formatsSupported).toBeTrue();
    expect(component.dateFormatPreview).toBe('20/10/2026');
    component.company.companyName = 'Unsaved company name';
    component.formatsDraft = { dateFormat: 'YMD', numberFormat: 'DECIMAL_POINT' };
    expect(component.numberFormatPreview).toBe('12,345.67');
    component.saveRegionalFormats();
    expect(regional.save).toHaveBeenCalledOnceWith({ dateFormat: 'YMD', numberFormat: 'DECIMAL_POINT' });
    expect(component.formatsConfirmed).toEqual({ dateFormat: 'YMD', numberFormat: 'DECIMAL_POINT' });
    expect(company.updateSettings).not.toHaveBeenCalled();
    expect(company.updateHrSettings).not.toHaveBeenCalled();
    expect(component.company.companyName).toBe('Unsaved company name');
  });

  it('keeps confirmed formats after a failed PUT and cancels the draft', () => {
    load('es');
    regional.save.and.returnValue(throwError(() => new Error('network')));
    component.formatsDraft = { dateFormat: 'MDY', numberFormat: 'DECIMAL_POINT' };
    component.saveRegionalFormats();
    expect(component.formatsConfirmed).toEqual({ dateFormat: 'DMY', numberFormat: 'DECIMAL_COMMA' });
    component.cancelRegionalFormats();
    expect(component.formatsDraft).toEqual(component.formatsConfirmed!);
  });

  it('does not enable formats when the active backend lacks the endpoint', () => {
    regional.get.and.returnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
    sessionKey = 'session-2';
    state.activate('session-2:tenant-a');
    TestBed.tick();
    expect(component.formatsSupported).toBeFalse();
    component.saveRegionalFormats();
    expect(regional.save).not.toHaveBeenCalled();
  });

  it('ignores a format PUT response from the previous tenant session', () => {
    load('es');
    const pending = new Subject<{ success: boolean; message: string; data: TenantRegionalFormats; timestamp: string }>();
    regional.save.and.returnValue(pending);
    component.formatsDraft = { dateFormat: 'YMD', numberFormat: 'DECIMAL_POINT' };
    component.saveRegionalFormats();
    sessionKey = 'session-2';
    state.activate('session-2:tenant-a');
    TestBed.tick();
    pending.next({ success: true, message: '', data: { dateFormat: 'YMD', numberFormat: 'DECIMAL_POINT' }, timestamp: '' });
    expect(component.formatsConfirmed).toEqual({ dateFormat: 'DMY', numberFormat: 'DECIMAL_COMMA' });
  });
});
