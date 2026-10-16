import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { LeaveRequestResponse, LeaveStatus } from '../../../../entities/absence';
import { AbsenceStatusBadgeComponent } from '../absence-status-badge/absence-status-badge.component';

@Component({
  selector: 'app-absence-table',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule, AbsenceStatusBadgeComponent],
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
            (ngModelChange)="searchChanged.emit($event)"
            placeholder="Buscar por colaborador o motivo..."
            class="w-full rounded-xl border border-border bg-background pl-10 pr-4 py-2 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:ring-1 focus:ring-primary transition-colors"
          />
        </div>

        <!-- Filter Controls -->
        <div class="flex flex-wrap items-center gap-2.5">
          <!-- Status Selector -->
          <select
            [ngModel]="statusFilter"
            (ngModelChange)="statusChanged.emit($event)"
            class="rounded-xl border border-border bg-background px-3 py-2 text-sm font-medium text-foreground focus:border-primary focus:ring-1 focus:ring-primary transition-colors"
          >
            <option value="ALL">Todos los Estados</option>
            <option value="DRAFT">Borradores</option>
            <option value="PENDING">Pendientes de Aprobación</option>
            <option value="APPROVED">Aprobadas</option>
            <option value="REJECTED">Rechazadas</option>
            <option value="CANCELLED">Canceladas</option>
          </select>

          <!-- Refresh Button -->
          <button
            type="button"
            (click)="refresh.emit()"
            class="inline-flex items-center gap-1.5 rounded-xl border border-border bg-background px-3 py-2 text-sm font-medium text-foreground hover:bg-accent hover:text-accent-foreground transition-colors"
            title="Actualizar solicitudes"
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
              <th scope="col" class="px-5 py-3 font-bold">Tipo de Ausencia</th>
              <th scope="col" class="px-5 py-3 font-bold">Período / Horario</th>
              <th scope="col" class="px-5 py-3 font-bold">Duración</th>
              <th scope="col" class="px-5 py-3 font-bold">Respaldo</th>
              <th scope="col" class="px-5 py-3 font-bold">Estado</th>
              <th scope="col" class="px-5 py-3 font-bold text-right">Acción</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-border/60">
            <!-- Loading Skeleton -->
            <tr *ngIf="loading && requests.length === 0">
              <td colspan="7" class="px-5 py-12 text-center text-muted-foreground">
                <lucide-icon name="loader-circle" class="h-6 w-6 animate-spin mx-auto mb-2 text-primary"></lucide-icon>
                Cargando solicitudes de ausencia...
              </td>
            </tr>

            <!-- Empty State -->
            <tr *ngIf="!loading && requests.length === 0">
              <td colspan="7" class="px-5 py-12 text-center text-muted-foreground">
                <div class="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-muted/50 mb-3">
                  <lucide-icon name="calendar" class="h-6 w-6 text-muted-foreground"></lucide-icon>
                </div>
                <p class="font-medium text-foreground">No se encontraron solicitudes de ausencia</p>
                <p class="text-xs text-muted-foreground mt-1">Pruebe ajustando los filtros o registre una nueva solicitud con el botón superior</p>
              </td>
            </tr>

            <!-- Requests Rows -->
            <tr
              *ngFor="let req of requests"
              class="hover:bg-accent/30 transition-colors"
            >
              <!-- Colaborador -->
              <td class="px-5 py-3.5">
                <div class="flex items-center gap-3">
                  <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary font-bold text-xs">
                    {{ getInitials(req.employeeFullName) }}
                  </div>
                  <div>
                    <div class="font-semibold text-foreground text-sm">
                      {{ req.employeeFullName || 'Colaborador' }}
                    </div>
                    <div class="text-xs text-muted-foreground line-clamp-1 max-w-[220px]" [title]="req.reason">
                      "{{ req.reason }}"
                    </div>
                  </div>
                </div>
              </td>

              <!-- Tipo de Licencia -->
              <td class="px-5 py-3.5 whitespace-nowrap">
                <span class="text-xs font-semibold text-foreground">
                  {{ req.leaveTypeName || 'Permiso Laboral' }}
                </span>
              </td>

              <!-- Período / Horas -->
              <td class="px-5 py-3.5 whitespace-nowrap text-xs text-foreground font-medium">
                <div>{{ req.startDate }} al {{ req.endDate }}</div>
                <div *ngIf="req.startTime && req.endTime" class="text-[11px] text-muted-foreground font-mono mt-0.5">
                  {{ req.startTime }} - {{ req.endTime }}
                </div>
              </td>

              <!-- Duración -->
              <td class="px-5 py-3.5 whitespace-nowrap">
                <span class="font-mono text-xs font-bold text-foreground">
                  {{ req.totalDays }}d
                </span>
                <span class="text-[11px] text-muted-foreground ml-1">hábiles</span>
              </td>

              <!-- Respaldo / Justificativo -->
              <td class="px-5 py-3.5 whitespace-nowrap">
                <button
                  type="button"
                  (click)="review.emit(req)"
                  class="inline-flex items-center gap-1.5 rounded-lg border border-border/80 bg-background/80 px-2 py-1 text-xs font-medium text-foreground hover:bg-accent transition-colors"
                  title="Ver o adjuntar documentos probatorios"
                >
                  <lucide-icon name="paperclip" class="h-3.5 w-3.5 text-primary"></lucide-icon>
                  <span>Respaldo</span>
                </button>
              </td>

              <!-- Estado -->
              <td class="px-5 py-3.5 whitespace-nowrap">
                <app-absence-status-badge [status]="req.status"></app-absence-status-badge>
              </td>

              <!-- Acción Evaluar / Ver Detalle -->
              <td class="px-5 py-3.5 whitespace-nowrap text-right">
                <button
                  type="button"
                  (click)="review.emit(req)"
                  class="inline-flex items-center gap-1.5 rounded-xl border border-border bg-background px-3 py-1.5 text-xs font-semibold text-foreground hover:bg-accent hover:text-accent-foreground transition-colors shadow-2xs"
                  title="Revisar o evaluar solicitud"
                >
                  <lucide-icon name="eye" class="h-3.5 w-3.5"></lucide-icon>
                  <span>{{ req.status === 'PENDING' ? 'Evaluar' : 'Detalles' }}</span>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Table Footer -->
      <div class="p-4 border-t border-border/60 bg-muted/20 flex flex-col sm:flex-row items-center justify-between gap-2 text-xs text-muted-foreground">
        <span>Mostrando {{ requests.length }} solicitudes de ausencia o permiso</span>
        <span>Cómputo en días hábiles conforme a la Ley General del Trabajo de Bolivia</span>
      </div>
    </div>
  `
})
export class AbsenceTableComponent {
  @Input() requests: LeaveRequestResponse[] = [];
  @Input() loading = false;
  @Input() searchTerm = '';
  @Input() statusFilter: LeaveStatus | 'ALL' = 'ALL';

  @Output() searchChanged = new EventEmitter<string>();
  @Output() statusChanged = new EventEmitter<LeaveStatus | 'ALL'>();
  @Output() refresh = new EventEmitter<void>();
  @Output() review = new EventEmitter<LeaveRequestResponse>();

  getInitials(name?: string): string {
    if (!name) return 'CO';
    const parts = name.trim().split(' ');
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return parts[0].substring(0, 2).toUpperCase();
  }
}
