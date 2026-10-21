import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, effect, inject, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize, forkJoin, Subscription, switchMap } from 'rxjs';
import { CompanySettings, CompanySettingsService, HrSettings } from '../../entities/company';
import { TenantLanguageService } from '../../entities/company/api/tenant-language.service';
import { TenantDateFormat, TenantNumberFormat, TenantRegionalFormats, TenantRegionalFormatsService } from '../../entities/company/api/tenant-regional-formats.service';
import { CompanyLanguage, CompanySettingsTextKey, companyLanguages, companySettingsText, isCompanyLanguage } from '../../entities/company/model/company-language-translations';
import { TenantLanguageStateService } from '../../shared/lib/i18n/tenant-language-state.service';
import { ToastService } from '../../shared/ui/toast/toast.service';
import { RegionalCountrySettingsComponent } from './regional-country-settings.component';

@Component({ selector: 'app-company-settings-page', standalone: true, imports: [CommonModule, FormsModule, RegionalCountrySettingsComponent], templateUrl: './company-settings-page.component.html', styles: [`
  .section-mark{display:flex;height:2rem;width:2rem;flex:none;align-items:center;justify-content:center;border-radius:.7rem;background:color-mix(in srgb,var(--primary) 12%,transparent);color:var(--primary);font-size:.7rem;font-weight:900}.field-label{display:block;color:var(--foreground);font-size:.78rem;font-weight:700;letter-spacing:.01em}.field-label>span{color:var(--primary)}.field{display:block;width:100%;margin-top:.45rem;border:1px solid var(--input);border-radius:.9rem;background:var(--card);padding:.78rem 1rem;color:var(--foreground);font:500 .9rem/1.4 inherit;outline:none;transition:border-color .2s,box-shadow .2s,background .2s}.field::placeholder{color:var(--muted-foreground);font-weight:400;opacity:.8}.field:focus{border-color:var(--primary);background:var(--background);box-shadow:0 0 0 3px color-mix(in srgb,var(--primary) 12%,transparent)}.policy{display:flex;align-items:flex-start;gap:.75rem;border:1px solid var(--border);border-radius:1rem;background:var(--card);padding:1rem;cursor:pointer;transition:border-color .2s,background .2s,box-shadow .2s}.policy:hover{border-color:color-mix(in srgb,var(--primary) 55%,transparent);background:color-mix(in srgb,var(--muted) 50%,transparent)}.policy:has(input:checked){border-color:color-mix(in srgb,var(--primary) 55%,transparent);background:color-mix(in srgb,var(--primary) 6%,transparent);box-shadow:0 0 0 1px color-mix(in srgb,var(--primary) 12%,transparent)}.policy input{margin-top:.2rem;height:1rem;width:1rem;accent-color:var(--primary)}.policy strong{display:block;font-size:.86rem}.policy small{display:block;margin-top:.25rem;color:var(--muted-foreground);font-size:.75rem;line-height:1.35;font-weight:500}
`] })
export class CompanySettingsPageComponent implements OnInit, OnDestroy {
  private readonly service = inject(CompanySettingsService); private readonly toast = inject(ToastService); saving = false; loading = true; loadError = false; missingFields: string[] = [];
  private companyIdentity: string | null = null;
  private companyLoad: Subscription | null = null;
  private readonly languageService = inject(TenantLanguageService);
  private readonly languageState = inject(TenantLanguageStateService);
  private readonly regionalFormatsService = inject(TenantRegionalFormatsService);
  private languageIdentity: string | null = null;
  languageConfirmed: CompanyLanguage | null = null;
  languageDraft: CompanyLanguage = 'es';
  languageLoading = true;
  languageLoadError = false;
  languageSaving = false;
  languageError = '';
  languageMessage = '';
  readonly languageOptions = companyLanguages;
  private supportIdentity: string | null = null;
  private supportedLanguageCodes: ReadonlySet<CompanyLanguage> = new Set(['es', 'en']);
  private formatsIdentity: string | null = null;
  formatsConfirmed: TenantRegionalFormats | null = null;
  formatsDraft: TenantRegionalFormats = { dateFormat: 'DMY', numberFormat: 'DECIMAL_COMMA' };
  formatsLoading = false;
  formatsSupported = false;
  formatsLoadError = false;
  formatsSaving = false;
  formatsError = '';
  formatsMessage = '';
  readonly dateFormats: { code: TenantDateFormat; labelKey: CompanySettingsTextKey; example: string }[] = [
    { code: 'DMY', labelKey: 'dateFormatDmy', example: '20/10/2026' },
    { code: 'MDY', labelKey: 'dateFormatMdy', example: '10/20/2026' },
    { code: 'YMD', labelKey: 'dateFormatYmd', example: '2026-10-20' },
  ];
  readonly numberFormats: { code: TenantNumberFormat; labelKey: CompanySettingsTextKey; example: string }[] = [
    { code: 'DECIMAL_COMMA', labelKey: 'numberFormatComma', example: '12.345,67' },
    { code: 'DECIMAL_POINT', labelKey: 'numberFormatPoint', example: '12,345.67' },
  ];
  readonly countries = [{ code: 'BO', name: 'Bolivia', currency: 'BOB', timezones: ['America/La_Paz'] }, { code: 'AR', name: 'Argentina', currency: 'ARS', timezones: ['America/Argentina/Buenos_Aires', 'America/Argentina/Cordoba'] }, { code: 'BR', name: 'Brasil', currency: 'BRL', timezones: ['America/Sao_Paulo', 'America/Manaus'] }, { code: 'CL', name: 'Chile', currency: 'CLP', timezones: ['America/Santiago', 'Pacific/Easter'] }, { code: 'CO', name: 'Colombia', currency: 'COP', timezones: ['America/Bogota'] }, { code: 'EC', name: 'Ecuador', currency: 'USD', timezones: ['America/Guayaquil', 'Pacific/Galapagos'] }, { code: 'MX', name: 'México', currency: 'MXN', timezones: ['America/Mexico_City', 'America/Tijuana'] }, { code: 'PE', name: 'Perú', currency: 'PEN', timezones: ['America/Lima'] }, { code: 'PY', name: 'Paraguay', currency: 'PYG', timezones: ['America/Asuncion'] }, { code: 'UY', name: 'Uruguay', currency: 'UYU', timezones: ['America/Montevideo'] }, { code: 'US', name: 'Estados Unidos', currency: 'USD', timezones: ['America/New_York', 'America/Chicago', 'America/Denver', 'America/Los_Angeles'] }, { code: 'ES', name: 'España', currency: 'EUR', timezones: ['Europe/Madrid', 'Atlantic/Canary'] }];
  readonly currencies = ['BOB', 'ARS', 'BRL', 'CLP', 'COP', 'EUR', 'MXN', 'PEN', 'PYG', 'USD', 'UYU'];
  company: CompanySettings = { companyName: '', legalName: null, timezone: '', currency: '', contactEmail: null, contactPhone: null, country: null, address: null, taxIdentifier: null };
  hr: HrSettings = { employeeWebPunchEnabled: true, geolocationRequired: false, breakPunchRequired: false, dailyProcessingEnabled: true, adjustmentApprovalRequired: true, leaveSelfServiceEnabled: true, leaveSupervisorApprovalEnabled: true };
  constructor() {
    effect(() => {
      this.languageState.sessionRevision();
      const status = this.languageState.status();
      const confirmed = this.languageState.confirmedForCurrentSession();
      const identity = this.sessionIdentity();
      if (identity !== this.companyIdentity) this.loadCompanySettings(identity);
      if (identity !== this.supportIdentity) {
        this.supportIdentity = identity;
        this.supportedLanguageCodes = new Set(['es', 'en']);
        if (identity) this.loadSupportedLanguages(identity);
      }
      if (identity !== this.formatsIdentity) {
        this.formatsIdentity = identity;
        this.formatsConfirmed = null;
        this.formatsDraft = { dateFormat: 'DMY', numberFormat: 'DECIMAL_COMMA' };
        this.formatsSupported = false;
        this.formatsLoading = false;
        this.formatsLoadError = false;
        this.formatsSaving = false;
        this.formatsError = '';
        this.formatsMessage = '';
        if (identity) this.loadRegionalFormats(identity);
      }
      if (identity !== this.languageIdentity) {
        this.languageIdentity = null;
        this.languageConfirmed = null;
        this.languageDraft = 'es';
        this.languageMessage = '';
        this.languageError = '';
      }
      this.languageLoading = status === 'idle' || status === 'loading';
      this.languageLoadError = status === 'error';
      if (status === 'ready' && identity && confirmed) {
        if (this.languageConfirmed !== confirmed || this.languageIdentity !== identity) {
          this.languageDraft = confirmed;
        }
        this.languageIdentity = identity;
        this.languageConfirmed = confirmed;
        this.languageLoading = false;
        this.languageLoadError = false;
      }
    });
  }
  ngOnInit() {
    const identity = this.sessionIdentity();
    if (identity && identity !== this.companyIdentity) this.loadCompanySettings(identity);
  }
  ngOnDestroy(): void { this.companyLoad?.unsubscribe(); }
  private loadCompanySettings(identity: string | null): void {
    this.companyLoad?.unsubscribe();
    this.companyIdentity = identity;
    this.company = { companyName: '', legalName: null, timezone: '', currency: '', contactEmail: null, contactPhone: null, country: null, address: null, taxIdentifier: null };
    this.hr = { employeeWebPunchEnabled: true, geolocationRequired: false, breakPunchRequired: false, dailyProcessingEnabled: true, adjustmentApprovalRequired: true, leaveSelfServiceEnabled: true, leaveSupervisorApprovalEnabled: true };
    this.missingFields = [];
    this.loadError = false;
    this.loading = !!identity;
    if (!identity) return;
    this.companyLoad = forkJoin({ settings: this.service.getSettings(), hr: this.service.getHrSettings(), status: this.service.getSetupStatus() }).subscribe({
      next: response => {
        if (identity !== this.companyIdentity || identity !== this.sessionIdentity()) return;
        this.company = response.settings.data;
        this.hr = response.hr.data;
        this.hr.adjustmentApprovalRequired = true;
        this.missingFields = response.status.data.missingFields ?? [];
        this.loading = false;
      },
      error: () => {
        if (identity !== this.companyIdentity || identity !== this.sessionIdentity()) return;
        this.loading = false;
        this.loadError = true;
        this.toast.error(this.t('settingsLoadToast'));
      },
    });
  }
  get activeLanguage(): CompanyLanguage {
    return this.languageState.language();
  }
  t(key: CompanySettingsTextKey): string { return companySettingsText(this.activeLanguage, key); }
  countryName(code: string, fallback: string): string {
    return new Intl.DisplayNames([this.activeLanguage], { type: 'region' }).of(code) ?? fallback;
  }
  private sessionIdentity(): string | null {
    return this.languageState.currentSessionIdentity();
  }
  saveLanguage(): void {
    const identity = this.sessionIdentity();
    if (!identity || identity !== this.languageIdentity || !this.languageConfirmed || this.languageLoading || this.languageLoadError || this.languageSaving || !this.validLanguage(this.languageDraft) || !this.canSaveLanguage(this.languageDraft)) return;
    const requested = this.languageDraft;
    this.languageSaving = true;
    this.languageError = '';
    this.languageMessage = '';
    this.languageService.save(requested).subscribe({
      next: response => {
        this.languageSaving = false;
        if (identity !== this.sessionIdentity()) {
          this.languageError = this.t('sessionChanged');
          return;
        }
        if (!this.validLanguage(response.data?.language)) {
          this.languageLoadError = true;
          this.languageError = this.t('invalidLanguageResponse');
          return;
        }
        if (!this.languageState.confirmSaved(identity, response.data.language)) {
          this.languageError = this.t('sessionChanged');
          return;
        }
        this.languageConfirmed = response.data.language;
        this.languageDraft = response.data.language;
        this.languageMessage = this.t('languageSaved');
      },
      error: () => {
        this.languageSaving = false;
        if (identity !== this.sessionIdentity()) return;
        this.languageError = this.t('languageSaveError');
      },
    });
  }
  cancelLanguage(): void {
    if (this.languageConfirmed && this.languageIdentity === this.sessionIdentity()) {
      this.languageDraft = this.languageConfirmed;
      this.languageError = '';
      this.languageMessage = '';
    }
  }
  private validLanguage(value: unknown): value is CompanyLanguage { return isCompanyLanguage(value); }
  get allLanguagesSupported(): boolean {
    return this.languageOptions.every(option => this.canSaveLanguage(option.code));
  }
  canSaveLanguage(language: CompanyLanguage): boolean {
    return this.supportedLanguageCodes.has(language);
  }
  private loadSupportedLanguages(identity: string): void {
    this.languageService.supported().subscribe({
      next: codes => {
        if (identity !== this.sessionIdentity() || identity !== this.supportIdentity) return;
        this.supportedLanguageCodes = new Set(codes.filter(isCompanyLanguage));
      },
      error: () => { /* Older API: keep the previously supported es/en codes. */ },
    });
  }
  get dateFormatPreview(): string {
    return this.dateFormats.find(option => option.code === this.formatsDraft.dateFormat)?.example ?? '';
  }
  get numberFormatPreview(): string {
    return this.numberFormats.find(option => option.code === this.formatsDraft.numberFormat)?.example ?? '';
  }
  private loadRegionalFormats(identity: string): void {
    this.formatsLoading = true;
    this.regionalFormatsService.get().subscribe({
      next: response => {
        if (identity !== this.sessionIdentity() || identity !== this.formatsIdentity) return;
        this.formatsLoading = false;
        if (!this.validRegionalFormats(response.data)) {
          this.formatsLoadError = true;
          return;
        }
        this.formatsSupported = true;
        this.formatsConfirmed = response.data;
        this.formatsDraft = { ...response.data };
      },
      error: (error: unknown) => {
        if (identity !== this.sessionIdentity() || identity !== this.formatsIdentity) return;
        this.formatsLoading = false;
        this.formatsLoadError = !(error instanceof HttpErrorResponse && error.status === 404);
      },
    });
  }
  saveRegionalFormats(): void {
    const identity = this.sessionIdentity();
    if (!identity || identity !== this.formatsIdentity || !this.formatsSupported || !this.formatsConfirmed
        || this.formatsLoading || this.formatsLoadError || this.formatsSaving || !this.validRegionalFormats(this.formatsDraft)) return;
    const requested = { ...this.formatsDraft };
    this.formatsSaving = true;
    this.formatsError = '';
    this.formatsMessage = '';
    this.regionalFormatsService.save(requested).subscribe({
      next: response => {
        if (identity !== this.sessionIdentity() || identity !== this.formatsIdentity) return;
        this.formatsSaving = false;
        if (!this.validRegionalFormats(response.data)
            || response.data.dateFormat !== requested.dateFormat
            || response.data.numberFormat !== requested.numberFormat) {
          this.formatsError = this.t('formatsSaveError');
          return;
        }
        this.formatsConfirmed = response.data;
        this.formatsDraft = { ...response.data };
        this.formatsMessage = this.t('formatsSaved');
      },
      error: () => {
        if (identity !== this.sessionIdentity() || identity !== this.formatsIdentity) return;
        this.formatsSaving = false;
        this.formatsError = this.t('formatsSaveError');
      },
    });
  }
  cancelRegionalFormats(): void {
    if (!this.formatsConfirmed || this.formatsIdentity !== this.sessionIdentity()) return;
    this.formatsDraft = { ...this.formatsConfirmed };
    this.formatsError = '';
    this.formatsMessage = '';
  }
  private validRegionalFormats(value: unknown): value is TenantRegionalFormats {
    if (!value || typeof value !== 'object') return false;
    const formats = value as Partial<TenantRegionalFormats>;
    return this.dateFormats.some(option => option.code === formats.dateFormat)
      && this.numberFormats.some(option => option.code === formats.numberFormat);
  }
  get availableTimezones(): string[] { return this.countries.find(country => country.code === this.company.country)?.timezones ?? []; }
  onCountryChange() { const country = this.countries.find(option => option.code === this.company.country); if (!country) { this.company.timezone = ''; return; } this.company.currency = country.currency; this.company.timezone = country.timezones[0]; }
  missingFieldLabel(field: string): string { const labels: Record<string, CompanySettingsTextKey> = { 'company.name': 'companyName', companyName: 'companyName', 'company.country': 'country', country: 'country', 'company.currency': 'currency', currency: 'currency', 'company.timezone': 'timezone', timezone: 'timezone', 'attendance.adjustment_approval_required': 'adjustmentApproval', 'attendance.adjustmentApprovalRequired': 'adjustmentApproval', adjustmentApprovalRequired: 'adjustmentApproval' }; return labels[field] ? this.t(labels[field]) : field; }
  get missingFieldLabels(): string { return this.missingFields.map(field => this.missingFieldLabel(field)).join(', '); }
  save() { if (!this.company.companyName.trim()) { this.toast.error(this.t('requiredCompanyName')); return; } if (!this.company.timezone || !this.company.currency) { this.toast.error(this.t('requiredTimezoneCurrency')); return; } this.hr.adjustmentApprovalRequired = true; this.saving = true; this.service.updateSettings(this.company).pipe(switchMap(() => this.service.updateHrSettings(this.hr)), finalize(() => this.saving = false)).subscribe({ next: () => this.toast.success(this.t('settingsSaved')), error: e => this.fail(e) }); }
  private fail(error: unknown) { this.toast.error(error instanceof Error ? error.message : this.t('settingsSaveError')); }
}
