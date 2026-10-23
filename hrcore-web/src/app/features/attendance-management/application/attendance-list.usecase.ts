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
  readonly page = signal(0);
  readonly pageSize = signal(20);
  readonly totalPages = signal(1);
  readonly totalElements = signal(0);

  // Filtros reactivos
  readonly statusFilter = signal<AttendanceStatus | 'ALL'>('ALL');
  readonly searchTerm = signal<string>('');
  private localDate(): string {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
  }
  readonly startDate = signal<string>(this.localDate());
  readonly endDate = signal<string>(this.localDate());
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

  async loadRecords(page = this.page()): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const [res, employees] = await Promise.all([
        firstValueFrom(this.attendanceService.listRecords(
          this.startDate(),
          this.endDate(),
          this.employeeFilter() || undefined,
          this.statusFilter(), page, this.pageSize()
        )),
        this.loadEmployeeDirectory()
      ]);
      const rawEmployees: EmployeeSummary[] = employees;
      const rawRecords: AttendanceRecordResponse[] = Array.isArray(res?.data)
        ? (res.data as AttendanceRecordResponse[])
        : (res?.data?.content || []);

      const directory = new Map<string, EmployeeSummary>(rawEmployees.map((employee) => [employee.id, employee]));
      this.records.set(rawRecords.map((record) => {
        const employee = directory.get(record.employeeId);
        return {
          ...record,
          employeeFullName: employee ? `${employee.firstName} ${employee.lastName}`.trim() : record.employeeFullName,
          employeeCode: employee?.employeeNumber || record.employeeCode
        };
      }));
      this.page.set(res.data?.page ?? page);
      this.pageSize.set(res.data?.size ?? this.pageSize());
      this.totalPages.set(Math.max(1, res.data?.totalPages ?? 1));
      this.totalElements.set(res.data?.totalElements ?? rawRecords.length);
    } catch (err: any) {
      this.error.set(err?.error?.message || err?.message || 'Error al cargar registros');
    } finally {
      this.loading.set(false);
    }
  }

  private async loadEmployeeDirectory(): Promise<EmployeeSummary[]> {
    const result: EmployeeSummary[] = [];
    for (let page = 0; page < 100; page++) {
      const response = await firstValueFrom(this.employeesService.list(page, 100));
      const content = Array.isArray(response.data) ? response.data as EmployeeSummary[] : (response.data?.content || []);
      result.push(...content);
      if (content.length < 100 || response.data?.last === true) break;
    }
    return result;
  }

  setStatusFilter(status: AttendanceStatus | 'ALL'): void {
    this.statusFilter.set(status);
    void this.loadRecords(0);
  }

  setDateRange(start: string, end: string): void {
    this.startDate.set(start);
    this.endDate.set(end);
    void this.loadRecords(0);
  }

  setEmployeeFilter(employeeId: string): void {
    this.employeeFilter.set(employeeId);
    void this.loadRecords(0);
  }

  setSearch(term: string): void {
    this.searchTerm.set(term);
  }

  nextPage(): void {
    if (this.page() + 1 < this.totalPages()) void this.loadRecords(this.page() + 1);
  }

  previousPage(): void {
    if (this.page() > 0) void this.loadRecords(this.page() - 1);
  }
}
