import {
  Component,
  OnInit,
  OnDestroy,
  inject,
  signal,
  Output,
  EventEmitter,
  ChangeDetectorRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { AttendanceClockUseCase } from '../../application/attendance-clock.usecase';
import { AttendanceListUseCase } from '../../application/attendance-list.usecase';
import { AttendanceRecordResponse } from '../../../../entities/attendance';
import { EmployeesService } from '../../../../entities/employees';
import { ToastService } from '../../../../shared/ui/toast/toast.service';
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
        <div class="space-y-2 shrink-0">
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

        <!-- Right: Sección de Operación de Asistencia -->
        <div class="flex-1 max-w-xl flex flex-col gap-3.5">
          
          <!-- Fila Superior: Colaborador en Marcación a la izquierda y Marcación Manual a la derecha -->
          <div class="flex flex-col sm:flex-row sm:items-end justify-between gap-3">
            <div class="flex-1 max-w-sm">
              <label class="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                Colaborador en Marcación
              </label>
              <select
                [(ngModel)]="selectedEmployeeId"
                (ngModelChange)="onEmployeeChange()"
                class="w-full rounded-xl border border-border bg-background px-3.5 py-2 text-sm font-medium text-foreground shadow-xs transition-colors focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
              >
                <option value="" disabled>Seleccione un colaborador</option>
                <option *ngFor="let emp of employeeOptions" [value]="emp.id">
                  {{ emp.name }} ({{ emp.code }})
                </option>
              </select>
              
              <!-- Estado del día del colaborador -->
              <div class="mt-1.5 text-xs flex items-center gap-1.5 text-muted-foreground">
                <span
                  class="inline-block w-2 h-2 rounded-full"
                  [ngClass]="{
                    'bg-slate-400': clockUseCase.employeePunchState() === 'NONE',
                    'bg-emerald-500 animate-pulse': clockUseCase.employeePunchState() === 'INSIDE',
                    'bg-amber-500': clockUseCase.employeePunchState() === 'ON_BREAK',
                    'bg-indigo-500': clockUseCase.employeePunchState() === 'OUTSIDE'
                  }"
                ></span>
                <span *ngIf="clockUseCase.employeePunchState() === 'NONE'" class="italic">
                  Sin marcaciones registradas hoy
                </span>
                <span *ngIf="clockUseCase.employeePunchState() === 'INSIDE'">
                  Activo en turno · Entrada: <strong>{{ formatTime(todayRecord()?.checkInTime) }}</strong>
                </span>
                <span *ngIf="clockUseCase.employeePunchState() === 'ON_BREAK'" class="text-amber-600 dark:text-amber-400 font-medium">
                  En pausa / refrigerio
                </span>
                <span *ngIf="clockUseCase.employeePunchState() === 'OUTSIDE'">
                  Salida registrada: <strong>{{ formatTime(todayRecord()?.checkOutTime) }}</strong> · Fuera de turno
                </span>
              </div>
            </div>

            <!-- Botón Marcación Manual a la derecha de Colaboradores (un solo '+', sin 'retroactiva') -->
            <div class="shrink-0 pb-0.5">
              <button
                type="button"
                (click)="isManualModalOpen = true"
                [disabled]="!selectedEmployeeId"
                class="inline-flex items-center gap-1.5 rounded-xl border border-primary/30 bg-primary/10 px-3.5 py-2 text-xs font-bold text-primary hover:bg-primary/20 transition-all shadow-xs disabled:opacity-50 disabled:cursor-not-allowed"
                title="Registrar marcación manual para el colaborador"
              >
                <lucide-icon name="plus" class="h-3.5 w-3.5"></lucide-icon>
                <span>Marcación Manual</span>
              </button>
            </div>
          </div>

          <!-- Fila Inferior: Botones de Marcación debajo de Colaborador y Marcación Manual -->
          <div class="grid grid-cols-2 sm:grid-cols-4 gap-2 pt-2 border-t border-border/50">
            <!-- Botón Entrada -->
            <button
              (click)="punch('IN')"
              [disabled]="!selectedEmployeeId || clockUseCase.clocking() || clockUseCase.isDebounced() || clockUseCase.employeePunchState() === 'INSIDE' || clockUseCase.employeePunchState() === 'ON_BREAK'"
              class="inline-flex items-center justify-center gap-1.5 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 px-3 py-2 text-xs font-bold text-white shadow-sm hover:from-emerald-500 hover:to-teal-500 disabled:opacity-50 disabled:cursor-not-allowed transition-all transform active:scale-95"
              [title]="clockUseCase.employeePunchState() === 'INSIDE' ? 'El colaborador ya se encuentra en turno' : (clockUseCase.employeePunchState() === 'ON_BREAK' ? 'Actualmente en pausa' : 'Registrar inicio de jornada laboral')"
            >
              <lucide-icon *ngIf="!clockUseCase.clocking()" name="log-in" class="h-3.5 w-3.5"></lucide-icon>
              <lucide-icon *ngIf="clockUseCase.clocking()" name="loader-circle" class="h-3.5 w-3.5 animate-spin"></lucide-icon>
              Entrada
            </button>

            <!-- Botón Pausa -->
            <button
              (click)="punch('BREAK_START')"
              [disabled]="!selectedEmployeeId || clockUseCase.clocking() || clockUseCase.isDebounced() || clockUseCase.employeePunchState() !== 'INSIDE'"
              class="inline-flex items-center justify-center gap-1.5 rounded-xl bg-gradient-to-r from-amber-600 to-yellow-600 px-3 py-2 text-xs font-bold text-white shadow-sm hover:from-amber-500 hover:to-yellow-500 disabled:opacity-50 disabled:cursor-not-allowed transition-all transform active:scale-95"
              [title]="clockUseCase.employeePunchState() === 'INSIDE' ? 'Registrar inicio de pausa o almuerzo' : (clockUseCase.employeePunchState() === 'ON_BREAK' ? 'Ya se encuentra en pausa' : 'Debe marcar primero su entrada antes de iniciar una pausa')"
            >
              <lucide-icon *ngIf="!clockUseCase.clocking()" name="coffee" class="h-3.5 w-3.5"></lucide-icon>
              <lucide-icon *ngIf="clockUseCase.clocking()" name="loader-circle" class="h-3.5 w-3.5 animate-spin"></lucide-icon>
              Pausa
            </button>

            <!-- Botón Retorno -->
            <button
              (click)="punch('BREAK_END')"
              [disabled]="!selectedEmployeeId || clockUseCase.clocking() || clockUseCase.isDebounced() || clockUseCase.employeePunchState() !== 'ON_BREAK'"
              class="inline-flex items-center justify-center gap-1.5 rounded-xl bg-gradient-to-r from-purple-600 to-indigo-600 px-3 py-2 text-xs font-bold text-white shadow-sm hover:from-purple-500 hover:to-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed transition-all transform active:scale-95"
              [title]="clockUseCase.employeePunchState() === 'ON_BREAK' ? 'Registrar fin de pausa y retorno al trabajo' : 'Solo disponible si se encuentra en pausa'"
            >
              <lucide-icon *ngIf="!clockUseCase.clocking()" name="play" class="h-3.5 w-3.5"></lucide-icon>
              <lucide-icon *ngIf="clockUseCase.clocking()" name="loader-circle" class="h-3.5 w-3.5 animate-spin"></lucide-icon>
              Retorno
            </button>

            <!-- Botón Salida -->
            <button
              (click)="punch('OUT')"
              [disabled]="!selectedEmployeeId || clockUseCase.clocking() || clockUseCase.isDebounced() || clockUseCase.employeePunchState() !== 'INSIDE'"
              class="inline-flex items-center justify-center gap-1.5 rounded-xl bg-gradient-to-r from-rose-600 to-red-600 px-3 py-2 text-xs font-bold text-white shadow-sm hover:from-rose-500 hover:to-red-500 disabled:opacity-50 disabled:cursor-not-allowed transition-all transform active:scale-95"
              [title]="clockUseCase.employeePunchState() === 'INSIDE' ? 'Registrar salida de jornada laboral' : (clockUseCase.employeePunchState() === 'ON_BREAK' ? 'Debe finalizar primero la pausa (Retorno) antes de marcar la salida' : 'Este usuario no tiene una entrada registrada. Debe marcar primero su entrada antes de registrar la salida.')"
            >
              <lucide-icon *ngIf="!clockUseCase.clocking()" name="log-out" class="h-3.5 w-3.5"></lucide-icon>
              <lucide-icon *ngIf="clockUseCase.clocking()" name="loader-circle" class="h-3.5 w-3.5 animate-spin"></lucide-icon>
              Salida
            </button>
          </div>

          <!-- Banner de Bloqueo Preventivo por Debounce si está activo -->
          <div
            *ngIf="clockUseCase.isDebounced()"
            class="flex items-center justify-between rounded-lg bg-amber-500/10 border border-amber-500/30 px-3 py-1 text-xs text-amber-700 dark:text-amber-400"
          >
            <div class="flex items-center gap-1.5">
              <lucide-icon name="alert-triangle" class="h-3.5 w-3.5"></lucide-icon>
              <span>Anti-doble marcación activa ({{ clockUseCase.remainingDebounceSeconds() }}s)</span>
            </div>
          </div>

        </div>

      </div>

      <!-- Modal de Marcación Manual -->
      <div
        *ngIf="isManualModalOpen"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-in fade-in duration-200"
      >
        <div
          class="w-full max-w-md rounded-2xl border border-border bg-card p-6 shadow-2xl transition-all"
          (click)="$event.stopPropagation()"
        >
          <div class="flex items-start justify-between border-b border-border pb-3">
            <div>
              <h3 class="text-base font-bold text-foreground">Registro de Marcación Manual</h3>
              <p class="text-xs text-muted-foreground mt-0.5">
                Para marcaciones omitidas o justificadas administrativamente
              </p>
            </div>
            <button
              type="button"
              (click)="isManualModalOpen = false"
              class="rounded-lg p-1 text-muted-foreground hover:bg-muted hover:text-foreground"
            >
              <lucide-icon name="x" class="h-4 w-4"></lucide-icon>
            </button>
          </div>

          <form (ngSubmit)="submitManualPunch()" class="mt-4 space-y-3.5">
            <div>
              <label class="block text-xs font-semibold text-foreground mb-1">Tipo de Marcación</label>
              <select
                [(ngModel)]="manualType"
                name="manualType"
                class="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:ring-1 focus:ring-primary"
              >
                <option value="IN">Entrada (IN)</option>
                <option value="OUT">Salida (OUT)</option>
                <option value="BREAK_START">Inicio de Pausa (BREAK_START)</option>
                <option value="BREAK_END">Fin de Pausa / Retorno (BREAK_END)</option>
              </select>
            </div>

            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="block text-xs font-semibold text-foreground mb-1">Fecha</label>
                <input
                  type="date"
                  [(ngModel)]="manualDate"
                  name="manualDate"
                  required
                  class="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:ring-1 focus:ring-primary"
                />
              </div>
              <div>
                <label class="block text-xs font-semibold text-foreground mb-1">Hora</label>
                <input
                  type="time"
                  [(ngModel)]="manualTime"
                  name="manualTime"
                  required
                  class="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:ring-1 focus:ring-primary"
                />
              </div>
            </div>

            <div>
              <div class="flex items-center justify-between mb-1">
                <label class="text-xs font-semibold text-foreground">
                  Motivo / Justificación <span class="text-rose-500">*</span>
                </label>
                <span class="text-[11px] font-mono text-muted-foreground">
                  {{ manualNotes.trim().length }}/5 mín.
                </span>
              </div>
              <textarea
                [(ngModel)]="manualNotes"
                name="manualNotes"
                rows="2"
                placeholder="Obligatorio para marcaciones manuales (ej. Olvido de credencial, ingreso por puerta auxiliar)"
                class="w-full rounded-xl border border-border bg-background p-2.5 text-xs text-foreground placeholder:text-muted-foreground focus:border-primary focus:ring-1 focus:ring-primary"
                required
              ></textarea>
            </div>

            <div class="flex items-center justify-end gap-2 pt-2">
              <button
                type="button"
                (click)="isManualModalOpen = false"
                class="rounded-xl border border-border px-4 py-2 text-xs font-semibold text-foreground hover:bg-accent"
              >
                Cancelar
              </button>
              <button
                type="submit"
                [disabled]="clockUseCase.clocking() || manualNotes.trim().length < 5"
                class="rounded-xl bg-primary px-4 py-2 text-xs font-bold text-primary-foreground hover:bg-primary/90 disabled:opacity-50 shadow-xs"
              >
                Guardar Marcación
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  `
})
export class ClockInWidgetComponent implements OnInit, OnDestroy {
  readonly clockUseCase = inject(AttendanceClockUseCase);
  readonly listUseCase = inject(AttendanceListUseCase);
  private readonly employeesService = inject(EmployeesService);
  private readonly toastService = inject(ToastService);
  private readonly cdr = inject(ChangeDetectorRef);

  @Output() clockRegistered = new EventEmitter<AttendanceRecordResponse>();

  readonly currentTime = signal<string>('00:00:00');
  readonly currentDate = signal<string>('');
  readonly todayRecord = signal<AttendanceRecordResponse | null>(null);

  employeeOptions: ClockEmployeeOption[] = [];
  selectedEmployeeId = '';

  isManualModalOpen = false;
  manualType: 'IN' | 'OUT' | 'BREAK_START' | 'BREAK_END' = 'IN';
  manualDate = new Date().toLocaleDateString('en-CA');
  manualTime = '08:30';
  manualNotes = '';

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

  hasEntryToday(): boolean {
    return this.clockUseCase.employeePunchState() !== 'NONE';
  }

  hasExitToday(): boolean {
    return this.clockUseCase.employeePunchState() === 'OUTSIDE';
  }

  async onEmployeeChange(): Promise<void> {
    if (!this.selectedEmployeeId) {
      this.todayRecord.set(null);
      return;
    }
    const record = await this.clockUseCase.fetchTodayRecord(this.selectedEmployeeId);
    this.todayRecord.set(record);
    this.cdr.markForCheck();
  }

  async punch(type: 'IN' | 'OUT' | 'BREAK_START' | 'BREAK_END'): Promise<void> {
    if (!this.selectedEmployeeId) {
      this.toastService.warning('Seleccione un colaborador antes de marcar.');
      return;
    }

    const state = this.clockUseCase.employeePunchState();

    if (type === 'IN' && (state === 'INSIDE' || state === 'ON_BREAK')) {
      this.toastService.warning('El colaborador ya se encuentra en turno activo.');
      return;
    }

    if (type === 'BREAK_START' && state !== 'INSIDE') {
      this.toastService.warning('Debe registrar una entrada antes de iniciar una pausa.');
      return;
    }

    if (type === 'BREAK_END' && state !== 'ON_BREAK') {
      this.toastService.warning('Solo puede marcar retorno si se encuentra en pausa.');
      return;
    }

    if (type === 'OUT' && state !== 'INSIDE') {
      if (state === 'ON_BREAK') {
        this.toastService.warning('Debe finalizar la pausa (marcar Retorno) antes de registrar la salida.');
      } else {
        this.toastService.warning('Debe marcar primero su entrada antes de registrar la salida.');
      }
      return;
    }

    const result = await this.clockUseCase.registerClock(this.selectedEmployeeId, type);
    if (result) {
      await this.onEmployeeChange();
      await this.listUseCase.loadRecords();
      this.clockRegistered.emit(result);
      this.cdr.markForCheck();
    }
  }

  async submitManualPunch(): Promise<void> {
    if (!this.selectedEmployeeId) return;

    if (this.manualNotes.trim().length < 5) return;
    const time = this.manualTime.length === 5 ? `${this.manualTime}:00` : this.manualTime;
    const isoString = new Date(`${this.manualDate}T${time}`).toISOString();

    const result = await this.clockUseCase.registerManualClock(
      this.selectedEmployeeId,
      this.manualType,
      isoString,
      this.manualNotes.trim()
    );

    if (result) {
      this.isManualModalOpen = false;
      this.manualNotes = '';
      await this.onEmployeeChange();
      await this.listUseCase.loadRecords();
      this.clockRegistered.emit(result);
      this.cdr.markForCheck();
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
