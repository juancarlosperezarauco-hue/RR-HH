import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AttendanceStatus } from '../../../../entities/attendance';

@Component({
  selector: 'app-attendance-status-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span
      class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold tracking-wide border shadow-xs transition-colors"
      [ngClass]="badgeClass"
    >
      <span class="w-1.5 h-1.5 rounded-full" [ngClass]="dotClass"></span>
      <span>{{ label }}</span>
      <span *ngIf="status === 'LATE' && lateMinutes && lateMinutes > 0" class="text-[10px] font-mono font-bold opacity-90">
        (+{{ lateMinutes }}m)
      </span>
    </span>
  `
})
export class AttendanceStatusBadgeComponent {
  @Input({ required: true }) status!: AttendanceStatus;
  @Input() lateMinutes?: number;

  get label(): string {
    switch (this.status) {
      case 'PRESENT':
        return 'Presente';
      case 'LATE':
        return 'Retraso';
      case 'EARLY_DEPARTURE':
        return 'Salida Anticipada';
      case 'ABSENT':
        return 'Inasistencia';
      case 'EXCUSED':
      case 'JUSTIFIED':
        return 'Justificado';
      case 'HOLIDAY':
        return 'Feriado';
      case 'REST_DAY':
      case 'DAY_OFF':
        return 'Día de Descanso';
      case 'PENDING':
        return 'Pendiente';
      case 'INCOMPLETE':
        return 'Jornada Incompleta';
      default:
        return this.status;
    }
  }

  get badgeClass(): string {
    switch (this.status) {
      case 'PRESENT':
        return 'bg-emerald-500/10 text-emerald-700 dark:text-emerald-400 border-emerald-500/20';
      case 'LATE':
        return 'bg-amber-500/15 text-amber-800 dark:text-amber-400 border-amber-500/30';
      case 'EARLY_DEPARTURE':
        return 'bg-indigo-500/10 text-indigo-700 dark:text-indigo-400 border-indigo-500/20';
      case 'ABSENT':
        return 'bg-rose-500/10 text-rose-700 dark:text-rose-400 border-rose-500/20';
      case 'EXCUSED':
      case 'JUSTIFIED':
        return 'bg-sky-500/10 text-sky-700 dark:text-sky-400 border-sky-500/20';
      case 'HOLIDAY':
        return 'bg-teal-500/10 text-teal-700 dark:text-teal-400 border-teal-500/20';
      case 'REST_DAY':
      case 'DAY_OFF':
        return 'bg-slate-500/10 text-slate-700 dark:text-slate-400 border-slate-500/20';
      case 'PENDING':
        return 'bg-amber-500/10 text-amber-700 dark:text-amber-400 border-amber-500/20';
      case 'INCOMPLETE':
        return 'bg-purple-500/10 text-purple-700 dark:text-purple-400 border-purple-500/20';
      default:
        return 'bg-muted text-muted-foreground border-border';
    }
  }

  get dotClass(): string {
    switch (this.status) {
      case 'PRESENT':
        return 'bg-emerald-500 animate-pulse';
      case 'LATE':
        return 'bg-amber-500';
      case 'EARLY_DEPARTURE':
        return 'bg-indigo-500';
      case 'ABSENT':
        return 'bg-rose-500';
      case 'EXCUSED':
      case 'JUSTIFIED':
        return 'bg-sky-500';
      case 'HOLIDAY':
        return 'bg-teal-500';
      case 'REST_DAY':
      case 'DAY_OFF':
        return 'bg-slate-400';
      case 'PENDING':
        return 'bg-amber-400';
      case 'INCOMPLETE':
        return 'bg-purple-500';
      default:
        return 'bg-muted-foreground';
    }
  }
}
