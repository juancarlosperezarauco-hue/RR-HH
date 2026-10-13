import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LeaveStatus } from '../../../../entities/absence';

@Component({
  selector: 'app-absence-status-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span
      class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold tracking-wide border shadow-xs transition-colors"
      [ngClass]="badgeClass"
    >
      <span class="w-1.5 h-1.5 rounded-full" [ngClass]="dotClass"></span>
      <span>{{ label }}</span>
    </span>
  `
})
export class AbsenceStatusBadgeComponent {
  @Input({ required: true }) status!: LeaveStatus;

  get label(): string {
    switch (this.status) {
      case 'PENDING':
        return 'Pendiente';
      case 'APPROVED':
        return 'Aprobada';
      case 'REJECTED':
        return 'Rechazada';
      case 'CANCELLED':
        return 'Cancelada';
      default:
        return this.status;
    }
  }

  get badgeClass(): string {
    switch (this.status) {
      case 'PENDING':
        return 'bg-amber-500/15 text-amber-800 dark:text-amber-400 border-amber-500/30';
      case 'APPROVED':
        return 'bg-emerald-500/10 text-emerald-700 dark:text-emerald-400 border-emerald-500/20';
      case 'REJECTED':
        return 'bg-rose-500/10 text-rose-700 dark:text-rose-400 border-rose-500/20';
      case 'CANCELLED':
        return 'bg-slate-500/10 text-slate-700 dark:text-slate-400 border-slate-500/20';
      default:
        return 'bg-muted text-muted-foreground border-border';
    }
  }

  get dotClass(): string {
    switch (this.status) {
      case 'PENDING':
        return 'bg-amber-500 animate-pulse';
      case 'APPROVED':
        return 'bg-emerald-500';
      case 'REJECTED':
        return 'bg-rose-500';
      case 'CANCELLED':
        return 'bg-slate-400';
      default:
        return 'bg-muted-foreground';
    }
  }
}
