import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-shift-status-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span
      [class]="status === 'ACTIVE'
        ? 'bg-emerald-500/10 text-emerald-600 border-emerald-500/20'
        : 'bg-muted text-muted-foreground border-border'"
      class="inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-[11px] font-bold uppercase tracking-wider">
      <span
        [class]="status === 'ACTIVE' ? 'bg-emerald-500' : 'bg-muted-foreground'"
        class="h-1.5 w-1.5 rounded-full"></span>
      {{ status === 'ACTIVE' ? 'Activo' : 'Inactivo' }}
    </span>
  `
})
export class ShiftStatusBadgeComponent {
  @Input() status: 'ACTIVE' | 'INACTIVE' = 'ACTIVE';
}
