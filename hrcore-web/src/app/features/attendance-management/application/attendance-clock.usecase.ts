import { inject, Injectable, signal, computed } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AttendanceService, AttendanceRecordResponse } from '../../../entities/attendance';
import { ToastService } from '../../../shared/ui/toast/toast.service';

@Injectable({ providedIn: 'root' })
export class AttendanceClockUseCase {
  private readonly attendanceService = inject(AttendanceService);
  private readonly toastService = inject(ToastService);

  private readonly DEBOUNCE_KEY = 'hrcore_last_clock_timestamp';
  private readonly DEBOUNCE_MS = 10 * 1000; // 10 segundos anti-doble clic rápido
  private debounceTimerInterval: any = null;

  readonly clocking = signal<boolean>(false);
  readonly error = signal<string | null>(null);
  readonly remainingDebounceSeconds = signal<number>(0);
  readonly lastPunchResult = signal<AttendanceRecordResponse | null>(null);
  readonly gpsAvailable = signal<boolean | null>(null);

  readonly isDebounced = computed(() => this.remainingDebounceSeconds() > 0);

  constructor() {
    this.checkDebounceTimer();
  }

  checkDebounceTimer(): void {
    if (typeof window === 'undefined' || !window.localStorage) return;
    
    const lastTimestamp = Number(localStorage.getItem(this.DEBOUNCE_KEY) || 0);
    const elapsed = Date.now() - lastTimestamp;

    if (this.debounceTimerInterval) {
      clearInterval(this.debounceTimerInterval);
      this.debounceTimerInterval = null;
    }

    if (elapsed < this.DEBOUNCE_MS) {
      this.remainingDebounceSeconds.set(Math.ceil((this.DEBOUNCE_MS - elapsed) / 1000));
      this.debounceTimerInterval = setInterval(() => {
        const remaining = this.remainingDebounceSeconds() - 1;
        if (remaining <= 0) {
          this.remainingDebounceSeconds.set(0);
          clearInterval(this.debounceTimerInterval);
          this.debounceTimerInterval = null;
        } else {
          this.remainingDebounceSeconds.set(remaining);
        }
      }, 1000);
    } else {
      this.remainingDebounceSeconds.set(0);
    }
  }

  async captureCoordinates(): Promise<{ latitude?: number; longitude?: number }> {
    if (typeof window === 'undefined' || !('geolocation' in navigator)) {
      this.gpsAvailable.set(false);
      return {};
    }

    return new Promise((resolve) => {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          this.gpsAvailable.set(true);
          resolve({
            latitude: Number(pos.coords.latitude.toFixed(7)),
            longitude: Number(pos.coords.longitude.toFixed(7))
          });
        },
        () => {
          // Fallback silencioso y amigable sin romper la experiencia si se deniegan permisos
          this.gpsAvailable.set(false);
          resolve({});
        },
        { timeout: 4000, enableHighAccuracy: true, maximumAge: 30000 }
      );
    });
  }

  async registerClock(
    employeeId: string,
    punchType: 'IN' | 'OUT' | 'BREAK_START' | 'BREAK_END' | 'BREAK_OUT' | 'BREAK_IN',
    notes?: string
  ): Promise<AttendanceRecordResponse | null> {
    if (this.isDebounced()) {
      this.toastService.warning(
        `Protección activa: Espere ${this.remainingDebounceSeconds()}s antes de volver a marcar.`
      );
      return null;
    }

    this.clocking.set(true);
    this.error.set(null);

    try {
      const coords = await this.captureCoordinates();
      await firstValueFrom(
        this.attendanceService.registerClock({
          employeeId,
          type: punchType,
          method: 'WEB',
          source: 'WEB',
          latitude: coords.latitude,
          longitude: coords.longitude,
          notes: notes
        })
      );

      // Iniciar bloqueo preventivo de debounce
      if (typeof window !== 'undefined' && window.localStorage) {
        localStorage.setItem(this.DEBOUNCE_KEY, Date.now().toString());
      }
      this.checkDebounceTimer();

      let label = 'Marcación';
      if (punchType === 'IN') label = 'Entrada';
      else if (punchType === 'OUT') label = 'Salida';
      else if (punchType === 'BREAK_START' || punchType === 'BREAK_OUT') label = 'Inicio de Pausa';
      else if (punchType === 'BREAK_END' || punchType === 'BREAK_IN') label = 'Fin de Pausa';

      this.toastService.success(`${label} registrada exitosamente.`);

      const localDate = new Date().toLocaleDateString('en-CA');
      try {
        await firstValueFrom(this.attendanceService.calculateRecord({ employeeId, workDate: localDate }));
      } catch {
        // Silencioso si no tiene turno asignado
      }

      let today = await this.fetchTodayRecord(employeeId);
      if (!today) {
        today = {
          id: `punch-${Date.now()}`,
          employeeId,
          workDate: localDate,
          checkInTime: punchType === 'IN' ? new Date().toISOString() : null,
          checkOutTime: punchType === 'OUT' ? new Date().toISOString() : null,
          actualFirstInAt: punchType === 'IN' ? new Date().toISOString() : null,
          actualLastOutAt: punchType === 'OUT' ? new Date().toISOString() : null,
          workedHours: 0,
          lateMinutes: 0,
          earlyDepartureMinutes: 0,
          status: 'PRESENT',
          source: 'WEB',
          createdAt: new Date().toISOString()
        };
      }
      this.lastPunchResult.set(today);
      return today;
    } catch (err: any) {
      const rawMsg = err?.error?.message || err?.message || 'Error al procesar marcación';
      let msg = rawMsg;
      if (rawMsg.toLowerCase().includes('sequence') || rawMsg.toLowerCase().includes('invalid punch sequence')) {
        msg = 'Este usuario no tiene una entrada registrada. Debe marcar primero su entrada antes de registrar la salida.';
      }
      this.error.set(msg);
      this.toastService.error(msg);
      return null;
    } finally {
      this.clocking.set(false);
    }
  }

  async registerManualClock(
    employeeId: string,
    punchType: 'IN' | 'OUT' | 'BREAK_START' | 'BREAK_END' | 'BREAK_OUT' | 'BREAK_IN',
    punchedAt: string,
    notes: string
  ): Promise<AttendanceRecordResponse | null> {
    if (!notes || notes.trim().length < 5) {
      this.toastService.warning('Las marcaciones manuales requieren una justificación (mínimo 5 caracteres).');
      return null;
    }

    this.clocking.set(true);
    this.error.set(null);

    try {
      await firstValueFrom(
        this.attendanceService.registerClock({
          employeeId,
          type: punchType,
          punchedAt,
          method: 'MANUAL',
          source: 'MANUAL',
          notes: notes.trim()
        })
      );

      this.toastService.success('Marcación manual registrada correctamente.');

      const punchDate = punchedAt.substring(0, 10);
      try {
        await firstValueFrom(this.attendanceService.calculateRecord({ employeeId, workDate: punchDate }));
      } catch {
        // Silencioso
      }

      let today = await this.fetchTodayRecord(employeeId);
      if (!today) {
        today = {
          id: `punch-${Date.now()}`,
          employeeId,
          workDate: punchDate,
          checkInTime: punchType === 'IN' ? punchedAt : null,
          checkOutTime: punchType === 'OUT' ? punchedAt : null,
          actualFirstInAt: punchType === 'IN' ? punchedAt : null,
          actualLastOutAt: punchType === 'OUT' ? punchedAt : null,
          workedHours: 0,
          lateMinutes: 0,
          earlyDepartureMinutes: 0,
          status: 'PRESENT',
          source: 'MANUAL',
          createdAt: new Date().toISOString()
        };
      }
      this.lastPunchResult.set(today);
      return today;
    } catch (err: any) {
      const rawMsg = err?.error?.message || err?.message || 'Error al registrar marcación manual';
      let msg = rawMsg;
      if (rawMsg.toLowerCase().includes('sequence') || rawMsg.toLowerCase().includes('invalid punch sequence')) {
        msg = 'Este usuario no tiene una entrada registrada. Debe marcar primero su entrada antes de registrar la salida.';
      }
      this.error.set(msg);
      this.toastService.error(msg);
      return null;
    } finally {
      this.clocking.set(false);
    }
  }

  async fetchTodayRecord(employeeId: string): Promise<AttendanceRecordResponse | null> {
    if (!employeeId) return null;
    const localToday = new Date().toLocaleDateString('en-CA');
    try {
      const [statusRes, punchesRes] = await Promise.all([
        firstValueFrom(this.attendanceService.getTodayStatus(employeeId)).catch(() => null),
        firstValueFrom(
          this.attendanceService.listPunches({
            employeeId,
            page: 0,
            size: 50,
            sort: 'punchedAt',
            direction: 'asc'
          })
        ).catch(() => null)
      ]);

      let record = statusRes?.data ?? null;
      const punches = punchesRes?.data?.content || [];
      const todayPunches = punches.filter((p) => {
        try {
          return new Date(p.punchedAt).toLocaleDateString('en-CA') === localToday;
        } catch {
          return p.punchedAt.startsWith(localToday);
        }
      });

      if (todayPunches.length > 0) {
        const inPunch = todayPunches.find((p) => p.punchType === 'IN');
        const outPunch = [...todayPunches].reverse().find((p) => p.punchType === 'OUT');

        if (record) {
          if (!record.checkInTime && inPunch) {
            record.checkInTime = inPunch.punchedAt;
            record.actualFirstInAt = inPunch.punchedAt;
          }
          if (!record.checkOutTime && outPunch) {
            record.checkOutTime = outPunch.punchedAt;
            record.actualLastOutAt = outPunch.punchedAt;
          }
        } else {
          record = {
            id: inPunch?.attendanceRecordId || inPunch?.id || `punch-${Date.now()}`,
            employeeId,
            workDate: localToday,
            checkInTime: inPunch ? inPunch.punchedAt : null,
            checkOutTime: outPunch ? outPunch.punchedAt : null,
            actualFirstInAt: inPunch ? inPunch.punchedAt : null,
            actualLastOutAt: outPunch ? outPunch.punchedAt : null,
            workedHours: 0,
            lateMinutes: 0,
            earlyDepartureMinutes: 0,
            status: inPunch ? 'PRESENT' : 'INCOMPLETE',
            source: inPunch?.source || 'WEB',
            createdAt: inPunch?.punchedAt || new Date().toISOString()
          };
        }
      }

      return record;
    } catch {
      return null;
    }
  }
}

