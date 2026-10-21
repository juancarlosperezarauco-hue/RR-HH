import { Component, computed, input } from '@angular/core';
import { columnLabel, ReportChart } from '../../application/report-presentation';

@Component({ selector: 'app-ai-report-chart', standalone: true, template: `
  <div class="overflow-x-auto rounded-md border bg-background p-3" tabindex="0" role="region" aria-label="Gráfico del reporte; la tabla está disponible en el selector de visualización">
    @if (chart().kind === 'bar') {
      <svg viewBox="0 0 640 400" [attr.viewBox]="'0 0 640 ' + barHeight()" class="min-w-[32rem] w-full text-primary" role="img" [attr.aria-label]="label(chart().metric) + ' por categoría'">
        <title>{{ label(chart().metric) }} por categoría</title><desc>Valores exactos del reporte. Usa la vista Tabla para leer todas las celdas.</desc>
        @for (point of chart().points; track $index) { <g>
          <text x="8" [attr.y]="$index * 38 + 24" fill="currentColor" class="text-xs">{{ shortened(point.label) }}</text>
          <rect x="230" [attr.y]="$index * 38 + 7" [attr.width]="point.value / maximum() * 310" height="25" rx="3" fill="currentColor"><title>{{ point.label }}: {{ point.value }}</title></rect>
          <text [attr.x]="242 + point.value / maximum() * 310" [attr.y]="$index * 38 + 24" fill="currentColor" class="text-xs">{{ point.value }}</text>
        </g> }
      </svg>
    } @else if (chart().kind === 'line') {
      <svg viewBox="0 0 640 280" class="min-w-[32rem] w-full text-primary" role="img" [attr.aria-label]="label(chart().metric) + ' por fecha'">
        <title>{{ label(chart().metric) }} por fecha</title><desc>Puntos ordenados por fecha y separados según el tiempo transcurrido. No se completan períodos ausentes.</desc>
        <line x1="60" y1="220" x2="580" y2="220" stroke="currentColor" stroke-opacity="0.3" />
        <text x="4" y="42" fill="currentColor" class="text-xs">{{ maximum() }}</text><text x="35" y="220" fill="currentColor" class="text-xs">0</text>
        <polyline [attr.points]="linePoints()" fill="none" stroke="currentColor" stroke-width="2" />
        @for (point of chart().points; track $index) { <circle [attr.cx]="lineX(point.timestamp!)" [attr.cy]="220 - point.value / maximum() * 180" r="4" fill="currentColor"><title>{{ point.label }}: {{ point.value }}</title></circle> }
        <text x="60" y="250" fill="currentColor" class="text-xs">{{ chart().points[0].label }}</text><text x="580" y="250" text-anchor="end" fill="currentColor" class="text-xs">{{ chart().points[chart().points.length - 1].label }}</text>
      </svg>
    }
    <p class="mt-2 text-xs text-muted-foreground">{{ label(chart().metric) }} · Valores devueltos por el reporte, sin sumas ni agrupaciones adicionales.</p>
  </div>` })
export class AiReportChartComponent {
  readonly chart = input.required<ReportChart>();
  readonly maximum = computed(() => Math.max(1, ...this.chart().points.map(point => point.value)));
  readonly barHeight = computed(() => Math.max(48, this.chart().points.length * 38 + 12));
  readonly linePoints = computed(() => this.chart().points.map(point => `${this.lineX(point.timestamp!)},${220 - point.value / this.maximum() * 180}`).join(' '));
  readonly label = columnLabel;
  shortened(value: string): string { return value.length > 29 ? `${value.slice(0, 26)}…` : value; }
  lineX(timestamp: number): number {
    const points = this.chart().points, first = points[0]?.timestamp ?? 0, last = points[points.length - 1]?.timestamp ?? 1;
    return 60 + (timestamp - first) / (last - first || 1) * 520;
  }
}
