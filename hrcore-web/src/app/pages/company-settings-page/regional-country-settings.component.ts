import { CommonModule } from '@angular/common';
import { Component, effect, inject, Input } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import {
  RegionalCountrySettingsService, RegionalHoliday, RegionalHolidays, RegionalRules,
  RegionalWorkday, RegionalWorkdayView,
} from '../../entities/company/api/regional-country-settings.service';
import { TenantLanguageStateService } from '../../shared/lib/i18n/tenant-language-state.service';

type Section = 'holidays' | 'workday' | 'rules';
const copy = {
  es: {
    title: 'Configuración regional por país',
    help: 'Cada país conserva sus propios feriados, jornada y parámetros dentro de esta empresa. Estos valores son de referencia y no cambian automáticamente contratos, turnos, asistencia ni ausencias existentes.',
    country: 'País a configurar', selectCountry: 'Selecciona un país', loading: 'Cargando configuración regional…',
    invalidResponse: 'La respuesta regional no es válida.', loadError: 'No se pudo cargar la configuración de este país. No se habilita el guardado.',
    holidays: 'Feriados configurados por la empresa', holidaysHelp: 'No se cargan feriados oficiales automáticamente. Un feriado por fecha.',
    date: 'Fecha', name: 'Nombre', add: 'Añadir', update: 'Actualizar', noHolidays: 'No hay feriados configurados para este país.',
    edit: 'Editar', remove: 'Eliminar', saving: 'Guardando…', saveHolidays: 'Guardar feriados', cancel: 'Cancelar',
    workday: 'Jornada laboral de referencia', workdayHelp: 'Se usa como configuración regional de referencia. No modifica los turnos ni calendarios laborales existentes.',
    start: 'Inicio', end: 'Fin', breakStart: 'Inicio del descanso (opcional)', breakEnd: 'Fin del descanso (opcional)',
    hoursDay: 'horas por día', hoursWeek: 'horas por semana', invalidWorkday: 'Selecciona días y horarios coherentes. El descanso debe estar dentro de la jornada.',
    saveWorkday: 'Guardar jornada', rules: 'Parámetros regionales de referencia',
    rulesHelp: 'Estos parámetros no son obligaciones legales ni se aplican automáticamente en los módulos laborales. Déjalos vacíos si no corresponden.',
    contracts: 'Recordatorio antes del fin de contrato (días, 0–365)', shifts: 'Intervalo de referencia entre turnos (minutos, 0–1440)',
    attendance: 'Tolerancia de asistencia de referencia (minutos, 0–240)', absences: 'Anticipación sugerida del aviso de ausencia (horas, 0–720)',
    invalidRules: 'Revisa los límites y usa números enteros.', saveRules: 'Guardar parámetros',
    invalidHoliday: 'Indica una fecha y un nombre válido (máximo 120 caracteres).', duplicateHoliday: 'Ya existe un feriado en esa fecha.',
    saved: 'Cambios guardados para', saveError: 'No se guardaron los cambios. Puedes revisar el formulario e intentarlo de nuevo.',
  },
  en: {
    title: 'Regional settings by country',
    help: 'Each country has its own holidays, workday and parameters within this company. These values are for reference and do not automatically change existing contracts, shifts, attendance or absences.',
    country: 'Country to configure', selectCountry: 'Select a country', loading: 'Loading regional settings…',
    invalidResponse: 'The regional response is invalid.', loadError: 'Could not load this country’s settings. Saving is unavailable.',
    holidays: 'Company holidays', holidaysHelp: 'Official holidays are not loaded automatically. One holiday per date.',
    date: 'Date', name: 'Name', add: 'Add', update: 'Update', noHolidays: 'No holidays configured for this country.',
    edit: 'Edit', remove: 'Remove', saving: 'Saving…', saveHolidays: 'Save holidays', cancel: 'Cancel',
    workday: 'Reference workday', workdayHelp: 'This is a regional reference setting. It does not change existing shifts or work calendars.',
    start: 'Start', end: 'End', breakStart: 'Break start (optional)', breakEnd: 'Break end (optional)',
    hoursDay: 'hours per day', hoursWeek: 'hours per week', invalidWorkday: 'Select valid days and hours. The break must be within the workday.',
    saveWorkday: 'Save workday', rules: 'Reference regional parameters',
    rulesHelp: 'These parameters are not legal requirements and are not applied automatically in workforce modules. Leave them empty if they do not apply.',
    contracts: 'Reminder before contract end (days, 0–365)', shifts: 'Reference gap between shifts (minutes, 0–1440)',
    attendance: 'Reference attendance tolerance (minutes, 0–240)', absences: 'Suggested absence notice (hours, 0–720)',
    invalidRules: 'Check the limits and use whole numbers.', saveRules: 'Save parameters',
    invalidHoliday: 'Enter a valid date and name (up to 120 characters).', duplicateHoliday: 'A holiday already exists on that date.',
    saved: 'Changes saved for', saveError: 'Changes were not saved. Review the form and try again.',
  },
} as const;
const EMPTY_WORKDAY: RegionalWorkdayView = {
  days: [], start: null, end: null, breakStart: null, breakEnd: null, dailyMinutes: 0, weeklyMinutes: 0,
};
const EMPTY_RULES: RegionalRules = {
  contractReminderDays: null, shiftBufferMinutes: null,
  attendanceToleranceMinutes: null, absenceNoticeHours: null,
};

@Component({
  selector: 'app-regional-country-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './regional-country-settings.component.html',
})
export class RegionalCountrySettingsComponent {
  @Input() countries: { code: string; name: string }[] = [];
  private readonly api = inject(RegionalCountrySettingsService);
  private readonly languageState = inject(TenantLanguageStateService);
  private identity: string | null = null;
  private generation = 0;
  country = '';
  loading = false;
  loaded = false;
  loadError = '';
  saving: Section | null = null;
  message = '';
  error = '';
  holidaysConfirmed: RegionalHolidays = { holidays: [] };
  holidaysDraft: RegionalHolidays = { holidays: [] };
  workdayConfirmed: RegionalWorkdayView = { ...EMPTY_WORKDAY };
  workdayDraft: RegionalWorkday = { ...EMPTY_WORKDAY, days: [] };
  rulesConfirmed: RegionalRules = { ...EMPTY_RULES };
  rulesDraft: RegionalRules = { ...EMPTY_RULES };
  holidayDate = '';
  holidayName = '';
  holidayIndex: number | null = null;
  readonly weekdays = [
    { code: 'MONDAY', name: 'Lunes' }, { code: 'TUESDAY', name: 'Martes' },
    { code: 'WEDNESDAY', name: 'Miércoles' }, { code: 'THURSDAY', name: 'Jueves' },
    { code: 'FRIDAY', name: 'Viernes' }, { code: 'SATURDAY', name: 'Sábado' },
    { code: 'SUNDAY', name: 'Domingo' },
  ];
  t(key: keyof typeof copy.es): string { return copy[this.languageState.language() === 'en' ? 'en' : 'es'][key]; }
  weekdayName(code: string): string {
    if (this.languageState.language() !== 'en') return this.weekdays.find(day => day.code === code)?.name ?? code;
    return ({ MONDAY: 'Monday', TUESDAY: 'Tuesday', WEDNESDAY: 'Wednesday', THURSDAY: 'Thursday',
      FRIDAY: 'Friday', SATURDAY: 'Saturday', SUNDAY: 'Sunday' } as Record<string, string>)[code] ?? code;
  }

  constructor() {
    effect(() => {
      this.languageState.sessionRevision();
      const identity = this.languageState.currentSessionIdentity();
      if (identity === this.identity) return;
      this.identity = identity;
      this.generation++;
      this.country = '';
      this.clear();
    });
  }

  countryName(code: string, fallback: string): string {
    return new Intl.DisplayNames([this.languageState.language()], { type: 'region' }).of(code) ?? fallback;
  }

  selectCountry(country: string): void {
    this.country = country;
    this.generation++;
    this.clear();
    const identity = this.identity;
    const generation = this.generation;
    if (!country || !identity) return;
    this.loading = true;
    forkJoin({
      holidays: this.api.holidays(country),
      workday: this.api.workday(country),
      rules: this.api.rules(country),
    }).subscribe({
      next: data => {
        if (!this.current(identity, country, generation)) return;
        if (!Array.isArray(data.holidays?.holidays) || !Array.isArray(data.workday?.days) || !data.rules) {
          this.loading = false;
          this.loadError = this.t('invalidResponse');
          return;
        }
        this.holidaysConfirmed = { holidays: data.holidays.holidays.map(item => ({ ...item })) };
        this.workdayConfirmed = { ...data.workday, days: [...data.workday.days] };
        this.rulesConfirmed = { ...data.rules };
        this.cancelHolidays();
        this.cancelWorkday();
        this.cancelRules();
        this.loading = false;
        this.loaded = true;
      },
      error: () => {
        if (!this.current(identity, country, generation)) return;
        this.loading = false;
        this.loadError = this.t('loadError');
      },
    });
  }

  private current(identity: string | null, country: string, generation: number): boolean {
    return !!identity && this.identity === identity && this.languageState.currentSessionIdentity() === identity
      && this.country === country && this.generation === generation;
  }

  private clear(): void {
    this.loading = false;
    this.loaded = false;
    this.loadError = '';
    this.saving = null;
    this.message = '';
    this.error = '';
    this.holidaysConfirmed = { holidays: [] };
    this.holidaysDraft = { holidays: [] };
    this.workdayConfirmed = { ...EMPTY_WORKDAY };
    this.workdayDraft = { ...EMPTY_WORKDAY, days: [] };
    this.rulesConfirmed = { ...EMPTY_RULES };
    this.rulesDraft = { ...EMPTY_RULES };
    this.holidayDate = '';
    this.holidayName = '';
    this.holidayIndex = null;
  }

  editHoliday(index: number): void {
    const holiday = this.holidaysDraft.holidays[index];
    if (!holiday) return;
    this.holidayIndex = index;
    this.holidayDate = holiday.date;
    this.holidayName = holiday.name;
  }

  stageHoliday(): void {
    const date = this.holidayDate;
    const name = this.holidayName.trim();
    const parsed = new Date(`${date}T00:00:00Z`);
    if (!/^\d{4}-\d{2}-\d{2}$/.test(date) || !name || name.length > 120
        || Number.isNaN(parsed.getTime()) || parsed.toISOString().slice(0, 10) !== date) {
      this.error = this.t('invalidHoliday');
      return;
    }
    if (this.holidaysDraft.holidays.some((item, index) => index !== this.holidayIndex && item.date === date)) {
      this.error = this.t('duplicateHoliday');
      return;
    }
    const draft = this.holidaysDraft.holidays.map(item => ({ ...item }));
    const entry: RegionalHoliday = { date, name };
    if (this.holidayIndex === null) draft.push(entry);
    else draft[this.holidayIndex] = entry;
    draft.sort((a, b) => a.date.localeCompare(b.date));
    this.holidaysDraft = { holidays: draft };
    this.holidayIndex = null;
    this.holidayDate = '';
    this.holidayName = '';
    this.error = '';
    this.message = '';
  }

  removeHoliday(index: number): void {
    this.holidaysDraft = { holidays: this.holidaysDraft.holidays.filter((_, item) => item !== index) };
    this.holidayIndex = null;
    this.message = '';
  }

  cancelHolidays(): void {
    this.holidaysDraft = { holidays: this.holidaysConfirmed.holidays.map(item => ({ ...item })) };
    this.holidayIndex = null;
    this.holidayDate = '';
    this.holidayName = '';
    this.error = '';
  }

  get holidaysDirty(): boolean {
    return JSON.stringify(this.holidaysDraft) !== JSON.stringify(this.holidaysConfirmed);
  }

  saveHolidays(): void {
    if (!this.canSave()) return;
    const draft = { holidays: this.holidaysDraft.holidays.map(item => ({ ...item })) };
    this.commit('holidays', this.api.saveHolidays(this.country, draft), result => {
      this.holidaysConfirmed = { holidays: result.holidays.map(item => ({ ...item })) };
      this.cancelHolidays();
    });
  }

  toggleDay(day: string): void {
    const days = this.workdayDraft.days.includes(day)
      ? this.workdayDraft.days.filter(value => value !== day) : [...this.workdayDraft.days, day];
    this.workdayDraft = { ...this.workdayDraft, days };
    this.message = '';
  }

  private minutes(value: string | null): number | null {
    if (!value || !/^([01]\d|2[0-3]):[0-5]\d$/.test(value)) return null;
    return Number(value.slice(0, 2)) * 60 + Number(value.slice(3, 5));
  }

  get workdayDailyMinutes(): number | null {
    const start = this.minutes(this.workdayDraft.start);
    const end = this.minutes(this.workdayDraft.end);
    if (!this.workdayDraft.days.length || start === null || end === null || end <= start) return null;
    const breakStart = this.workdayDraft.breakStart ? this.minutes(this.workdayDraft.breakStart) : null;
    const breakEnd = this.workdayDraft.breakEnd ? this.minutes(this.workdayDraft.breakEnd) : null;
    if ((breakStart === null) !== (breakEnd === null)) return null;
    if (breakStart !== null && breakEnd !== null) {
      if (breakStart <= start || breakEnd >= end || breakEnd <= breakStart) return null;
      return end - start - (breakEnd - breakStart);
    }
    return end - start;
  }

  get workdayDirty(): boolean {
    const saved = this.workdayConfirmed;
    const draft = this.workdayDraft;
    return JSON.stringify([...draft.days].sort()) !== JSON.stringify([...saved.days].sort())
      || draft.start !== saved.start || draft.end !== saved.end
      || draft.breakStart !== saved.breakStart || draft.breakEnd !== saved.breakEnd;
  }

  cancelWorkday(): void {
    const saved = this.workdayConfirmed;
    this.workdayDraft = { days: [...saved.days], start: saved.start, end: saved.end,
      breakStart: saved.breakStart, breakEnd: saved.breakEnd };
    this.error = '';
  }

  saveWorkday(): void {
    if (!this.canSave() || this.workdayDailyMinutes === null) return;
    const value: RegionalWorkday = {
      ...this.workdayDraft,
      days: [...this.workdayDraft.days],
      breakStart: this.workdayDraft.breakStart || null,
      breakEnd: this.workdayDraft.breakEnd || null,
    };
    this.commit('workday', this.api.saveWorkday(this.country, value), result => {
      this.workdayConfirmed = { ...result, days: [...result.days] };
      this.cancelWorkday();
    });
  }

  get rulesDirty(): boolean { return JSON.stringify(this.rulesDraft) !== JSON.stringify(this.rulesConfirmed); }
  get rulesValid(): boolean {
    return ([
      [this.rulesDraft.contractReminderDays, 365], [this.rulesDraft.shiftBufferMinutes, 1440],
      [this.rulesDraft.attendanceToleranceMinutes, 240], [this.rulesDraft.absenceNoticeHours, 720],
    ] as [number | null, number][]).every(([value, max]) => value === null
      || (Number.isInteger(value) && value >= 0 && value <= max));
  }

  cancelRules(): void { this.rulesDraft = { ...this.rulesConfirmed }; this.error = ''; }

  saveRules(): void {
    if (!this.canSave() || !this.rulesValid) return;
    this.commit('rules', this.api.saveRules(this.country, { ...this.rulesDraft }), result => {
      this.rulesConfirmed = { ...result };
      this.cancelRules();
    });
  }

  private canSave(): boolean { return !!this.identity && this.loaded && !this.loading && !this.saving; }

  private commit<T>(section: Section, request: import('rxjs').Observable<T>, accept: (value: T) => void): void {
    const identity = this.identity;
    const country = this.country;
    const generation = this.generation;
    this.saving = section;
    this.message = '';
    this.error = '';
    request.subscribe({
      next: value => {
        if (!this.current(identity, country, generation)) return;
        this.saving = null;
        accept(value);
        this.message = this.t('saved') + ' ' + this.countryName(country, country) + '.';
      },
      error: () => {
        if (!this.current(identity, country, generation)) return;
        this.saving = null;
        this.error = this.t('saveError');
      },
    });
  }
}
