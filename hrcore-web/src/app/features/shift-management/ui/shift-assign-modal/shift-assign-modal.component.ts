import { Component, EventEmitter, inject, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { LucideAngularModule } from 'lucide-angular';
import { ShiftResponse } from '../../../../entities/shift';
import { EmployeesService } from '../../../../entities/employees';
import { ShiftAssignmentUseCase } from '../../application/shift-assignment.usecase';
import { ShiftDaysPickerComponent } from '../shift-days-picker/shift-days-picker.component';

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
      class="app-modal-overlay fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm">
      <div
        class="app-modal-panel relative flex max-h-[92vh] w-full max-w-lg flex-col overflow-hidden rounded-[24px] border border-border bg-card shadow-2xl">
        
        <!-- Header fijo -->
        <div class="flex shrink-0 items-center justify-between border-b border-border px-6 py-4">
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
        <form (ngSubmit)="submit()" novalidate class="flex min-h-0 flex-1 flex-col overflow-hidden">
          <div class="flex-1 space-y-4 overflow-y-auto p-6">
          <!-- Selección de Colaboradores -->
          <div>
            <label class="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
              Colaboradores a Asignar *
            </label>
            <div class="relative mb-2">
              <lucide-icon name="search" [size]="14" class="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-muted-foreground"></lucide-icon>
              <input
                type="search"
                [(ngModel)]="employeeSearch"
                (keydown.enter)="onEmployeeSearchEnter($event)"
                name="employeeSearch"
                placeholder="Buscar por nombre..."
                class="w-full rounded-xl border border-border bg-muted/40 py-2 pl-11 pr-3 text-xs text-foreground outline-none focus:border-primary focus:ring-1 focus:ring-primary" />
            </div>
            <div *ngIf="employeesLoading" class="rounded-xl border border-dashed border-border p-6 text-center text-xs text-muted-foreground bg-muted/20">
              Cargando colaboradores activos...
            </div>
            <div *ngIf="!employeesLoading && availableEmployees.length === 0" class="rounded-xl border border-dashed border-border p-6 text-center text-xs text-muted-foreground bg-muted/20">
              <p class="font-bold text-accent-foreground">No hay colaboradores registrados</p>
              <p class="mt-0.5">No se encontraron empleados activos en la base de datos para asignar turnos.</p>
            </div>
            <div *ngIf="!employeesLoading && availableEmployees.length > 0"
              [class.border-destructive]="submitted && selectedEmployeeIds.length === 0"
              class="space-y-2 max-h-36 overflow-y-auto rounded-xl border border-border bg-muted/20 p-2.5">
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
            <div *ngIf="!employeesLoading && availableEmployees.length > 0" class="mt-2 flex items-center justify-between text-[11px] text-muted-foreground">
              <span>{{ employeeTotalElements }} colaboradores encontrados</span>
              <div class="flex items-center gap-1">
                <button type="button" (click)="changeEmployeePage(-1)" [disabled]="employeePage === 0" class="rounded-md border border-border px-2 py-1 disabled:cursor-not-allowed disabled:opacity-40">Anterior</button>
                <span class="px-1">{{ employeePage + 1 }} / {{ employeeTotalPages }}</span>
                <button type="button" (click)="changeEmployeePage(1)" [disabled]="employeePage + 1 >= employeeTotalPages" class="rounded-md border border-border px-2 py-1 disabled:cursor-not-allowed disabled:opacity-40">Siguiente</button>
              </div>
            </div>
            <p *ngIf="availableEmployees.length > 0" class="text-[11px] text-muted-foreground mt-1">
              {{ selectedEmployeeIds.length }} colaboradores seleccionados
            </p>
            <p *ngIf="submitted && selectedEmployeeIds.length === 0" class="mt-1 text-xs font-medium text-destructive">
              Selecciona al menos un colaborador para asignar el turno.
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
                [class.border-destructive]="submitted && !startDate"
                class="w-full rounded-xl border border-border bg-muted/40 px-3.5 py-2 text-sm text-foreground outline-none focus:border-primary focus:ring-1 focus:ring-primary" />
              <p *ngIf="submitted && dateError" class="mt-1 text-xs font-medium text-destructive">
                {{ dateError }}
              </p>
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

          <!-- Días de aplicación semanal -->
          <div class="rounded-xl border border-border bg-muted/20 p-3">
            <p class="text-xs font-bold text-accent-foreground">Días de aplicación semanal</p>
            <p class="mt-0.5 text-[11px] text-muted-foreground">
              La asignación se aplicará en los días seleccionados durante el rango indicado.
            </p>
          </div>

          <!-- Días de Aplicación de la Asignación (HU16: weekdays short array) -->
          <div>
            <app-shift-days-picker
              [selectedDays]="weekdays"
              (daysChange)="weekdays = $event">
            </app-shift-days-picker>
            <p *ngIf="submitted && weekdays.length === 0" class="mt-1 text-xs font-medium text-destructive">
              Selecciona al menos un día laboral de la semana.
            </p>
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

          <!-- Botones de Acción (Footer fijo) -->
          <div class="flex shrink-0 items-center justify-end gap-3 border-t border-border bg-card px-6 py-4">
            <button
              type="button"
              (click)="close()"
              class="rounded-full border border-border bg-card px-4 py-2 text-xs font-semibold text-muted-foreground hover:bg-accent hover:text-foreground cursor-pointer">
              Cancelar
            </button>
            <button
              type="submit"
              [disabled]="assignmentUseCase.assigning()"
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

  private readonly employeesService = inject(EmployeesService);
  readonly assignmentUseCase = inject(ShiftAssignmentUseCase);

  availableEmployees: EmployeeSelectOption[] = [];
  selectedEmployeeIds: string[] = [];
  employeeSearch = '';
  employeePage = 0;
  employeeTotalPages = 1;
  employeeTotalElements = 0;
  private readonly employeePageSize = 25;
  startDate = this.todayLocal();
  endDate = '';
  weekdays: number[] = [1, 2, 3, 4, 5];
  submitted = false;
  employeesLoading = false;

  ngOnInit(): void {
    // La carga se realiza al abrir el modal para evitar solicitudes duplicadas
    // durante la inicialización de la página.
  }

  async loadEmployees(page = this.employeePage): Promise<void> {
    if (this.employeesLoading) return;
    this.employeesLoading = true;
    try {
      const res = await firstValueFrom(this.employeesService.list(page, this.employeePageSize, {
        status: 'ACTIVE',
        name: this.employeeSearch
      }));
      const pageData = res?.data as any;
      const list = Array.isArray(pageData) ? pageData : (pageData?.content || []);
      this.availableEmployees = list.map((e: any) => ({
        id: e.id,
        name: `${e.firstName} ${e.lastName}`,
        code: e.employeeNumber,
        position: 'Colaborador'
      }));
      this.employeePage = Array.isArray(pageData) ? 0 : (pageData?.page ?? page);
      this.employeeTotalPages = Math.max(1, Array.isArray(pageData) ? 1 : (pageData?.totalPages ?? 1));
      this.employeeTotalElements = Array.isArray(pageData) ? list.length : (pageData?.totalElements ?? list.length);
    } catch {
      this.availableEmployees = [];
      this.employeeTotalElements = 0;
      this.employeeTotalPages = 1;
    } finally {
      this.employeesLoading = false;
    }
  }

  searchEmployees(): void {
    this.employeePage = 0;
    void this.loadEmployees(0);
  }

  onEmployeeSearchEnter(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.searchEmployees();
  }

  changeEmployeePage(delta: number): void {
    const nextPage = this.employeePage + delta;
    if (nextPage < 0 || nextPage >= this.employeeTotalPages) return;
    this.employeePage = nextPage;
    void this.loadEmployees(nextPage);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen']?.currentValue === true) {
      this.resetForm();
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

  get dateError(): string | null {
    if (!this.startDate) return 'Indica la fecha de inicio.';
    if (this.startDate < this.todayLocal()) return 'La fecha de inicio no puede estar en el pasado.';
    if (this.endDate && this.endDate < this.startDate) return 'La fecha fin no puede ser anterior a la fecha de inicio.';
    return null;
  }

  get overlapError(): string | null {
    if (!this.startDate) return null;
    for (const empId of this.selectedEmployeeIds) {
      const check = this.assignmentUseCase.validateOverlap(empId, this.startDate, this.endDate, {
        weekdays: this.weekdays,
        shiftStartTime: this.shift?.startTime,
        shiftEndTime: this.shift?.endTime,
        shiftCrossesMidnight: this.shift?.crossesMidnight
      });
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
      !this.dateError &&
      !this.overlapError
    );
  }

  async submit(): Promise<void> {
    this.submitted = true;
    if (!this.isValid()) return;

    const ok = await this.assignmentUseCase.assign({
      shiftId: this.shift!.id,
      employeeIds: this.selectedEmployeeIds,
      startDate: this.startDate,
      endDate: this.endDate || null,
      weekdays: this.weekdays,
      shiftStartTime: this.shift!.startTime,
      shiftEndTime: this.shift!.endTime,
      shiftCrossesMidnight: this.shift!.crossesMidnight
    });

    if (ok) {
      this.assigned.emit();
      this.close();
    }
  }

  close(): void {
    this.submitted = false;
    this.closeModal.emit();
  }

  private resetForm(): void {
    this.submitted = false;
    this.selectedEmployeeIds = [];
    this.employeeSearch = '';
    this.employeePage = 0;
    this.employeeTotalPages = 1;
    this.employeeTotalElements = 0;
    this.startDate = this.todayLocal();
    this.endDate = '';
    this.weekdays = [1, 2, 3, 4, 5];
  }

  private todayLocal(): string {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
