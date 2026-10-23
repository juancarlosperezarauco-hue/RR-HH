import {
  Component,
  OnInit,
  OnDestroy,
  OnChanges,
  SimpleChanges,
  Input,
  Output,
  EventEmitter,
  inject,
  signal,
  computed
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { AttendanceClockUseCase } from '../../application/attendance-clock.usecase';
import { AttendanceRecordResponse, AttendanceService } from '../../../../entities/attendance';
import { EmployeesService, EmployeeSummary } from '../../../../entities/employees';
import { ToastService } from '../../../../shared/ui/toast/toast.service';
import { firstValueFrom } from 'rxjs';

export interface ClockEmployeeOption {
  id: string;
  name: string;
  code: string;
}

export type PunchSequenceState = 'NONE' | 'IN' | 'BREAK_START' | 'BREAK_END' | 'OUT';

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

          <!-- Estado de marcación del colaborador -->
          <div *ngIf="selectedEmployeeId" class="mt-2 text-xs flex items-center gap-2">
            <span class="inline-block w-2.5 h-2.5 rounded-full shrink-0" [ngClass]="statusColorClass()"></span>
            <span class="font-medium text-foreground">{{ statusLabel() }}</span>
          </div>
          <p *ngIf="employeeOptions.length === 0" class="text-[11px] text-amber-600 dark:text-amber-400 mt-1">
            No se encontraron colaboradores activos registrados.
          </p>
        </div>

        <!-- Right: Botones de Marcación & Debounce -->
        <div class="flex flex-col items-stretch sm:items-end gap-3 min-w-[320px]">
          <div class="grid grid-cols-2 sm:flex sm:flex-wrap items-center gap-2 w-full">
            <!-- Botón Entrada -->
            <button
              type="button"
              (click)="punch('IN')"
              [disabled]="!canPunchIn()"
              class="inline-flex items-center justify-center gap-1.5 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 px-3.5 py-2.5 text-xs font-bold text-white shadow-sm hover:from-emerald-500 hover:to-teal-500 disabled:opacity-40 disabled:cursor-not-allowed transition-all transform active:scale-95"
              title="Registrar inicio de jornada laboral (requiere no haber marcado entrada)"
            >
              <lucide-icon *ngIf="!clockUseCase.clocking()" name="log-in" class="h-3.5 w-3.5"></lucide-icon>
              <lucide-icon *ngIf="clockUseCase.clocking()" name="loader-circle" class="h-3.5 w-3.5 animate-spin"></lucide-icon>
              Entrada
            </button>

            <!-- Botón Pausa Almuerzo -->
            <button
              type="button"
              (click)="punch('BREAK_START')"
              [disabled]="!canPunchBreakStart()"
              class="inline-flex items-center justify-center gap-1.5 rounded-xl bg-gradient-to-r from-amber-600 to-yellow-600 px-3.5 py-2.5 text-xs font-bold text-white shadow-sm hover:from-amber-500 hover:to-yellow-500 disabled:opacity-40 disabled:cursor-not-allowed transition-all transform active:scale-95"
              title="Registrar inicio de pausa o almuerzo (requiere entrada activa)"
            >
              <lucide-icon *ngIf="!clockUseCase.clocking()" name="coffee" class="h-3.5 w-3.5"></lucide-icon>
              <lucide-icon *ngIf="clockUseCase.clocking()" name="loader-circle" class="h-3.5 w-3.5 animate-spin"></lucide-icon>
              Pausa
            </button>

            <!-- Botón Fin Pausa -->
            <button
              type="button"
              (click)="punch('BREAK_END')"
              [disabled]="!canPunchBreakEnd()"
              class="inline-flex items-center justify-center gap-1.5 rounded-xl bg-gradient-to-r from-purple-600 to-indigo-600 px-3.5 py-2.5 text-xs font-bold text-white shadow-sm hover:from-purple-500 hover:to-indigo-500 disabled:opacity-40 disabled:cursor-not-allowed transition-all transform active:scale-95"
              title="Registrar fin de pausa y retorno al puesto (requiere pausa previa)"
            >
              <lucide-icon *ngIf="!clockUseCase.clocking()" name="play" class="h-3.5 w-3.5"></lucide-icon>
              <lucide-icon *ngIf="clockUseCase.clocking()" name="loader-circle" class="h-3.5 w-3.5 animate-spin"></lucide-icon>
              Retorno
            </button>

            <!-- Botón Salida -->
            <button
              type="button"
              (click)="punch('OUT')"
              [disabled]="!canPunchOut()"
              class="inline-flex items-center justify-center gap-1.5 rounded-xl bg-gradient-to-r from-rose-600 to-red-600 px-3.5 py-2.5 text-xs font-bold text-white shadow-sm hover:from-rose-500 hover:to-red-500 disabled:opacity-40 disabled:cursor-not-allowed transition-all transform active:scale-95"
              title="Registrar cierre de jornada laboral (requiere entrada activa y no estar en pausa)"
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
export class ClockInWidgetComponent implements OnInit, OnDestroy, OnChanges {
  @Input() employees?: EmployeeSummary[];
  @Output() clockRegistered = new EventEmitter<AttendanceRecordResponse>();

  readonly clockUseCase = inject(AttendanceClockUseCase);
  private readonly employeesService = inject(EmployeesService);
  private readonly attendanceService = inject(AttendanceService);
  private readonly toastService = inject(ToastService);

  readonly currentTime = signal<string>('00:00:00');
  readonly currentDate = signal<string>('');
  readonly todayRecord = signal<AttendanceRecordResponse | null>(null);
  readonly lastPunchState = signal<PunchSequenceState>('NONE');
  readonly lastPunchTime = signal<string | null>(null);

  employeeOptions: ClockEmployeeOption[] = [];
  selectedEmployeeId = '';

  private clockInterval: any = null;

  async ngOnInit(): Promise<void> {
    this.updateClock();
    this.clockInterval = setInterval(() => this.updateClock(), 1000);
    if (!this.employees || this.employees.length === 0) {
      await this.loadEmployees();
    } else {
      this.populateEmployeeOptions(this.employees);
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['employees'] && this.employees) {
      this.populateEmployeeOptions(this.employees);
    }
  }

  ngOnDestroy(): void {
    if (this.clockInterval) {
      clearInterval(this.clockInterval);
    }
  }

  private populateEmployeeOptions(list: EmployeeSummary[]): void {
    this.employeeOptions = list.map((employee) => ({
      id: employee.id,
      name: `${employee.firstName} ${employee.lastName}`.trim(),
      code: employee.employeeNumber
    }));
    // No seleccionar automáticamente: evita marcar por accidente al primer colaborador.
  }

  async loadEmployees(): Promise<void> {
    try {
      const response = await firstValueFrom(this.employeesService.list(0, 100, { status: 'ACTIVE' }));
      const list: EmployeeSummary[] = Array.isArray(response.data)
        ? (response.data as EmployeeSummary[])
        : (response.data?.content || []);
      this.populateEmployeeOptions(list);
    } catch {
      this.employeeOptions = [];
      this.selectedEmployeeId = '';
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
      this.lastPunchState.set('NONE');
      this.lastPunchTime.set(null);
      return;
    }
    await this.refreshEmployeeState();
  }

  async refreshEmployeeState(): Promise<void> {
    if (!this.selectedEmployeeId) return;

    // 1. Obtener registro de jornada de hoy
    const record = await this.clockUseCase.fetchTodayRecord(this.selectedEmployeeId);
    this.todayRecord.set(record);

    // 2. Determinar estado de la secuencia de marcaciones consultando las más recientes
    try {
      const punchesRes = await firstValueFrom(
        this.attendanceService.listPunches({
          employeeId: this.selectedEmployeeId,
          from: this.startOfLocalDay().toISOString(),
          to: this.startOfLocalDay(1).toISOString(),
          size: 10,
          sort: 'punchedAt',
          direction: 'desc'
        })
      );
      const punches = Array.isArray(punchesRes?.data) ? punchesRes.data : (punchesRes?.data?.content || []);

      if (punches.length > 0) {
        // Con direction: 'desc', el primer elemento es la última marcación registrada
        const latest = punches[0];
        this.lastPunchState.set(latest.punchType as PunchSequenceState);
        this.lastPunchTime.set(this.formatTime(latest.punchedAt));
      } else if (record?.checkOutTime) {
        this.lastPunchState.set('OUT');
        this.lastPunchTime.set(this.formatTime(record.checkOutTime));
      } else if (record?.checkInTime) {
        this.lastPunchState.set('IN');
        this.lastPunchTime.set(this.formatTime(record.checkInTime));
      } else {
        this.lastPunchState.set('NONE');
        this.lastPunchTime.set(null);
      }
    } catch {
      this.lastPunchState.set('NONE');
      this.lastPunchTime.set(null);
    }
  }

  private startOfLocalDay(offsetDays = 0): Date {
    const date = new Date();
    date.setHours(0, 0, 0, 0);
    date.setDate(date.getDate() + offsetDays);
    return date;
  }

  canPunchIn(): boolean {
    if (!this.selectedEmployeeId || this.clockUseCase.clocking() || this.clockUseCase.isDebouncedFor(this.selectedEmployeeId, 'IN')) return false;
    return this.lastPunchState() === 'NONE' || this.lastPunchState() === 'OUT';
  }

  canPunchBreakStart(): boolean {
    if (!this.selectedEmployeeId || this.clockUseCase.clocking() || this.clockUseCase.isDebouncedFor(this.selectedEmployeeId, 'BREAK_START')) return false;
    return this.lastPunchState() === 'IN' || this.lastPunchState() === 'BREAK_END';
  }

  canPunchBreakEnd(): boolean {
    if (!this.selectedEmployeeId || this.clockUseCase.clocking() || this.clockUseCase.isDebouncedFor(this.selectedEmployeeId, 'BREAK_END')) return false;
    return this.lastPunchState() === 'BREAK_START';
  }

  canPunchOut(): boolean {
    if (!this.selectedEmployeeId || this.clockUseCase.clocking() || this.clockUseCase.isDebouncedFor(this.selectedEmployeeId, 'OUT')) return false;
    return this.lastPunchState() === 'IN' || this.lastPunchState() === 'BREAK_END';
  }

  statusLabel(): string {
    const time = this.lastPunchTime();
    switch (this.lastPunchState()) {
      case 'IN':
        return `En jornada laboral (Entrada: ${time || '--:--'})`;
      case 'BREAK_START':
        return `En descanso / pausa (Iniciada: ${time || '--:--'})`;
      case 'BREAK_END':
        return `En jornada laboral (Retorno de pausa: ${time || '--:--'})`;
      case 'OUT':
        return `Jornada concluida (Salida: ${time || '--:--'})`;
      case 'NONE':
      default:
        return 'Sin marcación hoy · Esperando Entrada';
    }
  }

  statusColorClass(): string {
    switch (this.lastPunchState()) {
      case 'IN':
      case 'BREAK_END':
        return 'bg-emerald-500 shadow-sm shadow-emerald-500/50';
      case 'BREAK_START':
        return 'bg-amber-500 shadow-sm shadow-amber-500/50';
      case 'OUT':
        return 'bg-indigo-500 shadow-sm shadow-indigo-500/50';
      case 'NONE':
      default:
        return 'bg-muted-foreground/40';
    }
  }

  async punch(type: 'IN' | 'OUT' | 'BREAK_START' | 'BREAK_END'): Promise<void> {
    if (!this.selectedEmployeeId) {
      this.toastService.warning('Seleccione un colaborador antes de marcar.');
      return;
    }

    // Validación preventiva en cliente para mejorar la experiencia de usuario
    if (type === 'IN' && !this.canPunchIn()) {
      this.toastService.warning('El colaborador ya registró su entrada para esta jornada.');
      return;
    }
    if (type === 'OUT' && !this.canPunchOut()) {
      if (this.lastPunchState() === 'BREAK_START') {
        this.toastService.warning('El colaborador está en pausa. Debe registrar el retorno antes de marcar salida.');
      } else if (this.lastPunchState() === 'NONE') {
        this.toastService.warning('Debe registrar la entrada antes de poder marcar la salida.');
      } else {
        this.toastService.warning('La jornada ya fue finalizada para este colaborador.');
      }
      return;
    }
    if (type === 'BREAK_START' && !this.canPunchBreakStart()) {
      if (this.lastPunchState() === 'NONE') {
        this.toastService.warning('Debe registrar la entrada antes de iniciar una pausa.');
      } else if (this.lastPunchState() === 'BREAK_START') {
        this.toastService.warning('El colaborador ya se encuentra en pausa.');
      } else {
        this.toastService.warning('No se puede iniciar pausa en el estado actual.');
      }
      return;
    }
    if (type === 'BREAK_END' && !this.canPunchBreakEnd()) {
      this.toastService.warning('Debe iniciar una pausa antes de registrar el retorno.');
      return;
    }

    const result = await this.clockUseCase.registerClock(this.selectedEmployeeId, type);
    if (result) {
      await this.refreshEmployeeState();
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
