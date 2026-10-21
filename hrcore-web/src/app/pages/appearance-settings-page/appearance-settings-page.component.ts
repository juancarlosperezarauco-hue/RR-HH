import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnDestroy, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { LucideAngularModule } from 'lucide-angular';
import { AppearanceSettings, AppearanceSettingsService } from '../../entities/company/api/appearance-settings.service';
import { TenantAppearanceStateService } from '../../entities/company/model/tenant-appearance-state.service';
import { TenantLanguageStateService } from '../../shared/lib/i18n/tenant-language-state.service';
import { TenantTextKey } from '../../shared/lib/i18n/keys';
import {
  APPEARANCE_PRESETS, AppearanceColors, AppearancePreset, ColorKey, ColorMode,
  contrastRatio, generatePalettes, isHexColor, mix, normalizeHex, ORIGINAL_COLORS, readableAccent,
  PaletteOption, readableText,
} from './appearance-palette';

type AppearanceMessage = { key: TenantTextKey } | { raw: string } | null;

@Component({
  selector: 'app-appearance-settings-page',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './appearance-settings-page.component.html',
  styleUrl: './appearance-settings-page.component.css',
})
export class AppearanceSettingsPageComponent implements OnInit, OnDestroy {
  private readonly appearanceService = inject(AppearanceSettingsService);
  private readonly appearanceState = inject(TenantAppearanceStateService);
  private readonly languageState = inject(TenantLanguageStateService);
  private readonly destroyRef = inject(DestroyRef);
  readonly presets = APPEARANCE_PRESETS;
  readonly isHexColor = isHexColor;
  private readonly presetKeys: Record<string, { name: TenantTextKey; description: TenantTextKey }> = {
    original: { name: 'appearance.preset.original.name', description: 'appearance.preset.original.description' },
    corporate: { name: 'appearance.preset.corporate.name', description: 'appearance.preset.corporate.description' },
    modern: { name: 'appearance.preset.modern.name', description: 'appearance.preset.modern.description' },
    natural: { name: 'appearance.preset.natural.name', description: 'appearance.preset.natural.description' },
    minimal: { name: 'appearance.preset.minimal.name', description: 'appearance.preset.minimal.description' },
    warm: { name: 'appearance.preset.warm.name', description: 'appearance.preset.warm.description' },
  };
  private readonly paletteKeys: Record<string, { name: TenantTextKey; description: TenantTextKey }> = {
    harmonic: { name: 'appearance.palette.harmonic.name', description: 'appearance.palette.harmonic.description' },
    contrast: { name: 'appearance.palette.contrast.name', description: 'appearance.palette.contrast.description' },
    calm: { name: 'appearance.palette.calm.name', description: 'appearance.palette.calm.description' },
    vivid: { name: 'appearance.palette.vivid.name', description: 'appearance.palette.vivid.description' },
  };
  private readonly colorKeys: Record<ColorKey, { label: TenantTextKey; description: TenantTextKey }> = {
    primary: { label: 'appearance.color.primary', description: 'appearance.color.primaryHelp' },
    secondary: { label: 'appearance.color.secondary', description: 'appearance.color.secondaryHelp' },
    sidebar: { label: 'appearance.color.sidebar', description: 'appearance.color.sidebarHelp' },
  };
  readonly colorFields: readonly { key: ColorKey; label: string; description: string }[] = [
    { key: 'primary', label: 'Principal', description: 'Botones y acciones' },
    { key: 'secondary', label: 'Secundario', description: 'Detalles y énfasis' },
    { key: 'sidebar', label: 'Menú lateral', description: 'Fondo de navegación' },
  ];
  readonly maxLogoBytes = 2 * 1024 * 1024;

  selectedPresetId = 'original';
  selectedPaletteId: string | null = null;
  customized = false;
  mode: ColorMode = 'light';
  colors: AppearanceColors = { ...ORIGINAL_COLORS };
  colorDrafts: Record<ColorKey, string> = { ...ORIGINAL_COLORS };
  baseColor = ORIGINAL_COLORS.primary;
  baseColorDraft = ORIGINAL_COLORS.primary;
  palettes: PaletteOption[] = [];
  logoUrl: string | null = null;
  logoName = '';
  logoError: TenantTextKey | null = null;
  loading = true;
  loadFailed = false;
  saving = false;
  loadError: AppearanceMessage = null;
  saveError: AppearanceMessage = null;
  saveMessage: AppearanceMessage = null;
  private confirmed: AppearanceSettings | null = null;
  private editVersion = 0;
  private logoOperation = 0;
  private destroyed = false;

  ngOnInit(): void {
    this.loadAppearance();
  }

  get canSave(): boolean {
    return !this.loading && !this.loadFailed && !this.saving && this.confirmed !== null
      && this.colorFields.every(field => isHexColor(this.colorDrafts[field.key]))
      && this.isValidSettings(this.currentSettings())
      && !this.sameSettings(this.currentSettings(), this.confirmed);
  }

  get hasConfirmedSettings(): boolean {
    return this.confirmed !== null;
  }

  loadAppearance(): void {
    if (this.loading && this.confirmed !== null) return;
    this.loading = true;
    this.loadFailed = false;
    this.loadError = null;
    const versionAtRequest = this.editVersion;
    const identityAtRequest = this.appearanceState.currentSessionIdentity();
    this.appearanceService.get().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: response => {
        this.loading = false;
        if (identityAtRequest !== this.appearanceState.currentSessionIdentity()) {
          this.loadFailed = true;
          this.loadError = { key: 'appearance.error.sessionLoad' };
          return;
        }
        if (!this.isValidSettings(response.data)) {
          this.loadFailed = true;
          this.loadError = { key: 'appearance.error.invalidLoad' };
          return;
        }
        this.confirmed = { ...response.data };
        if (versionAtRequest === this.editVersion) this.applySettings(response.data);
      },
      error: error => {
        this.loading = false;
        this.loadFailed = true;
        this.loadError = this.errorMessage(error, 'cargar');
      },
    });
  }

  save(): void {
    if (!this.canSave) return;
    const requested = this.currentSettings();
    const versionAtRequest = this.editVersion;
    const identityAtRequest = this.appearanceState.currentSessionIdentity();
    this.saving = true;
    this.saveError = null;
    this.saveMessage = null;
    this.appearanceService.save(requested).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: response => {
        this.saving = false;
        if (identityAtRequest !== this.appearanceState.currentSessionIdentity()) {
          this.loadFailed = true;
          this.saveError = { key: 'appearance.error.sessionSave' };
          return;
        }
        if (!this.isValidSettings(response.data)) {
          this.saveError = { key: 'appearance.error.invalidSave' };
          this.loadFailed = true;
          return;
        }
        this.confirmed = { ...response.data };
        this.appearanceState.confirmSaved(identityAtRequest, response.data);
        if (versionAtRequest === this.editVersion) this.applySettings(response.data);
        this.saveMessage = response.message ? { raw: response.message } : { key: 'appearance.saved' };
      },
      error: error => {
        this.saving = false;
        this.saveError = this.errorMessage(error, 'guardar');
      },
    });
  }

  get selectedPresetName(): string {
    const preset = this.presets.find(option => option.id === this.selectedPresetId);
    return preset ? this.presetName(preset) : this.t('appearance.preset.original.name');
  }

  t(key: TenantTextKey): string { return this.languageState.t(key); }
  message(value: AppearanceMessage): string { return value ? ('key' in value ? this.t(value.key) : value.raw) : ''; }
  presetName(preset: AppearancePreset): string { return this.presetKeys[preset.id] ? this.t(this.presetKeys[preset.id].name) : preset.name; }
  presetDescription(preset: AppearancePreset): string { return this.presetKeys[preset.id] ? this.t(this.presetKeys[preset.id].description) : preset.description; }
  paletteName(option: PaletteOption): string { return this.paletteKeys[option.id] ? this.t(this.paletteKeys[option.id].name) : option.name; }
  paletteDescription(option: PaletteOption): string { return this.paletteKeys[option.id] ? this.t(this.paletteKeys[option.id].description) : option.description; }
  colorLabel(key: ColorKey): string { return this.t(this.colorKeys[key].label); }
  colorDescription(key: ColorKey): string { return this.t(this.colorKeys[key].description); }

  get previewStyles(): Record<string, string> {
    const dark = this.mode === 'dark';
    const background = dark ? '#111827' : '#f5f8fc';
    const surface = dark ? '#1f2937' : '#ffffff';
    const muted = dark ? '#293548' : '#edf3f9';
    const foreground = dark ? '#f9fafb' : '#1f2937';
    const mutedForeground = dark ? '#c4d0df' : '#526173';
    const border = dark ? '#39475a' : '#dce5ee';
    const primaryText = readableText(this.colors.primary);
    const secondaryText = readableText(this.colors.secondary);
    const sidebarText = readableText(this.colors.sidebar);
    const highlight = mix(this.colors.primary, surface, dark ? 0.78 : 0.90);
    return {
      '--pv-primary': this.colors.primary,
      '--pv-primary-text': primaryText,
      '--pv-primary-accent': readableAccent(this.colors.primary, surface, foreground),
      '--pv-secondary': this.colors.secondary,
      '--pv-secondary-text': secondaryText,
      '--pv-secondary-accent': readableAccent(this.colors.secondary, surface, foreground),
      '--pv-sidebar': this.colors.sidebar,
      '--pv-sidebar-text': sidebarText,
      '--pv-background': background,
      '--pv-surface': surface,
      '--pv-muted': muted,
      '--pv-foreground': foreground,
      '--pv-muted-foreground': mutedForeground,
      '--pv-border': border,
      '--pv-highlight': highlight,
    };
  }

  get contrastWarnings(): string[] {
    const warnings: string[] = [];
    for (const field of this.colorFields) {
      const background = this.colors[field.key];
      const ratio = contrastRatio(background, readableText(background));
      if (ratio < 4.5) warnings.push(`${this.colorLabel(field.key)}: ${this.t('appearance.contrastRatio')} ${ratio.toFixed(1)}:1`);
    }
    return warnings;
  }

  thumbnailStyles(preset: AppearancePreset): Record<string, string> {
    return {
      '--mini-primary': preset.colors.primary,
      '--mini-secondary': preset.colors.secondary,
      '--mini-sidebar': preset.colors.sidebar,
    };
  }

  paletteStyles(option: PaletteOption): Record<string, string> {
    return {
      '--mini-primary': option.colors.primary,
      '--mini-secondary': option.colors.secondary,
      '--mini-sidebar': option.colors.sidebar,
    };
  }

  selectPreset(preset: AppearancePreset): void {
    if (this.saving) return;
    this.editVersion++;
    this.saveMessage = null;
    this.selectedPresetId = preset.id;
    this.selectedPaletteId = null;
    this.customized = false;
    this.colors = { ...preset.colors };
    this.colorDrafts = { ...preset.colors };
    this.baseColor = preset.colors.primary;
    this.baseColorDraft = preset.colors.primary;
    this.palettes = [];
  }

  setMode(mode: ColorMode): void {
    if (this.saving) return;
    this.editVersion++;
    this.saveMessage = null;
    this.mode = mode;
  }

  onBaseColorInput(value: string): void {
    if (this.saving) return;
    this.editVersion++;
    this.baseColorDraft = value;
    if (isHexColor(value)) this.baseColor = normalizeHex(value);
    this.palettes = [];
    this.selectedPaletteId = null;
  }

  onBaseColorPicker(value: string): void {
    if (this.saving) return;
    this.editVersion++;
    this.baseColor = normalizeHex(value);
    this.baseColorDraft = this.baseColor;
    this.palettes = [];
    this.selectedPaletteId = null;
  }

  generate(): void {
    if (this.saving) return;
    if (!isHexColor(this.baseColorDraft)) return;
    this.baseColor = normalizeHex(this.baseColorDraft);
    this.baseColorDraft = this.baseColor;
    this.palettes = generatePalettes(this.baseColor);
    this.selectedPaletteId = null;
  }

  applyPalette(option: PaletteOption): void {
    if (this.saving) return;
    this.editVersion++;
    this.saveMessage = null;
    this.colors = { ...option.colors };
    this.colorDrafts = { ...option.colors };
    this.selectedPaletteId = option.id;
    this.customized = true;
  }

  onHexInput(key: ColorKey, value: string): void {
    if (this.saving) return;
    this.editVersion++;
    this.saveMessage = null;
    this.colorDrafts[key] = value;
    if (isHexColor(value)) this.setColor(key, normalizeHex(value));
  }

  onColorPicker(key: ColorKey, value: string): void {
    if (this.saving) return;
    this.setColor(key, normalizeHex(value));
    this.colorDrafts[key] = this.colors[key];
  }

  isColorDraftInvalid(key: ColorKey): boolean {
    return !isHexColor(this.colorDrafts[key]);
  }

  cancel(): void {
    if (this.saving || !this.confirmed) return;
    this.editVersion++;
    this.applySettings(this.confirmed);
    this.clearLogo();
    this.logoError = null;
    this.saveError = null;
    this.saveMessage = null;
  }

  restoreOriginal(): void {
    if (this.saving) return;
    this.resetToOriginal();
    this.saveMessage = null;
  }

  async onLogoSelected(event: Event): Promise<void> {
    if (this.saving) return;
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    const operation = ++this.logoOperation;
    this.logoError = null;
    if (file.size === 0 || file.size > this.maxLogoBytes) {
      this.logoError = 'appearance.logo.sizeError';
      return;
    }
    if (!['image/png', 'image/jpeg', 'image/webp'].includes(file.type)) {
      this.logoError = 'appearance.logo.typeError';
      return;
    }
    const extensions: Record<string, RegExp> = {
      'image/png': /\.png$/i,
      'image/jpeg': /\.jpe?g$/i,
      'image/webp': /\.webp$/i,
    };
    if (!extensions[file.type].test(file.name)) {
      this.logoError = 'appearance.logo.extensionError';
      return;
    }
    try {
      const bytes = new Uint8Array(await file.slice(0, 12).arrayBuffer());
      if (this.destroyed || operation !== this.logoOperation) return;
      if (!this.matchesImageSignature(file.type, bytes)) {
        this.logoError = 'appearance.logo.signatureError';
        return;
      }
      const nextUrl = URL.createObjectURL(file);
      this.clearLogo(false);
      this.logoUrl = nextUrl;
      this.logoName = file.name;
    } catch {
      if (!this.destroyed && operation === this.logoOperation) this.logoError = 'appearance.logo.readError';
    }
  }

  removeLogo(): void {
    if (this.saving) return;
    this.clearLogo();
    this.logoError = null;
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    this.clearLogo();
  }

  private setColor(key: ColorKey, value: string): void {
    if (this.saving) return;
    this.editVersion++;
    this.saveMessage = null;
    this.colors = { ...this.colors, [key]: value };
    this.selectedPaletteId = null;
    this.customized = true;
  }

  private resetToOriginal(): void {
    this.selectPreset(this.presets[0]);
    this.mode = 'light';
  }

  private currentSettings(): AppearanceSettings {
    return { preset: this.selectedPresetId, ...this.colors, mode: this.mode };
  }

  private applySettings(settings: AppearanceSettings): void {
    this.selectedPresetId = settings.preset;
    this.selectedPaletteId = null;
    this.colors = { primary: settings.primary, secondary: settings.secondary, sidebar: settings.sidebar };
    this.colorDrafts = { ...this.colors };
    this.customized = this.colorFields.some(field =>
      this.colors[field.key] !== this.presets.find(preset => preset.id === settings.preset)?.colors[field.key]);
    this.mode = settings.mode;
    this.baseColor = settings.primary;
    this.baseColorDraft = settings.primary;
    this.palettes = [];
  }

  private isValidSettings(value: AppearanceSettings | null | undefined): value is AppearanceSettings {
    return !!value && this.presets.some(preset => preset.id === value.preset)
      && (value.mode === 'light' || value.mode === 'dark')
      && isHexColor(value.primary) && isHexColor(value.secondary) && isHexColor(value.sidebar);
  }

  private sameSettings(first: AppearanceSettings, second: AppearanceSettings): boolean {
    return first.preset === second.preset && first.primary === second.primary
      && first.secondary === second.secondary && first.sidebar === second.sidebar && first.mode === second.mode;
  }

  private errorMessage(error: unknown, action: 'cargar' | 'guardar'): AppearanceMessage {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 401) return { key: action === 'cargar' ? 'appearance.error.unauthorizedLoad' : 'appearance.error.unauthorizedSave' };
      if (error.status === 403) return { key: action === 'cargar' ? 'appearance.error.forbiddenLoad' : 'appearance.error.forbiddenSave' };
    }
    return error instanceof Error && !(error instanceof HttpErrorResponse)
      ? { raw: error.message } : { key: action === 'cargar' ? 'appearance.error.load' : 'appearance.error.save' };
  }

  private clearLogo(invalidatePending = true): void {
    if (invalidatePending) this.logoOperation++;
    if (this.logoUrl) URL.revokeObjectURL(this.logoUrl);
    this.logoUrl = null;
    this.logoName = '';
  }

  private matchesImageSignature(type: string, bytes: Uint8Array): boolean {
    if (type === 'image/png') return bytes.length >= 8 && [137, 80, 78, 71, 13, 10, 26, 10].every((byte, index) => bytes[index] === byte);
    if (type === 'image/jpeg') return bytes.length >= 3 && bytes[0] === 255 && bytes[1] === 216 && bytes[2] === 255;
    return bytes.length >= 12 && String.fromCharCode(...bytes.slice(0, 4)) === 'RIFF' && String.fromCharCode(...bytes.slice(8, 12)) === 'WEBP';
  }
}
