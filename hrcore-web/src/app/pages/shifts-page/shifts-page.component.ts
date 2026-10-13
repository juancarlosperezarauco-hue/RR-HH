import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import {
  ShiftListUseCase,
  ShiftAssignmentUseCase,
  ShiftCardComponent,
  ShiftFormModalComponent,
  ShiftAssignModalComponent,
  ShiftAssignmentsTableComponent
} from '../../features/shift-management';
import { PlatformPaginationComponent } from '../../features/platform/ui/platform-pagination/platform-pagination.component';
import { ShiftResponse } from '../../entities/shift';
import { MOCK_SHIFTS } from '../../shared/api/interceptors/data/mock-shifts.data';

@Component({
  selector: 'app-shifts-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    LucideAngularModule,
    PlatformPaginationComponent,
    ShiftCardComponent,
    ShiftFormModalComponent,
    ShiftAssignModalComponent,
    ShiftAssignmentsTableComponent
  ],
  template: `
    <div class="space-y-6 relative">
      <!-- Encabezado Principal -->
      <section class="rounded-[28px] border border-border bg-gradient-to-br from-card via-background to-muted p-6 shadow-lg">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div class="space-y-3">
            <div class="inline-flex items-center gap-2 rounded-full border border-border bg-card/80 px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-primary">
              <span class="h-2 w-2 rounded-full bg-secondary"></span>
              Jornadas y Horarios Laborales
            </div>
            <div>
              <h1 class="text-3xl font-black tracking-tight text-accent-foreground sm:text-4xl">
                Parametrización y Asignación de Turnos
              </h1>
              <p class="mt-2 max-w-3xl text-sm leading-6 text-muted-foreground">
                Configura horarios laborales con validación estricta de la Ley General del Trabajo (LGT Bolivia) y asigna turnos a colaboradores previniendo solapamientos.
              </p>
            </div>
          </div>

          <div class="flex flex-wrap gap-3">
            <button
              type="button"
              (click)="reload()"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-border bg-card px-4 py-2 text-sm font-semibold text-primary transition-colors hover:bg-accent">
              <lucide-icon name="refresh-ccw" [size]="16"></lucide-icon>
              Recargar
            </button>

            <button
              type="button"
              (click)="openCreateModal()"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground shadow-md transition-all hover:bg-primary/90">
              <lucide-icon name="plus" [size]="16"></lucide-icon>
              Nuevo Turno
            </button>
          </div>
        </div>
      </section>

      <!-- Fila de Tarjetas KPI -->
      <div class="grid grid-cols-1 gap-4 sm:grid-cols-4">
        <!-- KPI 1 -->
        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm flex items-center justify-between">
          <div class="space-y-1">
            <p class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Total Turnos</p>
            <p class="text-2xl font-black text-accent-foreground">{{ shiftListUseCase.kpis().total }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary/10 text-primary">
            <lucide-icon name="clock" [size]="22"></lucide-icon>
          </div>
        </div>

        <!-- KPI 2 -->
        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm flex items-center justify-between">
          <div class="space-y-1">
            <p class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Turnos Activos</p>
            <p class="text-2xl font-black text-emerald-600">{{ shiftListUseCase.kpis().active }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-2xl bg-emerald-500/10 text-emerald-600">
            <lucide-icon name="check-circle" [size]="22"></lucide-icon>
          </div>
        </div>

        <!-- KPI 3 -->
        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm flex items-center justify-between">
          <div class="space-y-1">
            <p class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Turnos Nocturnos (7h)</p>
            <p class="text-2xl font-black text-indigo-600">{{ shiftListUseCase.kpis().night }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-2xl bg-indigo-500/10 text-indigo-600">
            <lucide-icon name="clock" [size]="22"></lucide-icon>
          </div>
        </div>

        <!-- KPI 4 -->
        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm flex items-center justify-between">
          <div class="space-y-1">
            <p class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Continuos (8h)</p>
            <p class="text-2xl font-black text-blue-600">{{ shiftListUseCase.kpis().continuous }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-2xl bg-blue-500/10 text-blue-600">
            <lucide-icon name="calendar" [size]="22"></lucide-icon>
          </div>
        </div>
      </div>

      <!-- Barra de Filtros, Búsqueda y Selector de Modo de Vista -->
      <div class="rounded-2xl border border-border bg-card p-5 shadow-sm space-y-4">
        <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <!-- Búsqueda -->
          <div class="relative flex-1 max-w-md">
            <lucide-icon
              name="search"
              [size]="16"
              class="absolute left-3.5 top-1/2 -translate-y-1/2 text-muted-foreground"></lucide-icon>
            <input
              type="text"
              [ngModel]="shiftListUseCase.searchQuery()"
              (ngModelChange)="shiftListUseCase.setSearchQuery($event)"
              placeholder="Buscar turno por nombre o código..."
              class="w-full rounded-full border border-border bg-muted/40 pl-10 pr-4 py-2 text-xs font-medium text-foreground outline-none focus:border-primary focus:ring-1 focus:ring-primary" />
          </div>

          <!-- Selector de Modo de Vista -->
          <div class="inline-flex rounded-full border border-border bg-muted/30 p-1">
            <button
              type="button"
              (click)="activeView = 'CATALOG'"
              [class.bg-card]="activeView === 'CATALOG'"
              [class.text-primary]="activeView === 'CATALOG'"
              [class.shadow-sm]="activeView === 'CATALOG'"
              [class.text-muted-foreground]="activeView !== 'CATALOG'"
              class="inline-flex items-center gap-1.5 rounded-full px-4 py-1.5 text-xs font-semibold transition-all cursor-pointer">
              <lucide-icon name="clock" [size]="14"></lucide-icon>
              Catálogo de Turnos
            </button>
            <button
              type="button"
              (click)="activeView = 'ASSIGNMENTS'"
              [class.bg-card]="activeView === 'ASSIGNMENTS'"
              [class.text-primary]="activeView === 'ASSIGNMENTS'"
              [class.shadow-sm]="activeView === 'ASSIGNMENTS'"
              [class.text-muted-foreground]="activeView !== 'ASSIGNMENTS'"
              class="inline-flex items-center gap-1.5 rounded-full px-4 py-1.5 text-xs font-semibold transition-all cursor-pointer">
              <lucide-icon name="users" [size]="14"></lucide-icon>
              Asignaciones Activas
            </button>
          </div>
        </div>

        <!-- Filtro por Estado (solo en vista de catálogo) -->
        <div *ngIf="activeView === 'CATALOG'" class="flex items-center gap-2 border-t border-border pt-3">
          <button
            type="button"
            (click)="shiftListUseCase.setStatusFilter('ALL')"
            [class.bg-primary]="shiftListUseCase.statusFilter() === 'ALL'"
            [class.text-primary-foreground]="shiftListUseCase.statusFilter() === 'ALL'"
            [class.bg-card]="shiftListUseCase.statusFilter() !== 'ALL'"
            [class.text-muted-foreground]="shiftListUseCase.statusFilter() !== 'ALL'"
            class="rounded-full border border-border px-3.5 py-1 text-xs font-semibold transition-all cursor-pointer hover:bg-accent hover:text-foreground">
            Todos
          </button>
          <button
            type="button"
            (click)="shiftListUseCase.setStatusFilter('ACTIVE')"
            [class.bg-primary]="shiftListUseCase.statusFilter() === 'ACTIVE'"
            [class.text-primary-foreground]="shiftListUseCase.statusFilter() === 'ACTIVE'"
            [class.bg-card]="shiftListUseCase.statusFilter() !== 'ACTIVE'"
            [class.text-muted-foreground]="shiftListUseCase.statusFilter() !== 'ACTIVE'"
            class="rounded-full border border-border px-3.5 py-1 text-xs font-semibold transition-all cursor-pointer hover:bg-accent hover:text-foreground">
            Activos
          </button>
          <button
            type="button"
            (click)="shiftListUseCase.setStatusFilter('INACTIVE')"
            [class.bg-primary]="shiftListUseCase.statusFilter() === 'INACTIVE'"
            [class.text-primary-foreground]="shiftListUseCase.statusFilter() === 'INACTIVE'"
            [class.bg-card]="shiftListUseCase.statusFilter() !== 'INACTIVE'"
            [class.text-muted-foreground]="shiftListUseCase.statusFilter() !== 'INACTIVE'"
            class="rounded-full border border-border px-3.5 py-1 text-xs font-semibold transition-all cursor-pointer hover:bg-accent hover:text-foreground">
            Inactivos
          </button>
        </div>
      </div>

      <!-- Vista 1: Catálogo de Turnos (Grilla de Tarjetas) -->
      <div *ngIf="activeView === 'CATALOG'" class="space-y-4">
        <div *ngIf="shiftListUseCase.filteredShifts().length === 0" class="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border p-12 text-center bg-card/50">
          <div class="flex h-14 w-14 items-center justify-center rounded-2xl bg-muted text-muted-foreground mb-3">
            <lucide-icon name="clock" [size]="28"></lucide-icon>
          </div>
          <h4 class="text-base font-bold text-accent-foreground">No se encontraron turnos</h4>
          <p class="text-xs text-muted-foreground max-w-sm mt-1">Crea un nuevo turno o ajusta los filtros de búsqueda.</p>
        </div>

        <div *ngIf="shiftListUseCase.filteredShifts().length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <app-shift-card
            *ngFor="let s of shiftListUseCase.filteredShifts()"
            [shift]="s"
            (assign)="openAssignModal($event)"
            (edit)="openEditModal($event)"
            (toggleStatus)="shiftListUseCase.toggleStatus($event)">
          </app-shift-card>
        </div>

        <!-- Paginación -->
        <div *ngIf="shiftListUseCase.totalPages() > 1" class="flex justify-end pt-2">
          <app-platform-pagination
            [currentPage]="shiftListUseCase.currentPage()"
            [totalPages]="shiftListUseCase.totalPages()"
            [totalElements]="shiftListUseCase.totalElements()"
            (pageChange)="shiftListUseCase.setPage($event)">
          </app-platform-pagination>
        </div>
      </div>

      <!-- Vista 2: Tabla de Asignaciones de Trabajadores -->
      <app-shift-assignments-table
        *ngIf="activeView === 'ASSIGNMENTS'"
        [assignments]="assignments">
      </app-shift-assignments-table>

      <!-- Modal de Creación / Edición de Turnos -->
      <app-shift-form-modal
        [isOpen]="isFormOpen"
        [shiftToEdit]="selectedShiftToEdit"
        (closeModal)="isFormOpen = false"
        (saved)="onShiftSaved()">
      </app-shift-form-modal>

      <!-- Modal de Asignación de Turno -->
      <app-shift-assign-modal
        [isOpen]="isAssignOpen"
        [shift]="selectedShiftToAssign"
        (closeModal)="isAssignOpen = false"
        (assigned)="onShiftAssigned()">
      </app-shift-assign-modal>
    </div>
  `
})
export class ShiftsPageComponent implements OnInit {
  readonly shiftListUseCase = inject(ShiftListUseCase);
  readonly shiftAssignmentUseCase = inject(ShiftAssignmentUseCase);

  activeView: 'CATALOG' | 'ASSIGNMENTS' = 'CATALOG';
  isFormOpen = false;
  isAssignOpen = false;
  selectedShiftToEdit: ShiftResponse | null = null;
  selectedShiftToAssign: ShiftResponse | null = null;

  get assignments() {
    return MOCK_SHIFTS.employeeShiftAssignments;
  }

  ngOnInit(): void {
    this.shiftListUseCase.loadShifts();
    this.shiftAssignmentUseCase.loadAssignments();
  }

  reload(): void {
    this.shiftListUseCase.loadShifts();
    this.shiftAssignmentUseCase.loadAssignments();
  }

  openCreateModal(): void {
    this.selectedShiftToEdit = null;
    this.isFormOpen = true;
  }

  openEditModal(shift: ShiftResponse): void {
    this.selectedShiftToEdit = shift;
    this.isFormOpen = true;
  }

  openAssignModal(shift: ShiftResponse): void {
    this.selectedShiftToAssign = shift;
    this.isAssignOpen = true;
  }

  onShiftSaved(): void {
    this.shiftListUseCase.loadShifts();
  }

  onShiftAssigned(): void {
    this.shiftAssignmentUseCase.loadAssignments();
  }
}
