import { inject, Injectable, signal, computed } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import {
  AttendanceService,
  AttendanceRecordResponse,
  AttendanceStatus
} from '../../../entities/attendance';

@Injectable({ providedIn: 'root' })
export class AttendanceListUseCase {
  private readonly attendanceService = inject(AttendanceService);

  readonly records = signal<AttendanceRecordResponse[]>([]);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  // Filtros reactivos
  readonly statusFilter = signal<AttendanceStatus | 'ALL'>('ALL');
  readonly searchTerm = signal<string>('');
  readonly startDate = signal<string>(new Date().toISOString().substring(0, 10));
  readonly endDate = signal<string>(new Date().toISOString().substring(0, 10));

  // Registros filtrados localmente por buscador de texto (nombre / código)
  readonly filteredRecords = computed(() => {
    const list = this.records();
    const term = this.searchTerm().trim().toLowerCase();
    const status = this.statusFilter();

    return list.filter((r) => {
      const matchesStatus = status === 'ALL' || r.status === status;
      const matchesTerm =
        !term ||
        (r.employeeFullName && r.employeeFullName.toLowerCase().includes(term)) ||
        (r.employeeCode && r.employeeCode.toLowerCase().includes(term));

      return matchesStatus && matchesTerm;
    });
  });

  // KPIs reactivos
  readonly totalRecords = computed(() => this.records().length);

  readonly presentCount = computed(
    () => this.records().filter((r) => r.status === 'PRESENT').length
  );

  readonly lateCount = computed(
    () => this.records().filter((r) => r.status === 'LATE').length
  );

  readonly absentCount = computed(
    () => this.records().filter((r) => r.status === 'ABSENT').length
  );

  readonly totalLateMinutes = computed(() =>
    this.records().reduce((acc, curr) => acc + (curr.lateMinutes || 0), 0)
  );

  readonly punctualityRate = computed(() => {
    const present = this.presentCount();
    const late = this.lateCount();
    const totalWorking = present + late;
    if (totalWorking === 0) return 100;
    return Math.round((present / totalWorking) * 100);
  });

  readonly totalWorkedHours = computed(() => {
    const total = this.records().reduce((acc, curr) => acc + (curr.workedHours || 0), 0);
    return Math.round(total * 10) / 10;
  });

  async loadRecords(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const res = await firstValueFrom(
        this.attendanceService.listRecords(
          this.startDate(),
          this.endDate(),
          undefined,
          this.statusFilter()
        )
      );
      this.records.set(res.data.content);
    } catch (err: any) {
      this.error.set(err?.error?.message || err?.message || 'Error al cargar registros');
    } finally {
      this.loading.set(false);
    }
  }

  setStatusFilter(status: AttendanceStatus | 'ALL'): void {
    this.statusFilter.set(status);
  }

  setDateRange(start: string, end: string): void {
    this.startDate.set(start);
    this.endDate.set(end);
    this.loadRecords();
  }

  setSearch(term: string): void {
    this.searchTerm.set(term);
  }
}
