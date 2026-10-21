import { tenantTextKeys, TenantTextKey } from '../keys';
import { es } from './es';
import { en } from './en';
import { ptBR } from './pt-BR';
import { fr } from './fr';
import { de } from './de';
import { it as italian } from './it';
import { zhHans } from './zh-Hans';

describe('Simplified Chinese catalog', () => {
  const complete: Record<TenantTextKey, string> = zhHans;

  it('covers exactly the existing keys with nonempty translations', () => {
    const required = [...tenantTextKeys].sort();
    expect(Object.keys(complete).sort()).toEqual(required);
    expect(Object.keys(es).sort()).toEqual(required);
    expect(Object.keys(en).sort()).toEqual(required);
    expect(Object.keys(ptBR).sort()).toEqual(required);
    expect(Object.keys(fr).sort()).toEqual(required);
    expect(Object.keys(de).sort()).toEqual(required);
    expect(Object.keys(italian).sort()).toEqual(required);
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

  it('provides Simplified Chinese wording for navigation, setup, appearance and errors', () => {
    expect(complete['nav.users']).toBe('用户');
    expect(complete['setup.identityTitle']).toBe('介绍一下您的企业');
    expect(complete['appearance.save']).toBe('保存更改');
    expect(complete['appearance.error.forbiddenSave']).toBe('您无权保存外观。');
  });
});
