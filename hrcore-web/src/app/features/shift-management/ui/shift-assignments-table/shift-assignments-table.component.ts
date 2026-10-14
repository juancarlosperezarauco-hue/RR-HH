import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule } from 'lucide-angular';
import { EmployeeShiftAssignmentResponse } from '../../../../entities/shift';

@Component({
  selector: 'app-shift-assignments-table',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  template: `
    <div class="rounded-2xl border border-border bg-card overflow-hidden shadow-sm">
      <div class="p-4 border-b border-border flex items-center justify-between">
        <div>
          <h3 class="text-base font-bold text-accent-foreground">Asignaciones Activas por Colaborador</h3>
          <p class="text-xs text-muted-foreground mt-0.5">Control de cobertura y prevención de solapamientos de horario.</p>
        </div>
        <span class="inline-flex items-center gap-1.5 rounded-full bg-primary/10 px-3 py-1 text-xs font-semibold text-primary">
          <lucide-icon name="shield-check" [size]="14"></lucide-icon>
          Solapamientos Validados
        </span>
      </div>

      <div class="overflow-x-auto">
        <table class="w-full text-left text-xs">
          <thead class="bg-muted/40 uppercase tracking-wider text-[11px] font-bold text-muted-foreground border-b border-border">
            <tr>
              <th class="px-5 py-3">Colaborador</th>
              <th class="px-5 py-3">Turno Asignado</th>
              <th class="px-5 py-3">Vigencia</th>
              <th class="px-5 py-3">Días</th>
              <th class="px-5 py-3">Recurrencia</th>
              <th class="px-5 py-3">Estado</th>
              <th class="px-5 py-3 text-right">Acciones</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-border/60 font-medium">
            <tr *ngFor="let asg of assignments" class="hover:bg-muted/20 transition-colors">
              <td class="px-5 py-3.5">
                <p class="font-bold text-foreground">{{ asg.employeeFullName || 'Colaborador' }}</p>
                <p class="text-[11px] text-muted-foreground">{{ asg.employeeCode || asg.employeeId }}</p>
              </td>
              <td class="px-5 py-3.5 font-semibold text-primary">
                {{ asg.shiftName || 'Turno Registrado' }}
              </td>
              <td class="px-5 py-3.5">
                <span class="text-foreground font-semibold">{{ asg.startDate }}</span>
                <span class="text-muted-foreground"> a {{ asg.endDate || 'Indefinido' }}</span>
              </td>
              <td class="px-5 py-3.5">
                <div *ngIf="asg.weekdays && asg.weekdays.length > 0; else defaultDaysTpl" class="flex flex-wrap gap-1">
                  <span
                    *ngFor="let d of asg.weekdays"
                    class="rounded bg-muted px-1.5 py-0.5 text-[10px] font-bold text-foreground">
                    {{ getDayLabel(d) }}
                  </span>
                </div>
                <ng-template #defaultDaysTpl>
                  <span class="text-[11px] text-muted-foreground">L-V</span>
                </ng-template>
              </td>
              <td class="px-5 py-3.5">
                <span class="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-bold bg-muted text-muted-foreground">
                  {{ asg.isRecurring ? 'Semanal' : 'Única' }}
                </span>
              </td>
              <td class="px-5 py-3.5">
                <span
                  [class]="getStatusBadgeClass(asg.status)"
                  class="inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-[10px] font-bold">
                  <span class="h-1.5 w-1.5 rounded-full" [class]="getStatusDotClass(asg.status)"></span>
                  {{ asg.status }}
                </span>
              </td>
              <td class="px-5 py-3.5 text-right">
                <div *ngIf="asg.status === 'ACTIVE'" class="inline-flex items-center gap-1.5">
                  <button
                    type="button"
                    (click)="onClose(asg.id)"
                    class="cursor-pointer rounded-lg border border-border px-2.5 py-1 text-[11px] font-semibold text-muted-foreground hover:bg-accent hover:text-foreground transition-colors"
                    title="Cerrar asignación por fin de ciclo">
                    Cerrar
                  </button>
                  <button
                    type="button"
                    (click)="onCancel(asg.id)"
                    class="cursor-pointer rounded-lg border border-border px-2.5 py-1 text-[11px] font-semibold text-destructive hover:bg-destructive/10 transition-colors"
                    title="Cancelar asignación">
                    Cancelar
                  </button>
                </div>
                <span *ngIf="asg.status !== 'ACTIVE'" class="text-[11px] text-muted-foreground italic">
                  Finalizado
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  `
})
export class ShiftAssignmentsTableComponent {
  @Input() assignments: EmployeeShiftAssignmentResponse[] = [];
  @Output() cancel = new EventEmitter<string>();
  @Output() close = new EventEmitter<string>();

  private readonly dayLabels: Record<number, string> = {
    1: 'Lun',
    2: 'Mar',
    3: 'Mié',
    4: 'Jue',
    5: 'Vie',
    6: 'Sáb',
    7: 'Dom'
  };

  getDayLabel(dayNumber: number): string {
    return this.dayLabels[dayNumber] || `D${dayNumber}`;
  }

  getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'ACTIVE':
        return 'bg-emerald-500/10 text-emerald-600 border border-emerald-500/20';
      case 'CLOSED':
        return 'bg-muted text-muted-foreground border border-border';
      case 'CANCELLED':
        return 'bg-rose-500/10 text-rose-600 border border-rose-500/20';
      default:
        return 'bg-muted text-muted-foreground border border-border';
    }
  }

  getStatusDotClass(status: string): string {
    switch (status) {
      case 'ACTIVE':
        return 'bg-emerald-500';
      case 'CLOSED':
        return 'bg-muted-foreground';
      case 'CANCELLED':
        return 'bg-rose-500';
      default:
        return 'bg-muted-foreground';
    }
  }

  onCancel(id: string): void {
    if (confirm('¿Está seguro de cancelar esta asignación de turno laboral?')) {
      this.cancel.emit(id);
    }
  }

  onClose(id: string): void {
    if (confirm('¿Desea cerrar esta asignación de turno marcando la vigencia como concluida?')) {
      this.close.emit(id);
    }
  }
}
