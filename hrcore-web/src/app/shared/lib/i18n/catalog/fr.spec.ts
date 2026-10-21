import { tenantTextKeys, TenantTextKey } from '../keys';
import { es } from './es';
import { en } from './en';
import { ptBR } from './pt-BR';
import { fr } from './fr';

describe('French catalog', () => {
  const complete: Record<TenantTextKey, string> = fr;

  it('covers exactly the existing keys without empty values', () => {
    const required = [...tenantTextKeys].sort();
    expect(Object.keys(complete).sort()).toEqual(required);
    expect(Object.keys(es).sort()).toEqual(required);
    expect(Object.keys(en).sort()).toEqual(required);
    expect(Object.keys(ptBR).sort()).toEqual(required);
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

  it('provides French wording for navigation, setup, appearance and errors', () => {
    expect(complete['nav.home']).toBe('Accueil');
    expect(complete['setup.identityTitle']).toBe('Parlez-nous de votre entreprise');
    expect(complete['appearance.save']).toBe('Enregistrer les modifications');
    expect(complete['appearance.error.forbiddenSave']).toBe('Vous n’êtes pas autorisé à enregistrer l’apparence.');
  });
});
