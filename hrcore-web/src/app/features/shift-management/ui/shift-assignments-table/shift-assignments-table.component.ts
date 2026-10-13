import { Component, Input } from '@angular/core';
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
              <th class="px-5 py-3">Fecha Inicio</th>
              <th class="px-5 py-3">Fecha Fin</th>
              <th class="px-5 py-3">Recurrencia</th>
              <th class="px-5 py-3">Estado</th>
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
              <td class="px-5 py-3.5 text-foreground">{{ asg.startDate }}</td>
              <td class="px-5 py-3.5 text-muted-foreground">
                {{ asg.endDate || 'Indefinido' }}
              </td>
              <td class="px-5 py-3.5">
                <span class="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-bold bg-muted text-muted-foreground">
                  {{ asg.isRecurring ? 'Semanal' : 'Única' }}
                </span>
              </td>
              <td class="px-5 py-3.5">
                <span class="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-bold bg-emerald-500/10 text-emerald-600">
                  <span class="h-1.5 w-1.5 rounded-full bg-emerald-500"></span>
                  {{ asg.status }}
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
}
