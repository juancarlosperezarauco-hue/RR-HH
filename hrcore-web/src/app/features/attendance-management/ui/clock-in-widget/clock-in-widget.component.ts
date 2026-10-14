import {
  Component,
  OnInit,
  OnDestroy,
  inject,
  signal,
  Output,
  EventEmitter
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { AttendanceClockUseCase } from '../../application/attendance-clock.usecase';
import { AttendanceRecordResponse } from '../../../../entities/attendance';
import { EmployeesService } from '../../../../entities/employees';
import { firstValueFrom } from 'rxjs';

export interface ClockEmployeeOption {
  id: string;
  name: string;
  code: string;
}

@Component({
  selector: 'app-clock-in-widget',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  template: `
    <div
      class="relative overflow-hidden rounded-2xl border border-border bg-gradient-to-br from-card via-card to-accent/30 p-6 shadow-sm transition-all"
    >
      <!-- Background Ambient Glow -->
      <div
        class="pointer-events-none absolute -right-16 -top-16 h-52 w-52 rounded-full bg-primary/10 blur-3xl"
      ></div>

      <div class="relative z-10 flex flex-col lg:flex-row lg:items-center lg:justify-between gap-6">
        
        <!-- Left: Reloj Digital y Fecha -->
        <div class="space-y-2">
          <div class="flex items-center gap-2">
            <span class="inline-flex items-center gap-1.5 rounded-md bg-primary/10 px-2.5 py-0.5 text-xs font-semibold text-primary">
              <lucide-icon name="clock" class="h-3.5 w-3.5"></lucide-icon>
              Hora Oficial Sincronizada
            </span>

            <!-- Indicador GPS -->
            <span
              class="inline-flex items-center gap-1 rounded-md px-2 py-0.5 text-xs font-medium border"
              [ngClass]="
                clockUseCase.gpsAvailable() === true
                  ? 'bg-emerald-500/10 text-emerald-700 dark:text-emerald-400 border-emerald-500/30'
                  : 'bg-muted text-muted-foreground border-border'
              "
              title="Geolocalización contra radio de sucursal"
            >
              <lucide-icon name="check-circle" class="h-3 w-3" *ngIf="clockUseCase.gpsAvailable()"></lucide-icon>
              <lucide-icon name="clock" class="h-3 w-3" *ngIf="!clockUseCase.gpsAvailable()"></lucide-icon>
              {{ clockUseCase.gpsAvailable() ? 'GPS Activo' : 'GPS Standby' }}
            </span>
          </div>

          <!-- Hora grande digital -->
          <div class="font-mono text-4xl sm:text-5xl font-black tracking-tight text-foreground">
            {{ currentTime() }}
          </div>

          <!-- Fecha en español -->
          <p class="text-sm font-medium text-muted-foreground capitalize">
            {{ currentDate() }}
          </p>
        </div>

        <!-- Center: Selector de Colaborador (Modo Operación RRHH / Kiosco) -->
        <div class="flex-1 max-w-sm">
          <label class="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
            Colaborador en Marcación
          </label>
          <div class="relative">
            <select
              [(ngModel)]="selectedEmployeeId"
              (ngModelChange)="onEmployeeChange()"
              class="w-full rounded-xl border border-border bg-background px-3.5 py-2.5 text-sm font-medium text-foreground shadow-xs transition-colors focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
            >
              <option value="" disabled>Seleccione un colaborador</option>
              <option *ngFor="let emp of employeeOptions" [value]="emp.id">
                {{ emp.name }} ({{ emp.code }})
              </option>
            </select>
          </div>

          <!-- Estado del día del colaborador -->
          <div *ngIf="todayRecord()" class="mt-2 text-xs flex items-center gap-2 text-muted-foreground">
            <span class="inline-block w-2 h-2 rounded-full" [ngClass]="todayRecord()?.checkOutTime ? 'bg-indigo-500' : 'bg-emerald-500'"></span>
            <span>
              Entrada: <strong>{{ formatTime(todayRecord()?.checkInTime) }}</strong>
              <ng-container *ngIf="todayRecord()?.checkOutTime">
                | Salida: <strong>{{ formatTime(todayRecord()?.checkOutTime) }}</strong>
              </ng-container>
            </span>
          </div>
        </div>

        <!-- Right: Botones de Marcación & Debounce -->
        <div class="flex flex-col items-stretch sm:items-end gap-3 min-w-[320px]">
          <div class="grid grid-cols-2 sm:flex sm:flex-wrap items-center gap-2 w-full">
            <!-- Botón Entrada -->
            <button
              (click)="punch('IN')"
              [disabled]="!selectedEmployeeId || clockUseCase.clocking() || clockUseCase.isDebounced()"
              class="inline-flex items-center justify-center gap-1.5 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 px-3.5 py-2.5 text-xs font-bold text-white shadow-sm hover:from-emerald-500 hover:to-teal-500 disabled:opacity-50 disabled:cursor-not-allowed transition-all transform active:scale-95"
              title="Registrar inicio de jornada laboral"
            >
              <lucide-icon *ngIf="!clockUseCase.clocking()" name="log-in" class="h-3.5 w-3.5"></lucide-icon>
              <lucide-icon *ngIf="clockUseCase.clocking()" name="loader-circle" class="h-3.5 w-3.5 animate-spin"></lucide-icon>
              Entrada
            </button>

            <!-- Botón Pausa Almuerzo -->
            <button
              (click)="punch('BREAK_START')"
              [disabled]="!selectedEmployeeId || clockUseCase.clocking() || clockUseCase.isDebounced()"
              class="inline-flex items-center justify-center gap-1.5 rounded-xl bg-gradient-to-r from-amber-600 to-yellow-600 px-3.5 py-2.5 text-xs font-bold text-white shadow-sm hover:from-amber-500 hover:to-yellow-500 disabled:opacity-50 disabled:cursor-not-allowed transition-all transform active:scale-95"
              title="Registrar inicio de pausa o almuerzo"
            >
              <lucide-icon *ngIf="!clockUseCase.clocking()" name="coffee" class="h-3.5 w-3.5"></lucide-icon>
              <lucide-icon *ngIf="clockUseCase.clocking()" name="loader-circle" class="h-3.5 w-3.5 animate-spin"></lucide-icon>
              Pausa
            </button>

            <!-- Botón Fin Pausa -->
            <button
              (click)="punch('BREAK_END')"
              [disabled]="!selectedEmployeeId || clockUseCase.clocking() || clockUseCase.isDebounced()"
              class="inline-flex items-center justify-center gap-1.5 rounded-xl bg-gradient-to-r from-purple-600 to-indigo-600 px-3.5 py-2.5 text-xs font-bold text-white shadow-sm hover:from-purple-500 hover:to-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed transition-all transform active:scale-95"
              title="Registrar fin de pausa y retorno al puesto"
            >
              <lucide-icon *ngIf="!clockUseCase.clocking()" name="play" class="h-3.5 w-3.5"></lucide-icon>
              <lucide-icon *ngIf="clockUseCase.clocking()" name="loader-circle" class="h-3.5 w-3.5 animate-spin"></lucide-icon>
              Retorno
            </button>

            <!-- Botón Salida -->
            <button
              (click)="punch('OUT')"
              [disabled]="!selectedEmployeeId || clockUseCase.clocking() || clockUseCase.isDebounced()"
              class="inline-flex items-center justify-center gap-1.5 rounded-xl bg-gradient-to-r from-rose-600 to-red-600 px-3.5 py-2.5 text-xs font-bold text-white shadow-sm hover:from-rose-500 hover:to-red-500 disabled:opacity-50 disabled:cursor-not-allowed transition-all transform active:scale-95"
              title="Registrar cierre de jornada laboral"
            >
              <lucide-icon *ngIf="!clockUseCase.clocking()" name="log-out" class="h-3.5 w-3.5"></lucide-icon>
              <lucide-icon *ngIf="clockUseCase.clocking()" name="loader-circle" class="h-3.5 w-3.5 animate-spin"></lucide-icon>
              Salida
            </button>
          </div>

          <!-- Banner de Bloqueo Preventivo por Debounce (2 min) -->
          <div
            *ngIf="clockUseCase.isDebounced()"
            class="w-full flex items-center justify-between gap-3 rounded-lg bg-amber-500/10 border border-amber-500/30 px-3 py-1.5 text-xs text-amber-700 dark:text-amber-400"
          >
            <div class="flex items-center gap-1.5 font-medium">
              <lucide-icon name="alert-triangle" class="h-3.5 w-3.5"></lucide-icon>
              <span>Espere <strong>{{ clockUseCase.remainingDebounceSeconds() }}s</strong> para volver a marcar</span>
            </div>
            <span class="font-mono font-bold text-[11px]">Anti-doble marcación</span>
          </div>
        </div>

      </div>
    </div>
  `
})
export class ClockInWidgetComponent implements OnInit, OnDestroy {
  readonly clockUseCase = inject(AttendanceClockUseCase);
  private readonly employeesService = inject(EmployeesService);

  @Output() clockRegistered = new EventEmitter<AttendanceRecordResponse>();

  readonly currentTime = signal<string>('00:00:00');
  readonly currentDate = signal<string>('');
  readonly todayRecord = signal<AttendanceRecordResponse | null>(null);

  employeeOptions: ClockEmployeeOption[] = [];
  selectedEmployeeId = '';

  private clockInterval: any = null;

  async ngOnInit(): Promise<void> {
    this.updateClock();
    this.clockInterval = setInterval(() => this.updateClock(), 1000);
    await this.loadEmployees();
  }

  private async loadEmployees(): Promise<void> {
    try {
      const response = await firstValueFrom(this.employeesService.list(0, 100, { status: 'ACTIVE' }));
      this.employeeOptions = (response.data?.content || []).map((employee) => ({
        id: employee.id,
        name: `${employee.firstName} ${employee.lastName}`.trim(),
        code: employee.employeeNumber
      }));
      this.selectedEmployeeId = this.employeeOptions[0]?.id || '';
      if (this.selectedEmployeeId) await this.onEmployeeChange();
    } catch {
      this.employeeOptions = [];
      this.selectedEmployeeId = '';
    }
  }

  ngOnDestroy(): void {
    if (this.clockInterval) {
      clearInterval(this.clockInterval);
    }
  }

  private updateClock(): void {
    const now = new Date();
    this.currentTime.set(
      now.toLocaleTimeString('es-BO', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
      })
    );
    this.currentDate.set(
      now.toLocaleDateString('es-BO', {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      })
    );
  }

  async onEmployeeChange(): Promise<void> {
    if (!this.selectedEmployeeId) {
      this.todayRecord.set(null);
      return;
    }
    const record = await this.clockUseCase.fetchTodayRecord(this.selectedEmployeeId);
    this.todayRecord.set(record);
  }

  async punch(type: 'IN' | 'OUT' | 'BREAK_START' | 'BREAK_END'): Promise<void> {
    const result = await this.clockUseCase.registerClock(this.selectedEmployeeId, type);
    if (result) {
      this.todayRecord.set(result);
      this.clockRegistered.emit(result);
    }
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
}
