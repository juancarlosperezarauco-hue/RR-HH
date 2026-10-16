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

  // Fechas en hora local (evita desfases por UTC-4 a altas horas de la noche)
  readonly startDate = signal<string>(
    new Date(new Date().getFullYear(), new Date().getMonth(), 1).toLocaleDateString('en-CA')
  );
  readonly endDate = signal<string>(
    new Date(new Date().getFullYear(), new Date().getMonth() + 1, 0).toLocaleDateString('en-CA')
  );
  readonly statusFilter = signal<AttendanceStatus | 'ALL'>('ALL');
  readonly searchTerm = signal<string>('');
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
      const [res, employeesRes, punchesRes] = await Promise.all([
        firstValueFrom(this.attendanceService.listRecords(
          this.startDate(),
          this.endDate(),
          this.employeeFilter() || undefined,
          this.statusFilter(),
          0,
          100
        )).catch(() => ({ data: { content: [] } })),
        firstValueFrom(this.employeesService.list(0, 100)).catch(() => null),
        firstValueFrom(this.attendanceService.listPunches({
          employeeId: this.employeeFilter() || undefined,
          page: 0,
          size: 100
        })).catch(() => ({ data: { content: [] } }))
      ]);

      const employees = employeesRes?.data?.content || [];
      const directory = new Map<string, EmployeeSummary>(employees.map((employee) => [employee.id, employee]));

      // 1. Mapeo de jornadas oficiales consolidadas
      const recordMap = new Map<string, AttendanceRecordResponse>();
      for (const record of res.data?.content || []) {
        const employee = directory.get(record.employeeId);
        const key = `${record.employeeId}_${record.workDate}`;
        recordMap.set(key, {
          ...record,
          employeeFullName: employee ? `${employee.firstName} ${employee.lastName}`.trim() : (record.employeeFullName || 'Colaborador'),
          employeeCode: employee?.employeeNumber || record.employeeCode
        });
      }

      // 2. Consolidar e integrar marcaciones recientes para que cualquier entrada o salida realizada aparezca inmediatamente
      const punches = punchesRes.data?.content || [];
      const chronologicPunches = [...punches].sort((a, b) => a.punchedAt.localeCompare(b.punchedAt));

      for (const punch of chronologicPunches) {
        const punchDate = this.toLocalDateString(punch.punchedAt);
        // Verificar si cae dentro del rango de visualización
        if (punchDate < this.startDate() || punchDate > this.endDate()) {
          continue;
        }

        const key = `${punch.employeeId}_${punchDate}`;
        const existing = recordMap.get(key);
        const employee = directory.get(punch.employeeId);

        if (existing) {
          if (punch.punchType === 'IN' && (!existing.checkInTime || punch.punchedAt < existing.checkInTime)) {
            existing.checkInTime = punch.punchedAt;
            existing.actualFirstInAt = punch.punchedAt;
          } else if (punch.punchType === 'OUT') {
            if (!existing.checkOutTime || punch.punchedAt > existing.checkOutTime) {
              existing.checkOutTime = punch.punchedAt;
              existing.actualLastOutAt = punch.punchedAt;
            }
          }
          if (punch.source === 'MANUAL') {
            existing.source = 'MANUAL';
          }
          if (existing.checkInTime && existing.checkOutTime && (!existing.workedHours || existing.workedHours === 0)) {
            const diffMs = new Date(existing.checkOutTime).getTime() - new Date(existing.checkInTime).getTime();
            existing.workedHours = Math.max(0, Math.round((diffMs / 3600000) * 10) / 10);
            if (existing.status === 'PENDING') {
              if (existing.earlyDepartureMinutes && existing.earlyDepartureMinutes > 0) {
                existing.status = 'INCOMPLETE';
              } else if (existing.lateMinutes && existing.lateMinutes > 0) {
                existing.status = 'LATE';
              } else {
                existing.status = 'PRESENT';
              }
            }
          }
        } else {
          // Si no existe jornada previa calculada en backend, sintetizar fila con las marcaciones registradas
          const synthesized: AttendanceRecordResponse = {
            id: punch.attendanceRecordId || punch.id,
            employeeId: punch.employeeId,
            workDate: punchDate,
            checkInTime: punch.punchType === 'IN' ? punch.punchedAt : null,
            checkOutTime: punch.punchType === 'OUT' ? punch.punchedAt : null,
            actualFirstInAt: punch.punchType === 'IN' ? punch.punchedAt : null,
            actualLastOutAt: punch.punchType === 'OUT' ? punch.punchedAt : null,
            workedHours: 0,
            lateMinutes: 0,
            earlyDepartureMinutes: 0,
            status: punch.punchType === 'IN' ? 'PRESENT' : 'INCOMPLETE',
            source: punch.source,
            createdAt: punch.punchedAt,
            employeeFullName: employee ? `${employee.firstName} ${employee.lastName}`.trim() : 'Colaborador',
            employeeCode: employee?.employeeNumber || punch.employeeId
          };
          recordMap.set(key, synthesized);
        }
      }

      // Ordenar por fecha descendente
      const sorted = Array.from(recordMap.values()).sort((a, b) => b.workDate.localeCompare(a.workDate));
      this.records.set(sorted);
    } catch (err: any) {
      this.error.set(err?.error?.message || err?.message || 'Error al cargar registros');
    } finally {
      this.loading.set(false);
    }
  }

  private toLocalDateString(iso: string): string {
    try {
      const d = new Date(iso);
      return d.toLocaleDateString('en-CA');
    } catch {
      return iso.substring(0, 10);
    }
  }

  setStatusFilter(status: AttendanceStatus | 'ALL'): void {
    this.statusFilter.set(status);
    this.loadRecords();
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
