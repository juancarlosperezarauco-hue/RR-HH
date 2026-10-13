import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { AttendanceRecordResponse, AttendanceStatus } from '../../../../entities/attendance';
import { AttendanceStatusBadgeComponent } from '../attendance-status-badge/attendance-status-badge.component';

@Component({
  selector: 'app-attendance-table',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule, AttendanceStatusBadgeComponent],
  template: `
    <div class="rounded-2xl border border-border bg-card shadow-sm overflow-hidden">
      <!-- Table Filters & Search Bar -->
      <div class="p-4 sm:p-5 border-b border-border/60 flex flex-col md:flex-row items-stretch md:items-center justify-between gap-4">
        
        <!-- Search Input -->
        <div class="relative flex-1 max-w-md">
          <lucide-icon name="search" class="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground"></lucide-icon>
          <input
            type="text"
            [ngModel]="searchTerm"
            (ngModelChange)="onSearchChange($event)"
            placeholder="Buscar por colaborador o código..."
            class="w-full rounded-xl border border-border bg-background pl-10 pr-4 py-2 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:ring-1 focus:ring-primary transition-colors"
          />
        </div>

        <!-- Filter Controls -->
        <div class="flex flex-wrap items-center gap-2.5">
          <!-- Status Selector -->
          <select
            [ngModel]="statusFilter"
            (ngModelChange)="onStatusChange($event)"
            class="rounded-xl border border-border bg-background px-3 py-2 text-sm font-medium text-foreground focus:border-primary focus:ring-1 focus:ring-primary transition-colors"
          >
            <option value="ALL">Todos los Estados</option>
            <option value="PRESENT">Puntual / Presente</option>
            <option value="LATE">Con Retraso</option>
            <option value="ABSENT">Inasistencia / Falta</option>
            <option value="EARLY_DEPARTURE">Salida Anticipada</option>
            <option value="EXCUSED">Justificado</option>
          </select>

          <!-- Refresh Button -->
          <button
            type="button"
            (click)="refresh.emit()"
            class="inline-flex items-center gap-1.5 rounded-xl border border-border bg-background px-3 py-2 text-sm font-medium text-foreground hover:bg-accent hover:text-accent-foreground transition-colors"
            title="Actualizar registros"
          >
            <lucide-icon name="refresh-ccw" class="h-4 w-4" [ngClass]="{ 'animate-spin': loading }"></lucide-icon>
            <span class="hidden sm:inline">Refrescar</span>
          </button>
        </div>
      </div>

      <!-- Table Content -->
      <div class="overflow-x-auto">
        <table class="w-full text-left text-sm">
          <thead class="bg-muted/40 text-xs uppercase tracking-wider text-muted-foreground border-b border-border">
            <tr>
              <th scope="col" class="px-5 py-3 font-bold">Colaborador</th>
              <th scope="col" class="px-5 py-3 font-bold">Fecha</th>
              <th scope="col" class="px-5 py-3 font-bold">Entrada</th>
              <th scope="col" class="px-5 py-3 font-bold">Salida</th>
              <th scope="col" class="px-5 py-3 font-bold">Horas</th>
              <th scope="col" class="px-5 py-3 font-bold">Estado</th>
              <th scope="col" class="px-5 py-3 font-bold text-right">Acción</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-border/60">
            <!-- Loading Skeleton -->
            <tr *ngIf="loading && records.length === 0">
              <td colspan="7" class="px-5 py-12 text-center text-muted-foreground">
                <lucide-icon name="loader-circle" class="h-6 w-6 animate-spin mx-auto mb-2 text-primary"></lucide-icon>
                Cargando registros de asistencia...
              </td>
            </tr>

            <!-- Empty State -->
            <tr *ngIf="!loading && records.length === 0">
              <td colspan="7" class="px-5 py-12 text-center text-muted-foreground">
                <div class="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-muted/50 mb-3">
                  <lucide-icon name="clipboard-list" class="h-6 w-6 text-muted-foreground"></lucide-icon>
                </div>
                <p class="font-medium text-foreground">No se encontraron registros de asistencia</p>
                <p class="text-xs text-muted-foreground mt-1">Pruebe ajustando los filtros o el rango de fechas</p>
              </td>
            </tr>

            <!-- Records Rows -->
            <tr
              *ngFor="let item of records"
              class="hover:bg-accent/30 transition-colors"
            >
              <!-- Colaborador -->
              <td class="px-5 py-3.5">
                <div class="flex items-center gap-3">
                  <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary font-bold text-xs">
                    {{ getInitials(item.employeeFullName) }}
                  </div>
                  <div>
                    <div class="font-semibold text-foreground text-sm">
                      {{ item.employeeFullName || 'Colaborador' }}
                    </div>
                    <div class="font-mono text-xs text-muted-foreground">
                      {{ item.employeeCode || item.employeeId }}
                    </div>
                  </div>
                </div>
              </td>

              <!-- Fecha -->
              <td class="px-5 py-3.5 whitespace-nowrap text-xs font-medium text-foreground">
                {{ item.workDate }}
              </td>

              <!-- Entrada -->
              <td class="px-5 py-3.5 whitespace-nowrap">
                <div class="flex items-center gap-1.5 text-xs font-mono font-medium">
                  <span [ngClass]="item.checkInTime ? 'text-foreground' : 'text-muted-foreground'">
                    {{ formatTime(item.checkInTime) }}
                  </span>
                  <span
                    *ngIf="item.source"
                    class="rounded px-1.5 py-0.5 text-[10px] font-sans font-semibold uppercase bg-muted text-muted-foreground border border-border"
                  >
                    {{ item.source }}
                  </span>
                </div>
              </td>

              <!-- Salida -->
              <td class="px-5 py-3.5 whitespace-nowrap">
                <span class="text-xs font-mono font-medium" [ngClass]="item.checkOutTime ? 'text-foreground' : 'text-muted-foreground'">
                  {{ formatTime(item.checkOutTime) }}
                </span>
              </td>

              <!-- Horas Trabajadas y Retraso -->
              <td class="px-5 py-3.5 whitespace-nowrap">
                <div class="text-xs font-semibold text-foreground">
                  {{ item.workedHours }}h
                  <span *ngIf="item.overtimeHours && item.overtimeHours > 0" class="text-emerald-600 dark:text-emerald-400 font-mono">
                    (+{{ item.overtimeHours }}h extra)
                  </span>
                </div>
                <div *ngIf="item.lateMinutes && item.lateMinutes > 0" class="text-[11px] font-medium text-amber-600 dark:text-amber-400">
                  Retraso: {{ item.lateMinutes }} min
                </div>
              </td>

              <!-- Estado -->
              <td class="px-5 py-3.5 whitespace-nowrap">
                <app-attendance-status-badge
                  [status]="item.status"
                  [lateMinutes]="item.lateMinutes"
                ></app-attendance-status-badge>
              </td>

              <!-- Acción Regularizar -->
              <td class="px-5 py-3.5 whitespace-nowrap text-right">
                <button
                  type="button"
                  (click)="regularize.emit(item)"
                  class="inline-flex items-center gap-1 rounded-lg border border-border bg-background px-2.5 py-1.5 text-xs font-semibold text-foreground hover:bg-accent hover:text-accent-foreground transition-colors"
                  title="Ajustar o regularizar marcación"
                >
                  <lucide-icon name="pencil" class="h-3.5 w-3.5"></lucide-icon>
                  <span>Regularizar</span>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Table Footer / Count -->
      <div class="p-4 border-t border-border/60 bg-muted/20 flex items-center justify-between text-xs text-muted-foreground">
        <span>Mostrando {{ records.length }} registros de asistencia</span>
        <span>Jornadas reguladas por LGT Bolivia</span>
      </div>
    </div>
  `
})
export class AttendanceTableComponent {
  @Input() records: AttendanceRecordResponse[] = [];
  @Input() loading = false;
  @Input() searchTerm = '';
  @Input() statusFilter: AttendanceStatus | 'ALL' = 'ALL';

  @Output() searchChanged = new EventEmitter<string>();
  @Output() statusChanged = new EventEmitter<AttendanceStatus | 'ALL'>();
  @Output() refresh = new EventEmitter<void>();
  @Output() regularize = new EventEmitter<AttendanceRecordResponse>();

  onSearchChange(term: string): void {
    this.searchChanged.emit(term);
  }

  onStatusChange(status: AttendanceStatus | 'ALL'): void {
    this.statusChanged.emit(status);
  }

  formatTime(iso?: string | null): string {
    if (!iso) return '--:--';
    try {
      const d = new Date(iso);
      return d.toLocaleTimeString('es-BO', { hour: '2-digit', minute: '2-digit', hour12: false });
    } catch {
      return '--:--';
    }
  }

  getInitials(name?: string): string {
    if (!name) return 'CO';
    const parts = name.trim().split(' ');
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return parts[0].substring(0, 2).toUpperCase();
  }
}
