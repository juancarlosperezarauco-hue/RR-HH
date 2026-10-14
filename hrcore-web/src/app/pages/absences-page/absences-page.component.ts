import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { LucideAngularModule } from 'lucide-angular';
import {
  VacationBalanceUseCase,
  AbsenceListUseCase,
  VacationBalanceCardComponent,
  AbsenceRequestModalComponent,
  AbsenceReviewModalComponent,
  AbsenceTableComponent
} from '../../features/absence-management';
import { LeaveRequestResponse, LeaveStatus } from '../../entities/absence';
import { EmployeesService, EmployeeSummary } from '../../entities/employees';

@Component({
  selector: 'app-absences-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    LucideAngularModule,
    VacationBalanceCardComponent,
    AbsenceRequestModalComponent,
    AbsenceReviewModalComponent,
    AbsenceTableComponent
  ],
  template: `
    <div class="space-y-6 pb-12">
      <!-- Page Header -->
      <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div class="flex items-center gap-2">
            <h1 class="text-2xl font-bold tracking-tight text-foreground">
              Gestión de Ausencias y Vacaciones
            </h1>
            <span class="rounded-md bg-primary/10 px-2 py-0.5 text-xs font-semibold text-primary">
              LGT Bolivia
            </span>
          </div>
          <p class="text-sm text-muted-foreground mt-1">
            Control legal de permisos, bajas médicas y licencias con cómputo de días hábiles.
          </p>
        </div>

        <div class="flex items-center gap-2">
          <button
            type="button"
            (click)="reloadData()"
            class="inline-flex items-center gap-2 rounded-xl border border-border bg-card px-4 py-2 text-sm font-semibold text-foreground hover:bg-accent transition-colors shadow-xs"
          >
            <lucide-icon name="refresh-ccw" class="h-4 w-4" [ngClass]="{ 'animate-spin': listUseCase.loading() }"></lucide-icon>
            <span>Actualizar</span>
          </button>
          <button
            type="button"
            (click)="isRequestModalOpen = true"
            [disabled]="!currentEmployeeId"
            class="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-bold text-primary-foreground hover:bg-primary/90 transition-all shadow-xs"
          >
            <lucide-icon name="plus" class="h-4 w-4"></lucide-icon>
            <span>Nueva Solicitud</span>
          </button>
        </div>
      </div>

      <div class="rounded-2xl border border-border bg-card p-4 shadow-xs">
        <label class="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
          Empleado
        </label>
        <select
          [(ngModel)]="currentEmployeeId"
          (ngModelChange)="onEmployeeChanged()"
          class="w-full max-w-xl rounded-xl border border-border bg-background px-3.5 py-2.5 text-sm font-medium text-foreground">
          <option value="" disabled>Seleccione un empleado</option>
          <option *ngFor="let employee of employees" [value]="employee.id">
            {{ employee.employeeNumber }} · {{ employee.firstName }} {{ employee.lastName }}
          </option>
        </select>
      </div>

      <!-- Vacation Balance Widget (LGT Bolivia) -->
      <app-vacation-balance-card
        [balance]="balanceUseCase.balance()"
        (requestLeave)="isRequestModalOpen = true"
      ></app-vacation-balance-card>

      <!-- KPI Summary Cards -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <!-- Pendientes -->
        <div class="rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:border-amber-500/40">
          <div class="flex items-center justify-between">
            <span class="text-xs font-bold uppercase tracking-wider text-muted-foreground">Por Revisar</span>
            <div class="flex h-9 w-9 items-center justify-center rounded-xl bg-amber-500/10 text-amber-600 dark:text-amber-400">
              <lucide-icon name="clock" class="h-5 w-5"></lucide-icon>
            </div>
          </div>
          <div class="mt-3 flex items-baseline gap-2">
            <span class="text-3xl font-black text-foreground">{{ listUseCase.pendingCount() }}</span>
            <span class="text-xs font-medium text-amber-600 dark:text-amber-400">en trámite</span>
          </div>
          <p class="mt-1 text-xs text-muted-foreground">Requieren aprobación de RRHH</p>
        </div>

        <!-- Aprobadas -->
        <div class="rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:border-emerald-500/40">
          <div class="flex items-center justify-between">
            <span class="text-xs font-bold uppercase tracking-wider text-muted-foreground">Aprobadas</span>
            <div class="flex h-9 w-9 items-center justify-center rounded-xl bg-emerald-500/10 text-emerald-600 dark:text-emerald-400">
              <lucide-icon name="check-circle" class="h-5 w-5"></lucide-icon>
            </div>
          </div>
          <div class="mt-3 flex items-baseline gap-2">
            <span class="text-3xl font-black text-foreground">{{ listUseCase.approvedCount() }}</span>
            <span class="text-xs font-medium text-emerald-600 dark:text-emerald-400">vigentes</span>
          </div>
          <p class="mt-1 text-xs text-muted-foreground">Sincronizadas con asistencia</p>
        </div>

        <!-- Rechazadas -->
        <div class="rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:border-rose-500/40">
          <div class="flex items-center justify-between">
            <span class="text-xs font-bold uppercase tracking-wider text-muted-foreground">Rechazadas</span>
            <div class="flex h-9 w-9 items-center justify-center rounded-xl bg-rose-500/10 text-rose-600 dark:text-rose-400">
              <lucide-icon name="ban" class="h-5 w-5"></lucide-icon>
            </div>
          </div>
          <div class="mt-3 flex items-baseline gap-2">
            <span class="text-3xl font-black text-foreground">{{ listUseCase.rejectedCount() }}</span>
            <span class="text-xs font-medium text-rose-600 dark:text-rose-400">con motivo</span>
          </div>
          <p class="mt-1 text-xs text-muted-foreground">Auditoría con justificación</p>
        </div>

        <!-- Días Totales Aprobados -->
        <div class="rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:border-primary/40">
          <div class="flex items-center justify-between">
            <span class="text-xs font-bold uppercase tracking-wider text-muted-foreground">Días Justificados</span>
            <div class="flex h-9 w-9 items-center justify-center rounded-xl bg-primary/10 text-primary">
              <lucide-icon name="calendar" class="h-5 w-5"></lucide-icon>
            </div>
          </div>
          <div class="mt-3 flex items-baseline gap-2">
            <span class="text-3xl font-black text-foreground">{{ listUseCase.totalApprovedDays() }}d</span>
            <span class="text-xs font-medium text-muted-foreground">hábiles</span>
          </div>
          <p class="mt-1 text-xs text-muted-foreground">{{ listUseCase.totalRequests() }} solicitudes en total</p>
        </div>
      </div>

      <!-- Requests Data Table -->
      <app-absence-table
        [requests]="listUseCase.filteredRequests()"
        [loading]="listUseCase.loading()"
        [searchTerm]="listUseCase.searchTerm()"
        [statusFilter]="listUseCase.statusFilter()"
        (searchChanged)="listUseCase.setSearch($event)"
        (statusChanged)="onStatusFilterChanged($event)"
        (refresh)="reloadData()"
        (review)="openReviewModal($event)"
      ></app-absence-table>

      <!-- New Request Modal -->
      <app-absence-request-modal
        [isOpen]="isRequestModalOpen"
        [employeeId]="currentEmployeeId"
        [leaveTypes]="listUseCase.leaveTypes()"
        [balance]="balanceUseCase.balance()"
        [holidays]="listUseCase.holidays()"
        (closeModal)="isRequestModalOpen = false"
        (requestSubmitted)="onRequestSubmitted()"
      ></app-absence-request-modal>

      <!-- Review / Decision Modal -->
      <app-absence-review-modal
        [isOpen]="isReviewModalOpen"
        [request]="selectedRequest"
        (closeModal)="isReviewModalOpen = false"
        (reviewCompleted)="onReviewCompleted()"
      ></app-absence-review-modal>
    </div>
  `
})
export class AbsencesPageComponent implements OnInit {
  private readonly employeesService = inject(EmployeesService);
  readonly balanceUseCase = inject(VacationBalanceUseCase);
  readonly listUseCase = inject(AbsenceListUseCase);

  employees: EmployeeSummary[] = [];
  currentEmployeeId = '';
  isRequestModalOpen = false;
  isReviewModalOpen = false;
  selectedRequest: LeaveRequestResponse | null = null;

  async ngOnInit(): Promise<void> {
    try {
      const employeeResponse = await firstValueFrom(this.employeesService.list(0, 100));
      this.employees = employeeResponse.data?.content || [];
      this.currentEmployeeId = this.employees[0]?.id || '';
    } catch {
      this.employees = [];
    }

    this.reloadData();
    this.listUseCase.loadLeaveTypes();
    this.listUseCase.loadHolidays();
  }

  reloadData(): void {
    if (this.currentEmployeeId) {
      this.balanceUseCase.loadBalance(this.currentEmployeeId);
      this.listUseCase.loadRequests(this.currentEmployeeId);
    }
  }

  onEmployeeChanged(): void {
    this.reloadData();
  }

  onStatusFilterChanged(status: LeaveStatus | 'ALL'): void {
    this.listUseCase.setStatusFilter(status);
  }

  openReviewModal(request: LeaveRequestResponse): void {
    this.selectedRequest = request;
    this.isReviewModalOpen = true;
  }

  onRequestSubmitted(): void {
    this.reloadData();
  }

  onReviewCompleted(): void {
    this.reloadData();
  }
}
