import { Component, EventEmitter, inject, Input, OnChanges, OnDestroy, OnInit, Output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { LucideAngularModule } from 'lucide-angular';
import { ShiftResponse } from '../../../../entities/shift';
import { ShiftAssignmentUseCase } from '../../application/shift-assignment.usecase';
import { ShiftDaysPickerComponent } from '../shift-days-picker/shift-days-picker.component';
import { environment } from '../../../../../environments/environment';

export interface EmployeeSelectOption {
  id: string;
  name: string;
  code: string;
  position: string;
}

@Component({
  selector: 'app-shift-assign-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule, ShiftDaysPickerComponent],
  template: `
    <div
      *ngIf="isOpen && shift"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm overflow-hidden">
      <div
        class="relative flex flex-col w-full max-w-lg max-h-[90vh] rounded-[24px] border border-border bg-card shadow-2xl overflow-hidden">
        
        <!-- Header (Fixed) -->
        <div class="flex items-center justify-between border-b border-border p-6 pb-4 shrink-0 bg-card">
          <div>
            <h3 class="text-xl font-bold text-accent-foreground">Asignar Turno de Trabajo</h3>
            <p class="text-xs text-muted-foreground mt-0.5">
              Turno: <span class="font-bold text-foreground">{{ shift.name }}</span> ({{ shift.code }})
            </p>
          </div>
          <button
            type="button"
            (click)="close()"
            class="inline-flex h-8 w-8 cursor-pointer items-center justify-center rounded-full border border-border bg-card text-muted-foreground hover:bg-destructive/10 hover:text-destructive transition-colors">
            <lucide-icon name="x" [size]="16"></lucide-icon>
          </button>
        </div>

        <!-- Formulario con Body Scrollable y Footer Fijo -->
        <form (ngSubmit)="submit()" class="flex flex-col flex-1 overflow-hidden">
          <div class="flex-1 overflow-y-auto p-6 space-y-4">

            <!-- Banner Informativo del Turno Configurado -->
            <div class="rounded-2xl border border-primary/20 bg-primary/5 p-3.5 space-y-1.5">
              <div class="flex items-center justify-between text-xs">
                <span class="font-bold text-accent-foreground flex items-center gap-1.5">
                  <lucide-icon name="clock" [size]="14" class="text-primary"></lucide-icon>
                  Horario: {{ shift.startTime.substring(0, 5) }} a {{ shift.endTime.substring(0, 5) }}
                </span>
                <span *ngIf="shift.crossesMidnight" class="rounded-full bg-indigo-500/10 px-2 py-0.5 text-[10px] font-bold text-indigo-600 border border-indigo-500/20">
                  Cruza medianoche (+1 día)
                </span>
              </div>
              <div class="flex items-center gap-1.5 text-xs text-muted-foreground">
                <lucide-icon name="calendar" [size]="14" class="text-primary shrink-0"></lucide-icon>
                <span>
                  Jornada configurada para este turno: <strong class="text-foreground">{{ shiftScheduleDescription }}</strong>
                </span>
              </div>
            </div>

            <!-- Selección de Colaboradores -->
            <div>
              <div class="flex items-center justify-between mb-1.5">
                <label class="block text-xs font-bold uppercase tracking-wider text-muted-foreground">
                  Colaboradores a Asignar *
                </label>
                <button
                  *ngIf="availableEmployees.length > 0"
                  type="button"
                  (click)="toggleAllEmployees()"
                  class="text-xs font-semibold text-primary hover:underline cursor-pointer">
                  {{ selectedEmployeeIds.length === availableEmployees.length ? 'Deseleccionar todos' : 'Seleccionar todos' }}
                </button>
              </div>

              <div *ngIf="availableEmployees.length === 0" class="rounded-xl border border-dashed border-border p-6 text-center text-xs text-muted-foreground bg-muted/20">
                <p class="font-bold text-accent-foreground">No hay colaboradores disponibles</p>
                <p class="mt-0.5">No se encontraron empleados activos para asignar turnos.</p>
              </div>

              <div *ngIf="availableEmployees.length > 0" class="space-y-1.5 max-h-44 overflow-y-auto rounded-xl border border-border bg-muted/20 p-2">
                <label
                  *ngFor="let emp of availableEmployees"
                  class="flex items-center gap-3 rounded-lg p-2 hover:bg-muted/50 cursor-pointer transition-colors"
                  [class.bg-primary/5]="isSelected(emp.id)">
                  <input
                    type="checkbox"
                    [checked]="isSelected(emp.id)"
                    (change)="toggleEmployee(emp.id)"
                    class="rounded border-border text-primary focus:ring-primary h-4 w-4 cursor-pointer" />
                  <div class="min-w-0 flex-1">
                    <p class="text-xs font-bold text-accent-foreground truncate">{{ emp.name }}</p>
                    <p class="text-[11px] text-muted-foreground truncate">{{ emp.code }} • {{ emp.position }}</p>
                  </div>
                </label>
              </div>
              <p *ngIf="availableEmployees.length > 0" class="text-[11px] text-muted-foreground mt-1">
                {{ selectedEmployeeIds.length }} colaborador(es) seleccionado(s)
              </p>
            </div>

            <!-- Rango de Fechas -->
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label class="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                  Fecha Inicio *
                </label>
                <input
                  type="date"
                  [(ngModel)]="startDate"
                  [min]="minStartDate"
                  name="startDate"
                  required
                  class="w-full rounded-xl border border-border bg-muted/40 px-3.5 py-2 text-sm text-foreground outline-none focus:border-primary focus:ring-1 focus:ring-primary" />
                <p *ngIf="startDatePastError" class="text-xs font-semibold text-destructive mt-1">
                  {{ startDatePastError }}
                </p>
              </div>

              <div>
                <label class="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                  Fecha Fin (Opcional)
                </label>
                <input
                  type="date"
                  [(ngModel)]="endDate"
                  [min]="startDate || minStartDate"
                  name="endDate"
                  class="w-full rounded-xl border border-border bg-muted/40 px-3.5 py-2 text-sm text-foreground outline-none focus:border-primary focus:ring-1 focus:ring-primary" />
              </div>
            </div>

            <p *ngIf="dateRangeError" class="text-xs font-semibold text-destructive">
              {{ dateRangeError }}
            </p>

            <!-- Switch Recurrente Semanal -->
            <div class="flex items-center justify-between rounded-xl border border-border bg-muted/20 p-3">
              <div class="space-y-0.5 pr-2">
                <p class="text-xs font-bold text-accent-foreground">Asignación Recurrente</p>
                <p class="text-[11px] text-muted-foreground">Repite automáticamente cada semana según los días laborales configurados.</p>
              </div>
              <input
                type="checkbox"
                [(ngModel)]="isRecurring"
                name="isRecurring"
                class="rounded border-border text-primary focus:ring-primary h-4 w-4 cursor-pointer" />
            </div>

            <!-- Días de Aplicación de la Asignación (Filtrados a los días del turno) -->
            <div>
              <app-shift-days-picker
                [allowedDays]="shiftWorkDays"
                [selectedDays]="weekdays"
                (daysChange)="weekdays = $event">
              </app-shift-days-picker>
            </div>

            <!-- Alerta de Solapamiento (Escenario 5 BDD) -->
            <div
              *ngIf="overlapError"
              class="flex items-start gap-2.5 rounded-xl border border-destructive/30 bg-destructive/10 p-3 text-xs font-medium text-destructive">
              <lucide-icon name="circle-alert" [size]="16" class="shrink-0 mt-0.5"></lucide-icon>
              <div>
                <p class="font-bold">Conflicto de Solapamiento Detectado</p>
                <p class="mt-0.5">{{ overlapError }}</p>
              </div>
            </div>
          </div>

          <!-- Botones de Acción (Footer Fijo) -->
          <div class="flex items-center justify-end gap-3 p-6 pt-4 border-t border-border shrink-0 bg-card">
            <button
              type="button"
              (click)="close()"
              class="rounded-full border border-border bg-card px-4 py-2 text-xs font-semibold text-muted-foreground hover:bg-accent hover:text-foreground cursor-pointer transition-colors">
              Cancelar
            </button>
            <button
              type="submit"
              [disabled]="!isValid() || assignmentUseCase.assigning()"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full bg-primary px-5 py-2 text-xs font-semibold text-primary-foreground shadow-md transition-all hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed">
              <lucide-icon
                *ngIf="assignmentUseCase.assigning()"
                name="loader-circle"
                [size]="14"
                class="animate-spin"></lucide-icon>
              {{ assignmentUseCase.assigning() ? 'Asignando...' : 'Confirmar Asignación' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  `
})
export class ShiftAssignModalComponent implements OnInit, OnChanges, OnDestroy {
  @Input() isOpen = false;
  @Input() shift: ShiftResponse | null = null;
  @Output() closeModal = new EventEmitter<void>();
  @Output() assigned = new EventEmitter<void>();

  private readonly http = inject(HttpClient);
  readonly assignmentUseCase = inject(ShiftAssignmentUseCase);

  availableEmployees: EmployeeSelectOption[] = [];
  selectedEmployeeIds: string[] = [];
  minStartDate = this.getTodayLocalDate();
  startDate = this.getTodayLocalDate();
  endDate = '';
  isRecurring = true;
  weekdays: number[] = [1, 2, 3, 4, 5];

  private getTodayLocalDate(): string {
    const d = new Date();
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  get shiftWorkDays(): number[] {
    if (!this.shift) return [1, 2, 3, 4, 5];

    if (this.shift.days && this.shift.days.length > 0) {
      const activeDays = this.shift.days.filter((d) => d.isWorkDay).map((d) => d.dayOfWeek);
      if (activeDays.length > 0) {
        return activeDays;
      }
    }

    if (this.shift.description) {
      try {
        const parsed = JSON.parse(this.shift.description);
        if (Array.isArray(parsed.workDays) && parsed.workDays.length > 0) {
          return parsed.workDays;
        }
      } catch {
        // description is plain text
      }
    }

    // Por defecto en LGT Bolivia: Lunes a Viernes
    return [1, 2, 3, 4, 5];
  }

  get shiftScheduleDescription(): string {
    const days = this.shiftWorkDays;
    const dayNames: Record<number, string> = {
      1: 'Lunes',
      2: 'Martes',
      3: 'Miércoles',
      4: 'Jueves',
      5: 'Viernes',
      6: 'Sábado',
      7: 'Domingo'
    };

    const isLtoV = days.length === 5 && [1, 2, 3, 4, 5].every((d) => days.includes(d));
    const isLtoS = days.length === 6 && [1, 2, 3, 4, 5, 6].every((d) => days.includes(d));
    const isAll = days.length === 7;

    if (isLtoV) return 'Lunes a Viernes';
    if (isLtoS) return 'Lunes a Sábado';
    if (isAll) return 'Lunes a Domingo';

    return days.map((d) => dayNames[d] || `Día ${d}`).join(', ');
  }

  ngOnInit(): void {
    this.loadEmployees();
  }

  ngOnDestroy(): void {
    if (typeof document !== 'undefined') {
      document.body.style.overflow = '';
    }
  }

  async loadEmployees(): Promise<void> {
    try {
      const res = await firstValueFrom(
        this.http.get<any>(`${environment.apiUrl}/api/employees?size=100`)
      );
      const list = res?.data?.content || [];
      this.availableEmployees = list
        .filter((e: any) => !e.employmentStatus || e.employmentStatus === 'ACTIVE')
        .map((e: any) => ({
          id: e.id,
          name: `${e.firstName} ${e.lastName}`,
          code: e.employeeNumber,
          position: e.positionTitle || 'Colaborador'
        }));
      if (this.availableEmployees.length > 0 && this.selectedEmployeeIds.length === 0) {
        this.selectedEmployeeIds = [this.availableEmployees[0].id];
      }
    } catch {
      this.availableEmployees = [];
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen']) {
      if (typeof document !== 'undefined') {
        document.body.style.overflow = this.isOpen ? 'hidden' : '';
      }
      if (this.isOpen) {
        this.minStartDate = this.getTodayLocalDate();
        this.startDate = this.getTodayLocalDate();
        this.endDate = '';
        this.weekdays = [...this.shiftWorkDays];
        this.loadEmployees();
      }
    }
    if (changes['shift'] && this.shift) {
      this.weekdays = [...this.shiftWorkDays];
    }
  }

  isSelected(id: string): boolean {
    return this.selectedEmployeeIds.includes(id);
  }

  toggleEmployee(id: string): void {
    if (this.isSelected(id)) {
      this.selectedEmployeeIds = this.selectedEmployeeIds.filter((e) => e !== id);
    } else {
      this.selectedEmployeeIds = [...this.selectedEmployeeIds, id];
    }
  }

  toggleAllEmployees(): void {
    if (this.selectedEmployeeIds.length === this.availableEmployees.length) {
      this.selectedEmployeeIds = [];
    } else {
      this.selectedEmployeeIds = this.availableEmployees.map((e) => e.id);
    }
  }

  get startDatePastError(): string | null {
    if (this.startDate && this.startDate < this.minStartDate) {
      return 'La fecha de inicio no puede ser anterior a la fecha de hoy.';
    }
    return null;
  }

  get dateRangeError(): string | null {
    if (this.startDate && this.endDate && this.endDate < this.startDate) {
      return 'La fecha de fin no puede ser anterior a la fecha de inicio.';
    }
    return null;
  }

  get overlapError(): string | null {
    if (!this.startDate) return null;
    for (const empId of this.selectedEmployeeIds) {
      const check = this.assignmentUseCase.validateOverlap(empId, this.startDate, this.endDate);
      if (check.hasOverlap) {
        return check.message || 'Solapamiento de turno detectado';
      }
    }
    return null;
  }

  isValid(): boolean {
    return (
      !!this.shift &&
      this.selectedEmployeeIds.length > 0 &&
      !!this.startDate &&
      !this.startDatePastError &&
      !this.dateRangeError &&
      this.weekdays.length > 0 &&
      !this.overlapError
    );
  }

  async submit(): Promise<void> {
    if (!this.isValid()) return;

    const ok = await this.assignmentUseCase.assign({
      shiftId: this.shift!.id,
      employeeIds: this.selectedEmployeeIds,
      startDate: this.startDate,
      endDate: this.endDate || null,
      isRecurring: this.isRecurring,
      weekdays: this.weekdays
    });

    if (ok) {
      this.assigned.emit();
      this.close();
    }
  }

  close(): void {
    if (typeof document !== 'undefined') {
      document.body.style.overflow = '';
    }
    this.closeModal.emit();
  }
}
