import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize, forkJoin, switchMap } from 'rxjs';
import { CompanySettings, CompanySettingsService, HrSettings } from '../../entities/company';
import { companySettingsText, CompanySettingsTextKey } from '../../entities/company/model/company-language-translations';
import { CompanySetupStateService } from '../../shared/lib/company/company-setup-state.service';
import { TenantLanguageStateService } from '../../shared/lib/i18n/tenant-language-state.service';
import { TenantTextKey } from '../../shared/lib/i18n/keys';
import { ToastService } from '../../shared/ui/toast/toast.service';

type CountryOption = { code: string; name: string; currency: string; timezones: string[] };

@Component({ selector: 'app-company-setup-page', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './company-setup-page.component.html', styles: [`
  .field-label{display:block;font-size:.78rem;font-weight:700;letter-spacing:.01em;color:var(--foreground)}.field-label>span{color:var(--primary)}.field-input{display:block;width:100%;margin-top:.45rem;border:1px solid var(--input);border-radius:.9rem;background:var(--card);padding:.78rem 1rem;font-family:inherit;font-size:.9rem;font-weight:500;outline:none;transition:border-color .2s,box-shadow .2s,background .2s}.field-input::placeholder{color:var(--muted-foreground);font-family:inherit;font-size:.85rem;font-weight:400;opacity:.8}.field-input:focus{border-color:var(--primary);background:var(--background);box-shadow:0 0 0 3px color-mix(in srgb,var(--primary) 12%,transparent)}.policy-card{display:flex;align-items:flex-start;gap:.75rem;border:1px solid var(--border);border-radius:1rem;background:var(--card);padding:1rem;cursor:pointer;transition:border-color .2s,background .2s,box-shadow .2s}.policy-card:hover{border-color:color-mix(in srgb,var(--primary) 55%,transparent);background:color-mix(in srgb,var(--muted) 50%,transparent)}.policy-card:has(input:checked){border-color:color-mix(in srgb,var(--primary) 55%,transparent);background:color-mix(in srgb,var(--primary) 6%,transparent);box-shadow:0 0 0 1px color-mix(in srgb,var(--primary) 12%,transparent)}.policy-card input{margin-top:.2rem;height:1rem;width:1rem;accent-color:var(--primary)}.policy-card strong{display:block;font-size:.86rem}.policy-card small{display:block;margin-top:.25rem;color:var(--muted-foreground);font-size:.75rem;line-height:1.35;font-weight:500}
`] })
export class CompanySetupPageComponent implements OnInit {
  private readonly service = inject(CompanySettingsService);
  private readonly state = inject(CompanySetupStateService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly languageState = inject(TenantLanguageStateService);
  readonly steps = [
    { number: 1, eyebrow: 'setup.identity', title: 'setup.identityTitle', description: 'setup.identityDescription' },
    { number: 2, eyebrow: 'setup.location', title: 'setup.locationTitle', description: 'setup.locationDescription' },
    { number: 3, eyebrow: 'setup.policies', title: 'setup.policiesTitle', description: 'setup.policiesDescription' },
    { number: 4, eyebrow: 'setup.confirmation', title: 'setup.confirmationTitle', description: 'setup.confirmationDescription' }
  ] as const;
  readonly countries: CountryOption[] = [
    { code: 'BO', name: 'Bolivia', currency: 'BOB', timezones: ['America/La_Paz'] },
    { code: 'AR', name: 'Argentina', currency: 'ARS', timezones: ['America/Argentina/Buenos_Aires', 'America/Argentina/Catamarca', 'America/Argentina/Cordoba', 'America/Argentina/Jujuy', 'America/Argentina/La_Rioja', 'America/Argentina/Mendoza', 'America/Argentina/Rio_Gallegos', 'America/Argentina/Salta', 'America/Argentina/San_Juan', 'America/Argentina/San_Luis', 'America/Argentina/Tucuman', 'America/Argentina/Ushuaia'] },
    { code: 'BR', name: 'Brasil', currency: 'BRL', timezones: ['America/Araguaina', 'America/Bahia', 'America/Belem', 'America/Boa_Vista', 'America/Campo_Grande', 'America/Cuiaba', 'America/Fortaleza', 'America/Manaus', 'America/Recife', 'America/Rio_Branco', 'America/Santarem', 'America/Sao_Paulo'] },
    { code: 'CL', name: 'Chile', currency: 'CLP', timezones: ['America/Santiago', 'Pacific/Easter'] },
    { code: 'CO', name: 'Colombia', currency: 'COP', timezones: ['America/Bogota'] },
    { code: 'EC', name: 'Ecuador', currency: 'USD', timezones: ['America/Guayaquil', 'Pacific/Galapagos'] },
    { code: 'MX', name: 'México', currency: 'MXN', timezones: ['America/Cancun', 'America/Chihuahua', 'America/Hermosillo', 'America/Matamoros', 'America/Mazatlan', 'America/Merida', 'America/Mexico_City', 'America/Monterrey', 'America/Ojinaga', 'America/Tijuana'] },
    { code: 'PY', name: 'Paraguay', currency: 'PYG', timezones: ['America/Asuncion'] },
    { code: 'PE', name: 'Perú', currency: 'PEN', timezones: ['America/Lima'] },
    { code: 'UY', name: 'Uruguay', currency: 'UYU', timezones: ['America/Montevideo'] },
    { code: 'US', name: 'Estados Unidos', currency: 'USD', timezones: ['America/New_York', 'America/Chicago', 'America/Denver', 'America/Los_Angeles', 'America/Anchorage', 'Pacific/Honolulu'] },
    { code: 'ES', name: 'España', currency: 'EUR', timezones: ['Europe/Madrid', 'Atlantic/Canary'] }
  ];
  readonly currencies = ['BOB', 'ARS', 'BRL', 'CLP', 'COP', 'EUR', 'MXN', 'PEN', 'PYG', 'USD', 'UYU'];
  currentStep = 1;
  loading = true;
  loadError = false;
  saving = false;
  missingFields: string[] = [];
  company: CompanySettings = { companyName: '', legalName: null, timezone: '', currency: '', contactEmail: null, contactPhone: null, country: null, address: null, taxIdentifier: null };
  hr: HrSettings = { employeeWebPunchEnabled: true, geolocationRequired: false, breakPunchRequired: false, dailyProcessingEnabled: true, adjustmentApprovalRequired: true, leaveSelfServiceEnabled: true, leaveSupervisorApprovalEnabled: true };

  ngOnInit(): void {
    forkJoin({ settings: this.service.getSettings(), hr: this.service.getHrSettings(), status: this.service.getSetupStatus() }).subscribe({
      next: response => {
        this.company = response.settings.data;
        this.hr = response.hr.data;
        this.hr.adjustmentApprovalRequired = true;
        this.missingFields = response.status.data.missingFields ?? [];
        this.loading = false;
      },
      error: () => { this.loading = false; this.loadError = true; this.toast.error(this.companyT('settingsLoadToast')); }
    });
  }
  next(): void {
    if (!this.validateCurrentStep()) return;
    if (this.currentStep < 4) { this.currentStep++; return; }
    this.saving = true;
    this.hr.adjustmentApprovalRequired = true;
    this.service.updateSettings(this.company).pipe(
      switchMap(() => this.service.updateHrSettings(this.hr)),
      switchMap(() => this.service.completeSetup(this.company.timezone)),
      finalize(() => this.saving = false)
    ).subscribe({
      next: response => { this.state.setConfigured(response.data); this.toast.success(this.t('setup.completedToast')); this.router.navigateByUrl('/dashboard/home'); },
      error: error => this.fail(error)
    });
  }
  previous(): void { if (!this.saving && this.currentStep > 1) this.currentStep--; }
  get availableTimezones(): string[] { return this.countries.find(country => country.code === this.company.country)?.timezones ?? []; }
  onCountryChange(): void { const country = this.countries.find(option => option.code === this.company.country); if (!country) { this.company.timezone = ''; return; } this.company.currency = country.currency; this.company.timezone = country.timezones[0]; }
  enabledPolicies(): number { return Object.values(this.hr).filter(Boolean).length; }
  fieldMissing(field: string): boolean { return this.missingFields.some(value => value.toLowerCase().includes(field.toLowerCase())); }
  t(key: TenantTextKey): string { return this.languageState.t(key); }
  companyT(key: CompanySettingsTextKey): string { return companySettingsText(this.languageState.language(), key); }
  countryName(country: CountryOption): string { return new Intl.DisplayNames([this.languageState.language()], { type: 'region' }).of(country.code) ?? country.name; }
  missingFieldLabel(field: string): string { const labels: Record<string, CompanySettingsTextKey> = { 'company.name': 'companyName', companyName: 'companyName', 'company.country': 'country', country: 'country', 'company.currency': 'currency', currency: 'currency', 'company.timezone': 'timezone', timezone: 'timezone', 'attendance.adjustment_approval_required': 'adjustmentApproval', 'attendance.adjustmentApprovalRequired': 'adjustmentApproval', adjustmentApprovalRequired: 'adjustmentApproval' }; return labels[field] ? this.companyT(labels[field]) : field; }
  private validateCurrentStep(): boolean {
    if (this.currentStep === 1 && !this.company.companyName.trim()) { this.toast.error(this.companyT('requiredCompanyName')); return false; }
    if (this.currentStep === 2 && (!this.company.country || !this.company.currency || !this.company.timezone)) { this.toast.error(this.t('setup.requiredRegion')); return false; }
    if (this.currentStep === 3 && !this.hr.adjustmentApprovalRequired) { this.hr.adjustmentApprovalRequired = true; this.toast.error(this.t('setup.adjustmentRequired')); return false; }
    return true;
  }
  private fail(error: unknown): void { this.toast.error(error instanceof Error ? error.message : this.t('setup.completeError')); }
}
