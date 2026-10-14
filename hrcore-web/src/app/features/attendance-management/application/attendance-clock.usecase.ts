import { inject, Injectable, signal, computed } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AttendanceService, AttendanceRecordResponse } from '../../../entities/attendance';
import { ToastService } from '../../../shared/ui/toast/toast.service';

@Injectable({ providedIn: 'root' })
export class AttendanceClockUseCase {
  private readonly attendanceService = inject(AttendanceService);
  private readonly toastService = inject(ToastService);

  private readonly DEBOUNCE_KEY = 'hrcore_last_clock_timestamp';
  private readonly DEBOUNCE_MS = 2 * 60 * 1000; // 120 segundos (2 minutos)
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
      const res = await firstValueFrom(
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

      this.toastService.success(`${label} registrada y sellada con timestamp inmutable.`);
      this.lastPunchResult.set(res.data);
      return res.data;
    } catch (err: any) {
      const msg = err?.error?.message || err?.message || 'Error al procesar marcación';
      this.error.set(msg);
      this.toastService.error(msg);
      return null;
    } finally {
      this.clocking.set(false);
    }
  }

  async fetchTodayRecord(employeeId: string): Promise<AttendanceRecordResponse | null> {
    try {
      const res = await firstValueFrom(this.attendanceService.getTodayStatus(employeeId));
      return res.data;
    } catch {
      return null;
    }
  }
}
