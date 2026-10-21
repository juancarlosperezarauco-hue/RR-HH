import { TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import {
  RegionalCountrySettingsService, RegionalHolidays, RegionalRules, RegionalWorkdayView,
} from '../../entities/company/api/regional-country-settings.service';
import { TenantLanguageStateService } from '../../shared/lib/i18n/tenant-language-state.service';
import { AuthStorageService } from '../../shared/lib/storage/auth-storage.service';
import { RegionalCountrySettingsComponent } from './regional-country-settings.component';

describe('RegionalCountrySettingsComponent', () => {
  let api: jasmine.SpyObj<RegionalCountrySettingsService>;
  let state: TenantLanguageStateService;
  let component: RegionalCountrySettingsComponent;
  let session = 'session-a';
  const emptyWorkday: RegionalWorkdayView = {
    days: [], start: null, end: null, breakStart: null, breakEnd: null, dailyMinutes: 0, weeklyMinutes: 0,
  };
  const emptyRules: RegionalRules = {
    contractReminderDays: null, shiftBufferMinutes: null,
    attendanceToleranceMinutes: null, absenceNoticeHours: null,
  };

  beforeEach(() => {
    session = 'session-a';
    api = jasmine.createSpyObj<RegionalCountrySettingsService>('RegionalCountrySettingsService',
      ['holidays', 'saveHolidays', 'workday', 'saveWorkday', 'rules', 'saveRules']);
    api.holidays.and.returnValue(of({ holidays: [] }));
    api.workday.and.returnValue(of(emptyWorkday));
    api.rules.and.returnValue(of(emptyRules));
    api.saveHolidays.and.callFake((_, value) => of(value));
    api.saveWorkday.and.callFake((_, value) => of({ ...value, dailyMinutes: 480, weeklyMinutes: 2400 }));
    api.saveRules.and.callFake((_, value) => of(value));
    TestBed.configureTestingModule({ providers: [
      { provide: RegionalCountrySettingsService, useValue: api },
      { provide: AuthStorageService, useValue: { getSession: () => ({ scope: 'TENANT', loginId: session, tenantSlug: 'tenant-a' }) } },
    ] });
    state = TestBed.inject(TenantLanguageStateService);
    state.enterLayout();
    state.activate('session-a:tenant-a');
    component = TestBed.runInInjectionContext(() => new RegionalCountrySettingsComponent());
    component.countries = [{ code: 'BO', name: 'Bolivia' }, { code: 'AR', name: 'Argentina' }];
    TestBed.tick();
  });

  it('stages, edits and deletes holidays without a PUT until Save', () => {
    component.selectCountry('BO');
    component.holidayDate = '2026-12-25';
    component.holidayName = 'Descanso';
    component.stageHoliday();
    expect(api.saveHolidays).not.toHaveBeenCalled();
    component.editHoliday(0);
    component.holidayName = 'Actualizado';
    component.stageHoliday();
    expect(component.holidaysDraft.holidays[0].name).toBe('Actualizado');
    component.saveHolidays();
    expect(api.saveHolidays).toHaveBeenCalledOnceWith('BO', { holidays: [{ date: '2026-12-25', name: 'Actualizado' }] });
    component.removeHoliday(0);
    expect(api.saveHolidays).toHaveBeenCalledTimes(1);
    component.cancelHolidays();
    expect(component.holidaysDraft.holidays[0].name).toBe('Actualizado');
  });

  it('rejects an impossible date and a duplicate holiday before saving', () => {
    component.selectCountry('BO');
    component.holidayDate = '2026-02-30';
    component.holidayName = 'Inválido';
    component.stageHoliday();
    expect(component.holidaysDraft.holidays).toEqual([]);
    component.holidayDate = '2026-02-28';
    component.stageHoliday();
    component.holidayName = 'Duplicado';
    component.stageHoliday();
    expect(component.holidaysDraft.holidays.length).toBe(1);
    expect(api.saveHolidays).not.toHaveBeenCalled();
  });

  it('does not mix country data when an old GET completes later', () => {
    const delayed = new Subject<RegionalHolidays>();
    api.holidays.and.callFake(country => country === 'BO' ? delayed : of({ holidays: [
      { date: '2026-02-01', name: 'AR' },
    ] }));
    component.selectCountry('BO');
    component.selectCountry('AR');
    expect(component.holidaysDraft.holidays[0].name).toBe('AR');
    delayed.next({ holidays: [{ date: '2026-01-01', name: 'BO' }] });
    delayed.complete();
    expect(component.country).toBe('AR');
    expect(component.holidaysDraft.holidays[0].name).toBe('AR');
  });

  it('reloads saved settings independently for each country and after recreation', () => {
    const stored: Record<string, RegionalHolidays> = {
      BO: { holidays: [] }, AR: { holidays: [] },
    };
    api.holidays.and.callFake(country => of({ holidays: stored[country].holidays.map(item => ({ ...item })) }));
    api.saveHolidays.and.callFake((country, value) => {
      stored[country] = { holidays: value.holidays.map(item => ({ ...item })) };
      return of(value);
    });
    component.selectCountry('BO');
    component.holidayDate = '2026-12-25';
    component.holidayName = 'Bolivia';
    component.stageHoliday();
    component.saveHolidays();
    component.selectCountry('AR');
    expect(component.holidaysDraft.holidays).toEqual([]);
    component.holidayDate = '2026-05-25';
    component.holidayName = 'Argentina';
    component.stageHoliday();
    component.saveHolidays();
    component.selectCountry('BO');
    expect(component.holidaysDraft.holidays).toEqual([{ date: '2026-12-25', name: 'Bolivia' }]);
    const reopened = TestBed.runInInjectionContext(() => new RegionalCountrySettingsComponent());
    TestBed.tick();
    reopened.selectCountry('AR');
    expect(reopened.holidaysDraft.holidays).toEqual([{ date: '2026-05-25', name: 'Argentina' }]);
    expect(api.saveHolidays).toHaveBeenCalledTimes(2);
  });

  it('discards a late GET from a previous session', () => {
    const delayed = new Subject<RegionalHolidays>();
    api.holidays.and.returnValue(delayed);
    component.selectCountry('BO');
    session = 'session-b';
    state.activate('session-b:tenant-a');
    TestBed.tick();
    delayed.next({ holidays: [{ date: '2026-01-01', name: 'Old session' }] });
    delayed.complete();
    expect(component.country).toBe('');
    expect(component.holidaysDraft.holidays).toEqual([]);
  });

  it('rejects an old session PUT response and clears regional state', () => {
    const pending = new Subject<RegionalRules>();
    api.saveRules.and.returnValue(pending);
    component.selectCountry('BO');
    component.rulesDraft.contractReminderDays = 30;
    component.saveRules();
    session = 'session-b';
    state.activate('session-b:tenant-a');
    TestBed.tick();
    pending.next({ ...emptyRules, contractReminderDays: 30 });
    expect(component.country).toBe('');
    expect(component.rulesConfirmed.contractReminderDays).toBeNull();
  });

  it('keeps confirmed data after PUT failure and can cancel the draft', () => {
    api.saveRules.and.returnValue(throwError(() => new Error('offline')));
    component.selectCountry('BO');
    component.rulesDraft.absenceNoticeHours = 24;
    component.saveRules();
    expect(component.rulesConfirmed.absenceNoticeHours).toBeNull();
    component.cancelRules();
    expect(component.rulesDraft.absenceNoticeHours).toBeNull();
  });

  it('validates workday and saves it separately from other sections', () => {
    component.selectCountry('BO');
    component.toggleDay('MONDAY');
    component.workdayDraft.start = '09:00';
    component.workdayDraft.end = '18:00';
    component.workdayDraft.breakStart = '13:00';
    component.workdayDraft.breakEnd = '14:00';
    expect(component.workdayDailyMinutes).toBe(480);
    component.saveWorkday();
    expect(api.saveWorkday).toHaveBeenCalledOnceWith('BO', jasmine.objectContaining({ days: ['MONDAY'] }));
    expect(api.saveHolidays).not.toHaveBeenCalled();
    expect(api.saveRules).not.toHaveBeenCalled();
  });
});
