import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { CompanySettings, CompanySettingsService, HrSettings } from '../../entities/company';
import { CompanySetupStateService } from '../../shared/lib/company/company-setup-state.service';
import { TenantLanguageStateService } from '../../shared/lib/i18n/tenant-language-state.service';
import { AuthStorageService } from '../../shared/lib/storage/auth-storage.service';
import { ToastService } from '../../shared/ui/toast/toast.service';
import { CompanySetupPageComponent } from './company-setup-page.component';

describe('CompanySetupPageComponent language and setup flow', () => {
  const company: CompanySettings = {
    companyName: 'Example Co', legalName: 'Example Legal', timezone: 'America/La_Paz',
    currency: 'BOB', contactEmail: 'contact@example.test', contactPhone: null,
    country: 'BO', address: 'Main address', taxIdentifier: null,
  };
  const hr: HrSettings = {
    employeeWebPunchEnabled: true, geolocationRequired: false, breakPunchRequired: false,
    dailyProcessingEnabled: true, adjustmentApprovalRequired: true,
    leaveSelfServiceEnabled: true, leaveSupervisorApprovalEnabled: true,
  };
  const response = <T>(data: T) => ({ success: true, message: '', data, timestamp: '' });
  let service: jasmine.SpyObj<CompanySettingsService>;
  let toast: jasmine.SpyObj<ToastService>;
  let setupState: jasmine.SpyObj<CompanySetupStateService>;
  let router: jasmine.SpyObj<Router>;
  let language: TenantLanguageStateService;

  beforeEach(() => {
    service = jasmine.createSpyObj<CompanySettingsService>('CompanySettingsService', [
      'getSettings', 'getHrSettings', 'getSetupStatus', 'updateSettings', 'updateHrSettings', 'completeSetup',
    ]);
    service.getSettings.and.returnValue(of(response({ ...company })));
    service.getHrSettings.and.returnValue(of(response({ ...hr })));
    service.getSetupStatus.and.returnValue(of(response({ configured: false, missingFields: [] })));
    service.updateSettings.and.returnValue(of(response({ ...company })));
    service.updateHrSettings.and.returnValue(of(response({ ...hr })));
    service.completeSetup.and.returnValue(of(response({ configured: true, missingFields: [] })));
    toast = jasmine.createSpyObj<ToastService>('ToastService', ['error', 'success']);
    setupState = jasmine.createSpyObj<CompanySetupStateService>('CompanySetupStateService', ['setConfigured']);
    router = jasmine.createSpyObj<Router>('Router', ['navigateByUrl']);
    TestBed.configureTestingModule({ imports: [CompanySetupPageComponent], providers: [
      { provide: CompanySettingsService, useValue: service },
      { provide: CompanySetupStateService, useValue: setupState },
      { provide: ToastService, useValue: toast },
      { provide: Router, useValue: router },
      { provide: AuthStorageService, useValue: { getSession: () => ({ scope: 'TENANT', loginId: 'login-a', tenantSlug: 'fragolitos' }) } },
    ] });
    language = TestBed.inject(TenantLanguageStateService);
    language.enterLayout();
    language.activate('login-a:fragolitos');
  });

  it('renders all four steps in Spanish and English without changing form values', () => {
    const fixture = TestBed.createComponent(CompanySetupPageComponent);
    fixture.detectChanges();
    const page = fixture.componentInstance;
    page.company.companyName = 'Edited company';
    page.hr.geolocationRequired = true;
    const expected = [
      ['Cuéntanos sobre tu empresa', 'Tell us about your company'],
      ['Define tu contexto regional', 'Set your regional context'],
      ['Configura la operación de RR. HH.', 'Set up HR operations'],
      ['Tu espacio está listo', 'Your space is ready'],
    ];
    for (let step = 1; step <= 4; step++) {
      page.currentStep = step;
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain(expected[step - 1][0]);
    }
    expect(language.confirmSaved('login-a:fragolitos', 'en')).toBeTrue();
    for (let step = 1; step <= 4; step++) {
      page.currentStep = step;
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain(expected[step - 1][1]);
    }
    expect(page.company.companyName).toBe('Edited company');
    expect(page.hr.geolocationRequired).toBeTrue();
    expect(service.updateSettings).not.toHaveBeenCalled();
  });

  it('translates partial-setup fields and validates without writing', () => {
    service.getSettings.and.returnValue(of(response({ ...company, companyName: '' })));
    service.getSetupStatus.and.returnValue(of(response({ configured: false, missingFields: ['company.name', 'company.timezone'] })));
    const fixture = TestBed.createComponent(CompanySetupPageComponent);
    fixture.detectChanges();
    const page = fixture.componentInstance;
    expect(fixture.nativeElement.textContent).toContain('La configuración aún está incompleta');
    expect(fixture.nativeElement.textContent).toContain('Nombre comercial');
    language.confirmSaved('login-a:fragolitos', 'en');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('The configuration is still incomplete');
    expect(fixture.nativeElement.textContent).toContain('Business name');
    page.next();
    expect(page.currentStep).toBe(1);
    expect(toast.error).toHaveBeenCalledWith('Enter the business name.');
    expect(service.updateSettings).not.toHaveBeenCalled();
  });

  it('keeps the same save payloads and call order after a language change', () => {
    const fixture = TestBed.createComponent(CompanySetupPageComponent);
    fixture.detectChanges();
    const page = fixture.componentInstance;
    const order: string[] = [];
    service.updateSettings.and.callFake(value => { order.push('company'); return of(response(value)); });
    service.updateHrSettings.and.callFake(value => { order.push('hr'); return of(response(value)); });
    service.completeSetup.and.callFake(value => { order.push('complete'); expect(value).toBe('America/La_Paz'); return of(response({ configured: true, missingFields: [] })); });
    page.currentStep = 4;
    page.next();
    const spanishCompany = service.updateSettings.calls.mostRecent().args[0];
    const spanishHr = service.updateHrSettings.calls.mostRecent().args[0];
    language.confirmSaved('login-a:fragolitos', 'en');
    page.next();
    expect(service.updateSettings.calls.mostRecent().args[0]).toEqual(spanishCompany);
    expect(service.updateHrSettings.calls.mostRecent().args[0]).toEqual(spanishHr);
    expect(service.completeSetup.calls.allArgs()).toEqual([['America/La_Paz'], ['America/La_Paz']]);
    expect(service.getSettings).toHaveBeenCalledTimes(1);
    expect(service.getHrSettings).toHaveBeenCalledTimes(1);
    expect(service.getSetupStatus).toHaveBeenCalledTimes(1);
    expect(order).toEqual(['company', 'hr', 'complete', 'company', 'hr', 'complete']);
    expect(service.updateSettings).toHaveBeenCalledWith(company);
    expect(service.updateHrSettings).toHaveBeenCalledWith(hr);
    expect(setupState.setConfigured).toHaveBeenCalledWith({ configured: true, missingFields: [] });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/dashboard/home');
    expect(toast.success).toHaveBeenCalledWith('Setup completed.');
  });

  it('retains regional and required-approval validation', () => {
    const fixture = TestBed.createComponent(CompanySetupPageComponent);
    fixture.detectChanges();
    const page = fixture.componentInstance;
    page.currentStep = 2;
    page.company.country = null;
    page.next();
    expect(page.currentStep).toBe(2);
    expect(toast.error).toHaveBeenCalledWith('Completa país, zona horaria y moneda.');
    page.currentStep = 3;
    page.hr.adjustmentApprovalRequired = false;
    page.next();
    expect(page.currentStep).toBe(3);
    expect(page.hr.adjustmentApprovalRequired).toBeTrue();
    expect(service.updateSettings).not.toHaveBeenCalled();
  });

  it('stops the save chain when the first write fails', () => {
    service.updateSettings.and.returnValue(throwError(() => new Error('Request failed')));
    const fixture = TestBed.createComponent(CompanySetupPageComponent);
    fixture.detectChanges();
    fixture.componentInstance.currentStep = 4;
    fixture.componentInstance.next();
    expect(service.updateHrSettings).not.toHaveBeenCalled();
    expect(service.completeSetup).not.toHaveBeenCalled();
    expect(setupState.setConfigured).not.toHaveBeenCalled();
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });
});
