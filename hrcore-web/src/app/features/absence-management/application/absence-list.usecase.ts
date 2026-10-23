import { inject, Injectable, signal, computed } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import {
  AbsenceService,
  LeaveRequestResponse,
  LeaveTypeResponse,
  HolidayResponse,
  LeaveStatus
} from '../../../entities/absence';
import { ToastService } from '../../../shared/ui/toast/toast.service';
import { EmployeesService, EmployeeSummary } from '../../../entities/employees';

@Injectable({ providedIn: 'root' })
export class AbsenceListUseCase {
  private readonly absenceService = inject(AbsenceService);
  private readonly employeesService = inject(EmployeesService);
  private readonly toastService = inject(ToastService);

  readonly requests = signal<LeaveRequestResponse[]>([]);
  readonly leaveTypes = signal<LeaveTypeResponse[]>([]);
  readonly holidays = signal<HolidayResponse[]>([]);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);
  readonly page = signal(0);
  readonly pageSize = 20;
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);

  // Filtros reactivos
  readonly statusFilter = signal<LeaveStatus | 'ALL'>('ALL');
  readonly searchTerm = signal<string>('');
  readonly selectedEmployeeId = signal<string | null>(null);

  // Solicitudes filtradas localmente
  readonly filteredRequests = computed(() => {
    const list = this.requests();
    const term = this.searchTerm().trim().toLowerCase();
    const status = this.statusFilter();

    return list.filter((r) => {
      const matchesStatus = status === 'ALL' || r.status === status;
      const matchesTerm =
        !term ||
        (r.employeeFullName && r.employeeFullName.toLowerCase().includes(term)) ||
        (r.leaveTypeName && r.leaveTypeName.toLowerCase().includes(term)) ||
        (r.reason && r.reason.toLowerCase().includes(term));

      return matchesStatus && matchesTerm;
    });
  });

  // KPIs reactivos
  readonly totalRequests = computed(() => this.totalElements() || this.requests().length);

  readonly pendingCount = computed(
    () => this.requests().filter((r) => r.status === 'PENDING').length
  );

  readonly approvedCount = computed(
    () => this.requests().filter((r) => r.status === 'APPROVED').length
  );

  readonly rejectedCount = computed(
    () => this.requests().filter((r) => r.status === 'REJECTED').length
  );

  readonly totalApprovedDays = computed(() =>
    this.requests()
      .filter((r) => r.status === 'APPROVED')
      .reduce((acc, curr) => acc + (curr.totalDays || 0), 0)
  );

  async loadRequests(employeeId?: string, page = this.page()): Promise<void> {
    this.page.set(Math.max(0, page));
    this.loading.set(true);
    this.error.set(null);
    try {
      const [res, employeesRes, typesRes] = await Promise.all([
        firstValueFrom(this.absenceService.listRequests(employeeId, this.statusFilter(), this.page(), this.pageSize)),
        firstValueFrom(this.employeesService.list(0, 100)).catch(() => null),
        firstValueFrom(this.absenceService.getLeaveTypes()).catch(() => null)
      ]);
      const employees = new Map<string, EmployeeSummary>(
        (employeesRes?.data?.content || []).map((employee) => [employee.id, employee])
      );
      const types = new Map((typesRes?.data || []).map((type) => [type.id, type]));
      if (typesRes?.data) this.leaveTypes.set(typesRes.data);
      this.totalElements.set(res.data?.totalElements ?? 0);
      this.totalPages.set(res.data?.totalPages ?? (this.totalElements() ? Math.ceil(this.totalElements() / this.pageSize) : 0));
      this.requests.set((res.data?.content || []).map((request) => {
        const employee = employees.get(request.employeeId);
        return {
          ...request,
          employeeFullName: employee ? `${employee.firstName} ${employee.lastName}`.trim() : request.employeeFullName,
          leaveTypeName: types.get(request.leaveTypeId)?.name || request.leaveTypeName
        };
      }));
    } catch (err: any) {
      this.error.set(err?.error?.message || 'Error al cargar las solicitudes de ausencia');
    } finally {
      this.loading.set(false);
    }
  }

  async loadLeaveTypes(): Promise<void> {
    try {
      const res = await firstValueFrom(this.absenceService.getLeaveTypes());
      this.leaveTypes.set(res.data);
    } catch {
      // Ignorar fallback silencioso
    }
  }

  async loadHolidays(): Promise<void> {
    try {
      const res = await firstValueFrom(this.absenceService.getHolidays());
      this.holidays.set(res.data);
    } catch {
      // Ignorar fallback silencioso
    }
  }

  setStatusFilter(status: LeaveStatus | 'ALL'): void {
    this.statusFilter.set(status);
  }

  setSearch(term: string): void {
    this.searchTerm.set(term);
  }

  setEmployeeFilter(employeeId: string | null): void {
    this.selectedEmployeeId.set(employeeId);
    this.loadRequests(employeeId || undefined, 0);
  }

  nextPage(employeeId?: string): void {
    if (this.page() + 1 < this.totalPages()) void this.loadRequests(employeeId, this.page() + 1);
  }

  previousPage(employeeId?: string): void {
    if (this.page() > 0) void this.loadRequests(employeeId, this.page() - 1);
  }

  async downloadAttachment(requestId: string, attachmentId: string, filename: string): Promise<void> {
    try {
      const blob = await firstValueFrom(this.absenceService.downloadAttachment(requestId, attachmentId));
      if (typeof window !== 'undefined') {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename || 'comprobante.pdf';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
        this.toastService.success(`Descarga iniciada: ${filename}`);
      }
    } catch {
      this.toastService.error('Error al descargar el comprobante adjunto');
    }
  }
}
