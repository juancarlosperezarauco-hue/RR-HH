import { tenantTextKeys, TenantTextKey } from '../keys';
import { es } from './es';
import { en } from './en';
import { ptBR } from './pt-BR';

describe('Brazilian Portuguese catalog', () => {
  const complete: Record<TenantTextKey, string> = ptBR;

  it('covers exactly the existing Spanish and English keys with nonempty texts', () => {
    const required = [...tenantTextKeys].sort();
    expect(Object.keys(complete).sort()).toEqual(required);
    expect(Object.keys(es).sort()).toEqual(required);
    expect(Object.keys(en).sort()).toEqual(required);
    expect(new Set(tenantTextKeys).size).toBe(required.length);
    expect(Object.values(complete).every(value => value.trim().length > 0)).toBeTrue();
  });

  it('keeps interpolation parameters compatible with Spanish', () => {
    const parameters = (value: string) => [...value.matchAll(/\{[a-zA-Z][a-zA-Z0-9_]*\}/g)]
      .map(match => match[0]).sort();
    for (const key of tenantTextKeys) {
      expect(parameters(complete[key])).withContext(key).toEqual(parameters(es[key]));
    }
  });

  it('uses Portuguese text for tenant navigation, setup, appearance and errors', () => {
    expect(complete['nav.home']).toBe('Início');
    expect(complete['setup.identityTitle']).toBe('Conte-nos sobre sua empresa');
    expect(complete['appearance.save']).toBe('Salvar alterações');
    expect(complete['appearance.error.forbiddenSave']).toBe('Você não tem permissão para salvar a aparência.');
  });
});
