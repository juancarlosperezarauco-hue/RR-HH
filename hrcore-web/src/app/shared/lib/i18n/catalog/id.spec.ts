import { tenantTextKeys, TenantTextKey } from '../keys';
import { es } from './es';
import { en } from './en';
import { ptBR } from './pt-BR';
import { fr } from './fr';
import { de } from './de';
import { it as italian } from './it';
import { zhHans } from './zh-Hans';
import { zhHant } from './zh-Hant';
import { ja as japanese } from './ja';
import { ko } from './ko';
import { ru } from './ru';
import { ar } from './ar';
import { hi } from './hi';
import { tr } from './tr';
import { id as indonesian } from './id';

describe('Indonesian catalog', () => {
  const complete: Record<TenantTextKey, string> = indonesian;

  it('covers exactly the existing keys with nonempty translations', () => {
    const required = [...tenantTextKeys].sort();
    expect(Object.keys(complete).sort()).toEqual(required);
    expect(Object.keys(es).sort()).toEqual(required);
    expect(Object.keys(en).sort()).toEqual(required);
    expect(Object.keys(ptBR).sort()).toEqual(required);
    expect(Object.keys(fr).sort()).toEqual(required);
    expect(Object.keys(de).sort()).toEqual(required);
    expect(Object.keys(italian).sort()).toEqual(required);
    expect(Object.keys(zhHans).sort()).toEqual(required);
    expect(Object.keys(zhHant).sort()).toEqual(required);
    expect(Object.keys(japanese).sort()).toEqual(required);
    expect(Object.keys(ko).sort()).toEqual(required);
    expect(Object.keys(ru).sort()).toEqual(required);
    expect(Object.keys(ar).sort()).toEqual(required);
    expect(Object.keys(hi).sort()).toEqual(required);
    expect(Object.keys(tr).sort()).toEqual(required);
    expect(new Set(tenantTextKeys).size).toBe(required.length);
    expect(Object.values(complete).every(value => value.trim().length > 0)).toBeTrue();
  });

  it('preserves Spanish interpolation parameters and their multiplicity', () => {
    const parameters = (value: string) => [...value.matchAll(/\{[a-zA-Z][a-zA-Z0-9_]*\}/g)]
      .map(match => match[0]).sort();
    for (const key of tenantTextKeys) {
      expect(parameters(complete[key])).withContext(key).toEqual(parameters(es[key]));
    }
  });

  it('uses Indonesian wording for navigation, setup, save and error states', () => {
    expect(complete['nav.users']).toBe('Pengguna');
    expect(complete['setup.identityTitle']).toBe('Ceritakan tentang perusahaan Anda');
    expect(complete['appearance.save']).toBe('Simpan perubahan');
    expect(complete['appearance.cancel']).toBe('Batal');
    expect(complete['appearance.restore']).toBe('Pulihkan pengaturan asli');
    expect(complete['appearance.error.forbiddenSave']).toBe('Anda tidak memiliki izin untuk menyimpan tampilan.');
  });
});
