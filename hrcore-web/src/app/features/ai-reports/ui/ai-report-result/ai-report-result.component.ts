import { CommonModule } from '@angular/common';
import { Component, computed, input, output, signal } from '@angular/core';
import { AiReportView } from '../../../../entities/reports/model/ai-conversation.model';
import { columnLabel, reportChart } from '../../application/report-presentation';
import { AiReportTableComponent } from '../ai-report-table/ai-report-table.component';
import { AiReportChartComponent } from '../ai-report-chart/ai-report-chart.component';

@Component({ selector: 'app-ai-report-result', standalone: true,
  imports: [CommonModule, AiReportTableComponent, AiReportChartComponent], templateUrl: './ai-report-result.component.html' })
export class AiReportResultComponent {
  readonly view = input.required<AiReportView>();
  readonly busy = input(false);
  readonly blocked = input(false);
  readonly refresh = output<void>();
  readonly pageChange = output<number>();
  readonly display = signal<'table' | 'bar' | 'line'>('table');
  readonly metric = signal('');
  readonly chart = computed(() => reportChart(this.view().result, this.metric()));
  readonly activeDisplay = computed(() => this.display() === this.chart().kind ? this.display() : 'table');
  readonly visualizationReason = computed(() => this.chart().reason || (this.display() !== 'table' && this.display() !== this.chart().kind
    ? 'La visualización elegida no corresponde a estos datos actualizados. Se muestra la tabla.' : ''));
  readonly label = columnLabel;
  warningLabel(value: string): string {
    return value === 'MISSING_PRIMARY_ASSIGNMENT'
      ? 'Hay empleados sin asignación principal vigente; el departamento o cargo puede aparecer vacío.' : value;
  }
  selectMetric(event: Event): void { this.metric.set((event.target as HTMLSelectElement).value); }
}
