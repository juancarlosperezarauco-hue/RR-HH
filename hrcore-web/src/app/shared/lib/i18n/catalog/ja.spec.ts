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

describe('Japanese catalog', () => {
  const complete: Record<TenantTextKey, string> = japanese;

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
    expect(new Set(tenantTextKeys).size).toBe(required.length);
    expect(Object.values(complete).every(value => value.trim().length > 0)).toBeTrue();
  });

  it('preserves Spanish interpolation parameters', () => {
    const parameters = (value: string) => [...value.matchAll(/\{[a-zA-Z][a-zA-Z0-9_]*\}/g)]
      .map(match => match[0]).sort();
    for (const key of tenantTextKeys) {
      expect(parameters(complete[key])).withContext(key).toEqual(parameters(es[key]));
    }
  });

  it('provides Japanese wording for navigation, setup, appearance and errors', () => {
    expect(complete['nav.users']).toBe('ユーザー');
    expect(complete['setup.identityTitle']).toBe('企業について教えてください');
    expect(complete['appearance.save']).toBe('変更を保存');
    expect(complete['appearance.error.forbiddenSave']).toBe('外観を保存する権限がありません。');
  });
});
