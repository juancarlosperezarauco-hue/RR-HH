export type ColorMode = 'light' | 'dark';
export type ColorKey = 'primary' | 'secondary' | 'sidebar';

export interface AppearanceColors {
  primary: string;
  secondary: string;
  sidebar: string;
}

export interface AppearancePreset {
  id: string;
  name: string;
  description: string;
  colors: AppearanceColors;
}

export interface PaletteOption {
  id: string;
  name: string;
  description: string;
  colors: AppearanceColors;
}

// Original HRCore uses the current light-mode tokens in src/styles.css.
export const APPEARANCE_PRESETS: readonly AppearancePreset[] = [
  { id: 'original', name: 'Original HRCore', description: 'Azules actuales del sistema', colors: { primary: '#1268c4', secondary: '#13a8e8', sidebar: '#082d55' } },
  { id: 'corporate', name: 'Corporativo', description: 'Azul sobrio y preciso', colors: { primary: '#22468b', secondary: '#4383bb', sidebar: '#162b50' } },
  { id: 'modern', name: 'Moderno', description: 'Índigo y acento vibrante', colors: { primary: '#5948c8', secondary: '#14a6bc', sidebar: '#252052' } },
  { id: 'natural', name: 'Natural', description: 'Verdes tranquilos', colors: { primary: '#28734e', secondary: '#8c9f47', sidebar: '#183c32' } },
  { id: 'minimal', name: 'Minimalista', description: 'Neutros con un acento claro', colors: { primary: '#334e68', secondary: '#567c91', sidebar: '#1d2935' } },
  { id: 'warm', name: 'Cálido', description: 'Terracota y ámbar', colors: { primary: '#a34c32', secondary: '#ae7622', sidebar: '#57352f' } },
];

export const ORIGINAL_COLORS: AppearanceColors = { ...APPEARANCE_PRESETS[0].colors };
export const HEX_COLOR = /^#[0-9a-fA-F]{6}$/;

export function isHexColor(value: string): boolean {
  return HEX_COLOR.test(value);
}

export function normalizeHex(value: string): string {
  return value.toLowerCase();
}

function channels(hex: string): [number, number, number] {
  return [1, 3, 5].map(index => parseInt(hex.slice(index, index + 2), 16)) as [number, number, number];
}

function luminance(hex: string): number {
  const [red, green, blue] = channels(hex).map(value => {
    const normalized = value / 255;
    return normalized <= 0.04045 ? normalized / 12.92 : ((normalized + 0.055) / 1.055) ** 2.4;
  });
  return red * 0.2126 + green * 0.7152 + blue * 0.0722;
}

export function contrastRatio(first: string, second: string): number {
  const lighter = Math.max(luminance(first), luminance(second));
  const darker = Math.min(luminance(first), luminance(second));
  return (lighter + 0.05) / (darker + 0.05);
}

export function readableText(background: string): string {
  return contrastRatio(background, '#ffffff') >= contrastRatio(background, '#111827') ? '#ffffff' : '#111827';
}

export function readableAccent(color: string, background: string, foreground: string): string {
  if (contrastRatio(color, background) >= 4.5) return color;
  for (let step = 1; step <= 20; step++) {
    const candidate = mix(color, foreground, step / 20);
    if (contrastRatio(candidate, background) >= 4.5) return candidate;
  }
  return foreground;
}

export function mix(first: string, second: string, weight: number): string {
  const a = channels(first);
  const b = channels(second);
  return '#' + a.map((channel, index) => Math.round(channel * (1 - weight) + b[index] * weight)
    .toString(16).padStart(2, '0')).join('');
}

function hsl(hex: string): [number, number, number] {
  const [r, g, b] = channels(hex).map(channel => channel / 255);
  const max = Math.max(r, g, b);
  const min = Math.min(r, g, b);
  const delta = max - min;
  const lightness = (max + min) / 2;
  if (delta === 0) return [0, 0, lightness];
  const saturation = delta / (1 - Math.abs(2 * lightness - 1));
  let hue = max === r ? ((g - b) / delta) % 6 : max === g ? (b - r) / delta + 2 : (r - g) / delta + 4;
  hue = (hue * 60 + 360) % 360;
  return [hue, saturation, lightness];
}

function fromHsl(hue: number, saturation: number, lightness: number): string {
  const chroma = (1 - Math.abs(2 * lightness - 1)) * saturation;
  const sector = ((hue % 360) + 360) % 360 / 60;
  const intermediate = chroma * (1 - Math.abs(sector % 2 - 1));
  const [r, g, b] = sector < 1 ? [chroma, intermediate, 0] : sector < 2 ? [intermediate, chroma, 0]
    : sector < 3 ? [0, chroma, intermediate] : sector < 4 ? [0, intermediate, chroma]
    : sector < 5 ? [intermediate, 0, chroma] : [chroma, 0, intermediate];
  const offset = lightness - chroma / 2;
  return '#' + [r, g, b].map(value => Math.round((value + offset) * 255).toString(16).padStart(2, '0')).join('');
}

function accessibleMain(color: string): string {
  const [hue, saturation, lightness] = hsl(color);
  // Preserve neutral bases; keep other colors away from very pale or nearly black values.
  const adjustedSaturation = saturation < 0.08 ? saturation : Math.max(0.42, saturation);
  return fromHsl(hue, adjustedSaturation, Math.min(0.62, Math.max(0.30, lightness)));
}

export function generatePalettes(base: string): PaletteOption[] {
  if (!isHexColor(base)) return [];
  const primary = accessibleMain(normalizeHex(base));
  const [baseHue, saturation] = hsl(primary);
  const hue = saturation < 0.08 ? 210 : baseHue;
  const secondarySaturation = Math.max(0.40, Math.min(0.76, saturation));
  return [
    { id: 'harmonic', name: 'Armónica', description: 'Acento cercano al color base', colors: { primary, secondary: fromHsl(hue + 28, secondarySaturation, 0.48), sidebar: fromHsl(hue, 0.48, 0.19) } },
    { id: 'contrast', name: 'Contraste', description: 'Acento complementario equilibrado', colors: { primary, secondary: fromHsl(hue + 170, secondarySaturation, 0.42), sidebar: fromHsl(hue + 8, 0.42, 0.17) } },
    { id: 'calm', name: 'Serena', description: 'Acento suave y menú profundo', colors: { primary, secondary: fromHsl(hue - 32, 0.48, 0.45), sidebar: fromHsl(hue - 12, 0.30, 0.20) } },
    { id: 'vivid', name: 'Vibrante', description: 'Acento más expresivo', colors: { primary, secondary: fromHsl(hue + 88, 0.70, 0.39), sidebar: fromHsl(hue + 16, 0.58, 0.17) } },
  ];
}
