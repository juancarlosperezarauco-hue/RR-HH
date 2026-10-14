import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import {
  AttendanceListUseCase,
  ClockInWidgetComponent,
  AttendanceTableComponent,
  AttendanceRegularizeDialogComponent
} from '../../features/attendance-management';
import { AttendanceRecordResponse, AttendanceStatus } from '../../entities/attendance';
import { EmployeesService, EmployeeSummary } from '../../entities/employees';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-attendance-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    LucideAngularModule,
    ClockInWidgetComponent,
    AttendanceTableComponent,
    AttendanceRegularizeDialogComponent
  ],
  template: `
    <div class="space-y-6 pb-12">
      <!-- Page Header -->
      <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div class="flex items-center gap-2">
            <h1 class="text-2xl font-bold tracking-tight text-foreground">
              Control y Registro de Asistencia
            </h1>
            <span class="rounded-md bg-primary/10 px-2 py-0.5 text-xs font-semibold text-primary">
              LGT Bolivia
            </span>
          </div>
          <p class="text-sm text-muted-foreground mt-1">
            Monitoreo en tiempo real de marcaciones, puntualidad y cumplimiento de jornadas laborales.
          </p>
        </div>

        <div class="flex items-center gap-2">
          <button
            type="button"
            (click)="listUseCase.loadRecords()"
            class="inline-flex items-center gap-2 rounded-xl border border-border bg-card px-4 py-2 text-sm font-semibold text-foreground hover:bg-accent transition-colors shadow-xs"
          >
            <lucide-icon name="refresh-ccw" class="h-4 w-4" [ngClass]="{ 'animate-spin': listUseCase.loading() }"></lucide-icon>
            <span>Actualizar</span>
          </button>
        </div>
      </div>

      <div class="grid grid-cols-1 gap-4 rounded-2xl border border-border bg-card p-4 shadow-xs md:grid-cols-3">
        <div>
          <label class="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">Empleado</label>
          <select
            [(ngModel)]="selectedEmployeeId"
            (ngModelChange)="listUseCase.setEmployeeFilter($event)"
            class="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm text-foreground">
            <option value="">Todos los empleados</option>
            <option *ngFor="let employee of employees" [value]="employee.id">
              {{ employee.employeeNumber }} · {{ employee.firstName }} {{ employee.lastName }}
            </option>
          </select>
        </div>
        <div>
          <label class="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">Desde</label>
          <input
            type="date"
            [ngModel]="listUseCase.startDate()"
            (ngModelChange)="updateDateRange($event, listUseCase.endDate())"
            class="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm text-foreground" />
        </div>
        <div>
          <label class="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">Hasta</label>
          <input
            type="date"
            [ngModel]="listUseCase.endDate()"
            (ngModelChange)="updateDateRange(listUseCase.startDate(), $event)"
            class="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm text-foreground" />
        </div>
      </div>

      <!-- Live Clock Widget (Entrada / Salida & Anti-Debounce) -->
      <app-clock-in-widget
        (clockRegistered)="onClockRegistered($event)"
      ></app-clock-in-widget>

      <!-- KPI Summary Cards -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <!-- Puntualidad -->
        <div class="rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:border-emerald-500/40">
          <div class="flex items-center justify-between">
            <span class="text-xs font-bold uppercase tracking-wider text-muted-foreground">Puntualidad</span>
            <div class="flex h-9 w-9 items-center justify-center rounded-xl bg-emerald-500/10 text-emerald-600 dark:text-emerald-400">
              <lucide-icon name="check-circle" class="h-5 w-5"></lucide-icon>
            </div>
          </div>
          <div class="mt-3 flex items-baseline gap-2">
            <span class="text-3xl font-black text-foreground">{{ listUseCase.punctualityRate() }}%</span>
            <span class="text-xs font-medium text-emerald-600 dark:text-emerald-400">Objetivo &gt; 95%</span>
          </div>
          <p class="mt-1 text-xs text-muted-foreground">{{ listUseCase.presentCount() }} colaboradores puntuales</p>
        </div>

        <!-- Retrasos -->
        <div class="rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:border-amber-500/40">
          <div class="flex items-center justify-between">
            <span class="text-xs font-bold uppercase tracking-wider text-muted-foreground">Retrasos Hoy</span>
            <div class="flex h-9 w-9 items-center justify-center rounded-xl bg-amber-500/10 text-amber-600 dark:text-amber-400">
              <lucide-icon name="clock" class="h-5 w-5"></lucide-icon>
            </div>
          </div>
          <div class="mt-3 flex items-baseline gap-2">
            <span class="text-3xl font-black text-foreground">{{ listUseCase.lateCount() }}</span>
            <span class="text-xs font-medium text-amber-600 dark:text-amber-400">{{ listUseCase.totalLateMinutes() }} min totales</span>
          </div>
          <p class="mt-1 text-xs text-muted-foreground">Fuera del margen de tolerancia</p>
        </div>

        <!-- Inasistencias -->
        <div class="rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:border-rose-500/40">
          <div class="flex items-center justify-between">
            <span class="text-xs font-bold uppercase tracking-wider text-muted-foreground">Inasistencias</span>
            <div class="flex h-9 w-9 items-center justify-center rounded-xl bg-rose-500/10 text-rose-600 dark:text-rose-400">
              <lucide-icon name="alert-triangle" class="h-5 w-5"></lucide-icon>
            </div>
          </div>
          <div class="mt-3 flex items-baseline gap-2">
            <span class="text-3xl font-black text-foreground">{{ listUseCase.absentCount() }}</span>
            <span class="text-xs font-medium text-rose-600 dark:text-rose-400">Sin justificar</span>
          </div>
          <p class="mt-1 text-xs text-muted-foreground">Marcaciones ausentes registradas</p>
        </div>

        <!-- Horas Computadas -->
        <div class="rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:border-primary/40">
          <div class="flex items-center justify-between">
            <span class="text-xs font-bold uppercase tracking-wider text-muted-foreground">Horas Computadas</span>
            <div class="flex h-9 w-9 items-center justify-center rounded-xl bg-primary/10 text-primary">
              <lucide-icon name="clipboard-list" class="h-5 w-5"></lucide-icon>
            </div>
          </div>
          <div class="mt-3 flex items-baseline gap-2">
            <span class="text-3xl font-black text-foreground">{{ listUseCase.totalWorkedHours() }}h</span>
            <span class="text-xs font-medium text-muted-foreground">efectivas</span>
          </div>
          <p class="mt-1 text-xs text-muted-foreground">{{ listUseCase.totalRecords() }} jornadas computadas</p>
        </div>
      </div>

      <!-- Attendance Table -->
      <app-attendance-table
        [records]="listUseCase.filteredRecords()"
        [loading]="listUseCase.loading()"
        [searchTerm]="listUseCase.searchTerm()"
        [statusFilter]="listUseCase.statusFilter()"
        (searchChanged)="listUseCase.setSearch($event)"
        (statusChanged)="onStatusFilterChanged($event)"
        (refresh)="listUseCase.loadRecords()"
        (regularize)="openRegularizeDialog($event)"
      ></app-attendance-table>

      <!-- Regularize Modal Dialog -->
      <app-attendance-regularize-dialog
        [isOpen]="isRegularizeModalOpen"
        [record]="selectedRecord"
        (closeDialog)="isRegularizeModalOpen = false"
        (regularized)="onRegularized()"
      ></app-attendance-regularize-dialog>
    </div>
  `
})
export class AttendancePageComponent implements OnInit {
  readonly listUseCase = inject(AttendanceListUseCase);
  private readonly employeesService = inject(EmployeesService);

  employees: EmployeeSummary[] = [];
  selectedEmployeeId = '';

  isRegularizeModalOpen = false;
  selectedRecord: AttendanceRecordResponse | null = null;

  async ngOnInit(): Promise<void> {
    try {
      const response = await firstValueFrom(this.employeesService.list(0, 100));
      this.employees = response.data?.content || [];
    } catch {
      this.employees = [];
    }
    this.listUseCase.loadRecords();
  }

  updateDateRange(start: string, end: string): void {
    if (!start || !end || start > end) return;
    this.listUseCase.setDateRange(start, end);
  }

  onClockRegistered(_record: AttendanceRecordResponse): void {
    this.listUseCase.loadRecords();
  }

  onStatusFilterChanged(status: AttendanceStatus | 'ALL'): void {
    this.listUseCase.setStatusFilter(status);
  }

  openRegularizeDialog(record: AttendanceRecordResponse): void {
    this.selectedRecord = record;
    this.isRegularizeModalOpen = true;
  }

  onRegularized(): void {
    this.listUseCase.loadRecords();
  }
}
