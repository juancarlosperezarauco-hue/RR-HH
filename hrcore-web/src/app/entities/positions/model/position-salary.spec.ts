import { PositionRequest } from './positions.model';
import { compareSalaries, normalizeSalary, parsePositionJson, serializePosition } from './position-salary';

describe('Salarios de cargos: decimales exactos NUMERIC(19,4)', () => {
  it('conserva enteros y entre uno y cuatro decimales', () => {
    for (const value of ['0', '123', '1.1', '1.12', '1.123', '1.1234', '0.0001']) expect(normalizeSalary(value)).toBe(value);
    expect(normalizeSalary(' 0001.2300 ')).toBe('1.2300');
  });
  it('conserva el importe detectado y el límite máximo sin redondear', () => {
    for (const value of ['12345678901234.1234', '999999999999999', '999999999999999.9999']) expect(normalizeSalary(value)).toBe(value);
  });
  it('mantiene opcionales y vacíos como null', () => {
    expect(normalizeSalary(null)).toBeNull(); expect(normalizeSalary('')).toBeNull(); expect(normalizeSalary(' ')).toBeNull();
  });
  it('rechaza negativos, escala o rango excesivos y formatos inválidos', () => {
    for (const value of ['-1', '1.12345', '1000000000000000', '1e3', 'NaN', 'Infinity', '1,25', '1.', '.25', '1 2', '1,"code":"otro"']) {
      expect(() => normalizeSalary(value)).toThrowError();
    }
  });
  it('compara importes grandes con diferencia de una diezmilésima', () => {
    expect(compareSalaries('12345678901234.1234', '12345678901234.1233')).toBe(1);
    expect(compareSalaries('999999999999999.9998', '999999999999999.9999')).toBe(-1);
    expect(compareSalaries('9.9', '100')).toBe(-1);
    expect(compareSalaries('1.0', '1.0000')).toBe(0);
  });
  it('serializa números JSON exactos y escapa los demás campos', () => {
    const fields = { code: 'DEV', name: 'Cargo "especial"', description: 'Texto "salaryMin":123', responsibilities: null, organizationalLevel: 'OPERATIONAL' as const, currency: 'BOB' };
    const request: PositionRequest = { ...fields, salaryMin: '12345678901234.1234', salaryMax: '999999999999999.9999' };
    expect(serializePosition(request)).toBe(`${JSON.stringify(fields).slice(0, -1)},"salaryMin":12345678901234.1234,"salaryMax":999999999999999.9999}`);
    expect(serializePosition({ ...request, salaryMin: null, salaryMax: '' })).toContain('"salaryMin":null,"salaryMax":null');
  });
  it('lee salarios sin alterar textos, escapes, null ni números de paginación', () => {
    const description = 'Texto "salaryMin": 999 y una barra \\';
    const json = `{"data":{"content":[{"description":${JSON.stringify(description)},"salaryMin":12345678901234.1234,"salaryMax":999999999999999.9999},{"salaryMin":0,"salaryMax":null}],"number":2,"totalElements":10}}`;
    const result = parsePositionJson<{ data: { content: { description?: string; salaryMin: string; salaryMax: string | null }[]; number: number; totalElements: number } }>(json);
    expect(result.data.content[0].description).toBe(description);
    expect(result.data.content[0].salaryMin).toBe('12345678901234.1234'); expect(result.data.content[0].salaryMax).toBe('999999999999999.9999');
    expect(result.data.content[1].salaryMin).toBe('0'); expect(result.data.content[1].salaryMax).toBeNull();
    expect(result.data.number).toBe(2); expect(result.data.totalElements).toBe(10);
  });
  it('rechaza JSON mal formado en lugar de ocultar el error', () => {
    expect(() => parsePositionJson('{"salaryMin":01}')).toThrowError();
    expect(() => parsePositionJson('{"salaryMin":1.2')).toThrowError();
  });
});
