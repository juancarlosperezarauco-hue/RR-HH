import { PositionRequest } from './positions.model';

/** NUMERIC(19,4), without converting decimal values to JavaScript numbers. */
export function normalizeSalary(value: string | null): string | null {
  if (value === null || value.trim() === '') return null;
  const match = /^(\d+)(?:\.(\d{1,4}))?$/.exec(value.trim());
  if (!match) throw new Error('Los salarios deben ser no negativos, menores a 10¹⁵ y con hasta cuatro decimales.');
  const whole = match[1].replace(/^0+(?=\d)/, '');
  if (whole.length > 15) throw new Error('Los salarios deben ser no negativos, menores a 10¹⁵ y con hasta cuatro decimales.');
  return whole + (match[2] === undefined ? '' : `.${match[2]}`);
}

export function compareSalaries(min: string, max: string): number {
  const units = (value: string) => {
    const [whole, fraction = ''] = normalizeSalary(value)!.split('.');
    return BigInt(whole) * 10000n + BigInt(fraction.padEnd(4, '0'));
  };
  const a = units(min), b = units(max);
  return a < b ? -1 : a > b ? 1 : 0;
}

/** HttpClient accepts a serialized JSON body; only validated decimal tokens are unquoted. */
export function serializePosition(request: PositionRequest): string {
  const { salaryMin, salaryMax, ...fields } = request;
  const json = JSON.stringify(fields);
  return `${json.slice(0, -1)},"salaryMin":${normalizeSalary(salaryMin) ?? 'null'},"salaryMax":${normalizeSalary(salaryMax) ?? 'null'}}`;
}

/** Protect salary tokens before JSON.parse, including values in paginated responses.
 * Scanning complete string tokens keeps descriptions and escaped quotes untouched.
 */
export function parsePositionJson<T>(json: string): T {
  const strings = /"(?:[^"\\]|\\.)*"/g;
  const numberValue = /^(\s*:\s*)(-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?)(?=\s*[,}])/;
  let result = '', cursor = 0;
  for (let token = strings.exec(json); token; token = strings.exec(json)) {
    result += json.slice(cursor, strings.lastIndex);
    cursor = strings.lastIndex;
    const key: unknown = JSON.parse(token[0]);
    if (key !== 'salaryMin' && key !== 'salaryMax') continue;
    const value = numberValue.exec(json.slice(cursor));
    if (!value) continue;
    result += value[1] + JSON.stringify(value[2]);
    cursor += value[0].length;
    strings.lastIndex = cursor;
  }
  return JSON.parse(result + json.slice(cursor)) as T;
}
