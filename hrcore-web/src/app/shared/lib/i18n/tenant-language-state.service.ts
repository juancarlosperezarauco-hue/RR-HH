import { inject, Injectable, signal } from '@angular/core';
import { CompanyLanguage, isCompanyLanguage } from '../../../entities/company/model/company-language-translations';
import { AuthStorageService } from '../storage/auth-storage.service';
import { en } from './catalog/en';
import { es } from './catalog/es';
import { ptBR } from './catalog/pt-BR';
import { fr } from './catalog/fr';
import { de } from './catalog/de';
import { it } from './catalog/it';
import { zhHans } from './catalog/zh-Hans';
import { zhHant } from './catalog/zh-Hant';
import { ja } from './catalog/ja';
import { ko } from './catalog/ko';
import { ru } from './catalog/ru';
import { ar } from './catalog/ar';
import { hi } from './catalog/hi';
import { tr } from './catalog/tr';
import { id } from './catalog/id';
import { TenantTextKey } from './keys';

type LoadStatus = 'idle' | 'loading' | 'ready' | 'error';
const catalogs: Record<CompanyLanguage, Record<TenantTextKey, string>> = {
  es, en, 'pt-BR': ptBR, fr, de, it, 'zh-Hans': zhHans,
  'zh-Hant': zhHant, ja, ko, ru, ar, hi, tr, id,
};

@Injectable({ providedIn: 'root' })
export class TenantLanguageStateService {
  private readonly authStorage = inject(AuthStorageService);
  private readonly value = signal<CompanyLanguage | null>(null);
  private readonly loadState = signal<LoadStatus>('idle');
  private readonly sessionVersion = signal(0);
  private activeIdentity: string | null = null;
  private layoutActive = false;
  private layoutGeneration = 0;
  private revision = 0;

  readonly confirmed = this.value.asReadonly();
  readonly status = this.loadState.asReadonly();
  readonly sessionRevision = this.sessionVersion.asReadonly();
  readonly language = (): CompanyLanguage => this.confirmedForCurrentSession() ?? 'es';

  enterLayout(): number {
    this.layoutGeneration++;
    this.layoutActive = true;
    this.activeIdentity = null;
    this.revision++;
    this.value.set(null);
    this.loadState.set('idle');
    this.sessionVersion.update(value => value + 1);
    return this.layoutGeneration;
  }

  activate(identity: string | null): boolean {
    if (identity === this.activeIdentity) return false;
    this.activeIdentity = identity;
    this.revision++;
    this.value.set(null);
    this.loadState.set(identity ? 'loading' : 'idle');
    this.sessionVersion.update(value => value + 1);
    return true;
  }

  getRevision(): number { return this.revision; }

  currentSessionIdentity(): string | null {
    const session = this.authStorage.getSession();
    return session?.scope === 'TENANT' && session.tenantSlug
      ? `${session.loginId}:${session.tenantSlug}` : null;
  }

  confirmedForCurrentSession(): CompanyLanguage | null {
    const language = this.value();
    return this.layoutActive && this.activeIdentity !== null
      && this.activeIdentity === this.currentSessionIdentity() ? language : null;
  }

  confirmFetched(identity: string, revision: number, language: unknown): boolean {
    if (!this.matches(identity) || this.revision !== revision || !this.valid(language)) return false;
    this.value.set(language);
    this.loadState.set('ready');
    return true;
  }

  failFetched(identity: string, revision: number): void {
    if (this.matches(identity) && this.revision === revision) this.loadState.set('error');
  }

  confirmSaved(identity: string | null, language: unknown): boolean {
    if (!identity || !this.matches(identity) || !this.valid(language)) return false;
    this.revision++;
    this.value.set(language);
    this.loadState.set('ready');
    return true;
  }

  deactivate(layoutGeneration: number): void {
    if (this.layoutGeneration !== layoutGeneration) return;
    this.layoutActive = false;
    this.activeIdentity = null;
    this.revision++;
    this.value.set(null);
    this.loadState.set('idle');
    this.sessionVersion.update(value => value + 1);
  }

  t(key: TenantTextKey): string {
    return catalogs[this.language()][key] ?? es[key];
  }

  private matches(identity: string): boolean {
    return this.layoutActive && this.activeIdentity === identity && this.currentSessionIdentity() === identity;
  }

  private valid(value: unknown): value is CompanyLanguage { return isCompanyLanguage(value); }
}
