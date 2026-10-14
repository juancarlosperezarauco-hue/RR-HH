import { inject, Injectable, signal, computed } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import {
  AttendanceService,
  AttendanceRecordResponse,
  AttendanceStatus
} from '../../../entities/attendance';
import { EmployeesService, EmployeeSummary } from '../../../entities/employees';

@Injectable({ providedIn: 'root' })
export class AttendanceListUseCase {
  private readonly attendanceService = inject(AttendanceService);
  private readonly employeesService = inject(EmployeesService);

  readonly records = signal<AttendanceRecordResponse[]>([]);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  // Filtros reactivos
  readonly statusFilter = signal<AttendanceStatus | 'ALL'>('ALL');
  readonly searchTerm = signal<string>('');
  readonly startDate = signal<string>(new Date().toISOString().substring(0, 10));
  readonly endDate = signal<string>(new Date().toISOString().substring(0, 10));
  readonly employeeFilter = signal<string>('');

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
    const total = this.records().reduce((acc, curr) => {
      if (curr.workedHours !== undefined) return acc + curr.workedHours;
      if (curr.workedMinutes !== undefined) return acc + (curr.workedMinutes / 60);
      return acc;
    }, 0);
    return Math.round(total * 10) / 10;
  });

  async calculateRecord(employeeId: string, workDate: string): Promise<boolean> {
    try {
      await firstValueFrom(this.attendanceService.calculateRecord({ employeeId, workDate }));
      await this.loadRecords();
      return true;
    } catch {
      return false;
    }
  }

  async loadRecords(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const [res, employeesRes] = await Promise.all([
        firstValueFrom(this.attendanceService.listRecords(
          this.startDate(),
          this.endDate(),
          this.employeeFilter() || undefined,
          this.statusFilter()
        )),
        firstValueFrom(this.employeesService.list(0, 100)).catch(() => null)
      ]);
      const employees = employeesRes?.data?.content || [];
      const directory = new Map<string, EmployeeSummary>(employees.map((employee) => [employee.id, employee]));
      this.records.set((res.data.content || []).map((record) => {
        const employee = directory.get(record.employeeId);
        return {
          ...record,
          employeeFullName: employee ? `${employee.firstName} ${employee.lastName}`.trim() : record.employeeFullName,
          employeeCode: employee?.employeeNumber || record.employeeCode
        };
      }));
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

  setEmployeeFilter(employeeId: string): void {
    this.employeeFilter.set(employeeId);
    this.loadRecords();
  }

  setSearch(term: string): void {
    this.searchTerm.set(term);
  }
}
