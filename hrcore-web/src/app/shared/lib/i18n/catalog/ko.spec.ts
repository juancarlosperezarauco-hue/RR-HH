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

describe('Korean catalog', () => {
  const complete: Record<TenantTextKey, string> = ko;

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

  it('provides Korean wording for navigation, setup, appearance and errors', () => {
    expect(complete['nav.users']).toBe('사용자');
    expect(complete['setup.identityTitle']).toBe('회사를 소개해 주세요');
    expect(complete['appearance.save']).toBe('변경 사항 저장');
    expect(complete['appearance.error.forbiddenSave']).toBe('화면 설정을 저장할 권한이 없습니다.');
  });
});
