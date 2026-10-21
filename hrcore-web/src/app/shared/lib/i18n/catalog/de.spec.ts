import { tenantTextKeys, TenantTextKey } from '../keys';
import { es } from './es';
import { en } from './en';
import { ptBR } from './pt-BR';
import { fr } from './fr';
import { de } from './de';

describe('German catalog', () => {
  const complete: Record<TenantTextKey, string> = de;

  it('covers exactly the existing keys with nonempty translations', () => {
    const required = [...tenantTextKeys].sort();
    expect(Object.keys(complete).sort()).toEqual(required);
    expect(Object.keys(es).sort()).toEqual(required);
    expect(Object.keys(en).sort()).toEqual(required);
    expect(Object.keys(ptBR).sort()).toEqual(required);
    expect(Object.keys(fr).sort()).toEqual(required);
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

  it('provides German wording for navigation, setup, appearance and errors', () => {
    expect(complete['nav.home']).toBe('Startseite');
    expect(complete['setup.identityTitle']).toBe('Erzählen Sie uns von Ihrem Unternehmen');
    expect(complete['appearance.save']).toBe('Änderungen speichern');
    expect(complete['appearance.error.forbiddenSave']).toBe('Sie sind nicht berechtigt, das Design zu speichern.');
  });
});
