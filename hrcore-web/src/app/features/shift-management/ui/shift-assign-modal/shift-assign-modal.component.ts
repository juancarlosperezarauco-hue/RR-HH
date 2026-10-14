import { Component, EventEmitter, inject, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
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
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm overflow-y-auto">
      <div
        class="relative w-full max-w-lg rounded-[24px] border border-border bg-card p-6 shadow-2xl space-y-5 my-8">
        
        <!-- Header -->
        <div class="flex items-center justify-between border-b border-border pb-4">
          <div>
            <h3 class="text-xl font-bold text-accent-foreground">Asignar Turno de Trabajo</h3>
            <p class="text-xs text-muted-foreground mt-0.5">
              Turno: <span class="font-bold text-foreground">{{ shift.name }}</span> ({{ shift.code }})
            </p>
          </div>
          <button
            type="button"
            (click)="close()"
            class="inline-flex h-8 w-8 cursor-pointer items-center justify-center rounded-full border border-border bg-card text-muted-foreground hover:bg-destructive/10 hover:text-destructive">
            <lucide-icon name="x" [size]="16"></lucide-icon>
          </button>
        </div>

        <!-- Formulario -->
        <form (ngSubmit)="submit()" class="space-y-4">
          <!-- Selección de Colaboradores -->
          <div>
            <label class="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
              Colaboradores a Asignar *
            </label>
            <div *ngIf="availableEmployees.length === 0" class="rounded-xl border border-dashed border-border p-6 text-center text-xs text-muted-foreground bg-muted/20">
              <p class="font-bold text-accent-foreground">No hay colaboradores registrados</p>
              <p class="mt-0.5">No se encontraron empleados activos en la base de datos para asignar turnos.</p>
            </div>
            <div *ngIf="availableEmployees.length > 0" class="space-y-2 max-h-40 overflow-y-auto rounded-xl border border-border bg-muted/20 p-2.5">
              <label
                *ngFor="let emp of availableEmployees"
                class="flex items-center gap-3 rounded-lg p-2 hover:bg-muted/40 cursor-pointer">
                <input
                  type="checkbox"
                  [checked]="isSelected(emp.id)"
                  (change)="toggleEmployee(emp.id)"
                  class="rounded border-border text-primary focus:ring-primary h-4 w-4" />
                <div class="min-w-0 flex-1">
                  <p class="text-xs font-bold text-accent-foreground">{{ emp.name }}</p>
                  <p class="text-[11px] text-muted-foreground">{{ emp.code }} • {{ emp.position }}</p>
                </div>
              </label>
            </div>
            <p *ngIf="availableEmployees.length > 0" class="text-[11px] text-muted-foreground mt-1">
              {{ selectedEmployeeIds.length }} colaboradores seleccionados
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
                name="startDate"
                required
                class="w-full rounded-xl border border-border bg-muted/40 px-3.5 py-2 text-sm text-foreground outline-none focus:border-primary focus:ring-1 focus:ring-primary" />
            </div>

            <div>
              <label class="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                Fecha Fin (Opcional)
              </label>
              <input
                type="date"
                [(ngModel)]="endDate"
                name="endDate"
                class="w-full rounded-xl border border-border bg-muted/40 px-3.5 py-2 text-sm text-foreground outline-none focus:border-primary focus:ring-1 focus:ring-primary" />
            </div>
          </div>

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

          <!-- Días de Aplicación de la Asignación (HU16: weekdays short array) -->
          <app-shift-days-picker
            [selectedDays]="weekdays"
            (daysChange)="weekdays = $event">
          </app-shift-days-picker>

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

          <!-- Botones de Acción -->
          <div class="flex items-center justify-end gap-3 pt-3 border-t border-border">
            <button
              type="button"
              (click)="close()"
              class="rounded-full border border-border bg-card px-4 py-2 text-xs font-semibold text-muted-foreground hover:bg-accent hover:text-foreground cursor-pointer">
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
export class ShiftAssignModalComponent implements OnInit, OnChanges {
  @Input() isOpen = false;
  @Input() shift: ShiftResponse | null = null;
  @Output() closeModal = new EventEmitter<void>();
  @Output() assigned = new EventEmitter<void>();

  private readonly http = inject(HttpClient);
  readonly assignmentUseCase = inject(ShiftAssignmentUseCase);

  availableEmployees: EmployeeSelectOption[] = [];
  selectedEmployeeIds: string[] = [];
  startDate = new Date().toISOString().substring(0, 10);
  endDate = '';
  isRecurring = true;
  weekdays: number[] = [1, 2, 3, 4, 5];

  ngOnInit(): void {
    this.loadEmployees();
  }

  async loadEmployees(): Promise<void> {
    try {
      const res = await firstValueFrom(
        this.http.get<any>(`${environment.apiUrl}/api/employees?size=50`)
      );
      const list = res?.data?.content || [];
      this.availableEmployees = list.map((e: any) => ({
        id: e.id,
        name: `${e.firstName} ${e.lastName}`,
        code: e.employeeNumber,
        position: 'Colaborador'
      }));
      if (this.availableEmployees.length > 0 && this.selectedEmployeeIds.length === 0) {
        this.selectedEmployeeIds = [this.availableEmployees[0].id];
      }
    } catch {
      this.availableEmployees = [];
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen']?.currentValue === true) {
      this.loadEmployees();
    }
    if (changes['shift'] && this.shift) {
      if (this.shift.days && this.shift.days.length > 0) {
        this.weekdays = this.shift.days.filter((d) => d.isWorkDay).map((d) => d.dayOfWeek);
      } else {
        this.weekdays = [1, 2, 3, 4, 5];
      }
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
    this.closeModal.emit();
  }
}
