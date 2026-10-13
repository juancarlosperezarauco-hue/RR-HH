import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { forkJoin } from 'rxjs';
import { HasPermissionPipe } from '../../shared/api';
import { ToastService } from '../../shared/ui/toast/toast.service';
import {
  AssignmentOption, ContractHistory, ContractRequest, ContractStatus, ContractsService,
  DepartmentOption, EmployeeOption, EmploymentContract, PositionOption
} from '../../entities/contracts';

type FormMode = 'create' | 'edit' | 'renew';

@Component({
  selector: 'app-contracts-page',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule, HasPermissionPipe],
  templateUrl: './contracts-page.component.html'
})
export class ContractsPageComponent implements OnInit {
  private readonly service = inject(ContractsService);
  private readonly toast = inject(ToastService);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly contracts = signal<EmploymentContract[]>([]);
  readonly employees = signal<EmployeeOption[]>([]);
  readonly assignments = signal<AssignmentOption[]>([]);
  readonly departments = signal<DepartmentOption[]>([]);
  readonly positions = signal<PositionOption[]>([]);
  readonly historyItems = signal<ContractHistory[]>([]);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly currentPage = signal(0);
  readonly showForm = signal(false);
  readonly showHistory = signal(false);
  readonly formMode = signal<FormMode>('create');
  readonly selected = signal<EmploymentContract | null>(null);
  search = '';
  statusFilter = '';
  form: ContractRequest = this.emptyForm();
  renewalReason = '';

  ngOnInit(): void {
    forkJoin({ employees: this.service.employees(), departments: this.service.departments(), positions: this.service.positions() })
      .subscribe({
        next: result => {
          this.employees.set(result.employees.data.content);
          this.departments.set(result.departments.data.content);
          this.positions.set(result.positions.data.content);
          this.load();
        },
        error: error => { this.handleError(error, 'No se pudieron cargar los datos relacionados'); this.load(); }
      });
  }

  load(page = this.currentPage()): void {
    this.loading.set(true);
    this.service.list(page, 25, this.search, this.statusFilter).subscribe({
      next: response => {
        this.contracts.set(response.data.content);
        this.totalElements.set(response.data.totalElements);
        this.totalPages.set(response.data.totalPages);
        this.currentPage.set(response.data.number);
        this.loading.set(false);
      },
      error: error => { this.loading.set(false); this.handleError(error, 'No se pudieron cargar los contratos'); }
    });
  }

  openCreate(): void {
    this.formMode.set('create'); this.selected.set(null); this.form = this.emptyForm();
    this.assignments.set([]); this.renewalReason = ''; this.showForm.set(true);
  }
  openEdit(contract: EmploymentContract): void {
    this.formMode.set('edit'); this.selected.set(contract); this.form = this.fromContract(contract);
    this.renewalReason = ''; this.loadAssignments(contract.employeeId, contract.employeeAssignmentId);
    this.showForm.set(true);
  }
  openRenew(contract: EmploymentContract): void {
    this.formMode.set('renew'); this.selected.set(contract);
    const nextStart = contract.endDate ? this.addDay(contract.endDate) : '';
    this.form = { ...this.fromContract(contract), contractNumber: `${contract.contractNumber}-R1`, startDate: nextStart, endDate: null };
    this.renewalReason = 'Continuidad de la relación laboral';
    this.loadAssignments(contract.employeeId, contract.employeeAssignmentId);
    this.showForm.set(true);
  }
  closeForm(): void { if (!this.saving()) this.showForm.set(false); }

  employeeChanged(): void {
    this.form.employeeAssignmentId = '';
    this.loadAssignments(this.form.employeeId);
  }
  loadAssignments(employeeId: string, selectedId?: string): void {
    if (!employeeId) { this.assignments.set([]); return; }
    this.service.assignments(employeeId).subscribe({
      next: response => {
        this.assignments.set(response.data.content);
        if (selectedId) this.form.employeeAssignmentId = selectedId;
      },
      error: error => this.handleError(error, 'No se pudieron cargar las asignaciones del empleado')
    });
  }

  save(): void {
    if (!this.form.employeeId || !this.form.employeeAssignmentId || !this.form.contractNumber || !this.form.startDate) {
      this.toast.warning('Complete empleado, asignación, número y fecha de inicio.'); return;
    }
    if (this.form.contractType !== 'INDEFINITE' && !this.form.endDate) {
      this.toast.warning('Este tipo de contrato requiere fecha de finalización.'); return;
    }
    const request: ContractRequest = {
      ...this.form,
      endDate: this.form.contractType === 'INDEFINITE' ? null : this.form.endDate,
      weeklyHours: this.form.weeklyHours ? Number(this.form.weeklyHours) : null,
      baseSalary: Number(this.form.baseSalary),
      probationEndDate: this.form.probationEndDate || null,
      terms: this.form.terms || null
    };
    this.saving.set(true);
    const selected = this.selected();
    const { employeeId: _employeeId, ...renewalRequest } = request;
    const operation = this.formMode() === 'create'
      ? this.service.create(request)
      : this.formMode() === 'edit' && selected
        ? this.service.update(selected.id, request)
        : this.service.renew(selected!.id, { ...renewalRequest, reason: this.renewalReason || null });
    operation.subscribe({
      next: () => {
        this.saving.set(false); this.showForm.set(false);
        this.toast.success(this.formMode() === 'renew' ? 'Renovación creada como borrador.' : 'Contrato guardado correctamente.');
        this.load(0);
      },
      error: error => { this.saving.set(false); this.handleError(error, 'No se pudo guardar el contrato'); }
    });
  }

  transition(contract: EmploymentContract, target: ContractStatus): void {
    let reason: string | null = null; let effectiveDate: string | null = null;
    if (target === 'TERMINATED') {
      reason = window.prompt('Motivo de terminación del contrato:')?.trim() || null;
      if (!reason) return;
      effectiveDate = window.prompt('Fecha efectiva (AAAA-MM-DD):', new Date().toISOString().slice(0, 10)) || null;
      if (!effectiveDate) return;
    }
    const label = this.statusLabel(target);
    if (!window.confirm(`¿Confirmar cambio del contrato ${contract.contractNumber} a ${label}?`)) return;
    this.service.changeStatus(contract.id, target, reason, effectiveDate).subscribe({
      next: () => { this.toast.success(`Contrato actualizado a ${label}.`); this.load(); },
      error: error => this.handleError(error, 'No se pudo cambiar el estado')
    });
  }

  viewHistory(contract: EmploymentContract): void {
    this.selected.set(contract); this.historyItems.set([]); this.showHistory.set(true);
    this.service.history(contract.id).subscribe({
      next: response => this.historyItems.set(response.data),
      error: error => this.handleError(error, 'No se pudo consultar el historial')
    });
  }

  contractDocument(contract: EmploymentContract, print = false): void {
    this.service.document(contract.id).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        if (print) {
          const preview = window.open(url, '_blank');
          if (!preview) {
            URL.revokeObjectURL(url);
            this.toast.warning('El navegador bloqueó la ventana de impresión. Habilite las ventanas emergentes.');
            return;
          }
          window.setTimeout(() => { preview.print(); window.setTimeout(() => URL.revokeObjectURL(url), 60000); }, 1200);
        } else {
          const link = document.createElement('a');
          link.href = url;
          link.download = `contrato-${contract.contractNumber.replace(/[^A-Za-z0-9._-]/g, '_')}.pdf`;
          link.click();
          window.setTimeout(() => URL.revokeObjectURL(url), 1000);
        }
      },
      error: error => this.handleError(error, 'No se pudo generar el documento del contrato')
    });
  }

  employeeName(id: string): string {
    const e = this.employees().find(item => item.id === id);
    return e ? `${e.firstName} ${e.lastName} · ${e.employeeNumber}` : id.slice(0, 8);
  }
  activeCount(): number { return this.contracts().filter(item => item.status === 'ACTIVE').length; }
  assignmentLabel(a: AssignmentOption): string {
    const department = this.departments().find(item => item.id === a.departmentId);
    const position = this.positions().find(item => item.id === a.positionId);
    return `${department?.name ?? 'Departamento'} / ${position?.name ?? 'Puesto'} · ${a.startDate}`;
  }
  statusLabel(status: ContractStatus): string {
    return ({ DRAFT: 'Borrador', PENDING_SIGNATURE: 'Pendiente de firma', ACTIVE: 'Activo', EXPIRED: 'Vencido', TERMINATED: 'Finalizado', CANCELLED: 'Cancelado' })[status];
  }
  typeLabel(type: string): string {
    return ({ INDEFINITE: 'Tiempo indefinido', FIXED_TERM: 'Plazo fijo', SEASONAL: 'Temporada', WORK_OR_SERVICE: 'Obra o servicio', EVENTUAL: 'Eventual', INTERNSHIP: 'Pasantía', OTHER: 'Otro' } as Record<string, string>)[type] ?? type;
  }
  statusClass(status: ContractStatus): string {
    return ({ ACTIVE: 'bg-emerald-100 text-emerald-800', PENDING_SIGNATURE: 'bg-amber-100 text-amber-800', DRAFT: 'bg-slate-100 text-slate-700', EXPIRED: 'bg-orange-100 text-orange-800', TERMINATED: 'bg-red-100 text-red-800', CANCELLED: 'bg-zinc-100 text-zinc-600' })[status];
  }
  private emptyForm(): ContractRequest {
    return { employeeId: '', employeeAssignmentId: '', contractNumber: '', contractType: 'FIXED_TERM',
      contractForm: 'WRITTEN', workSchedule: 'FULL_TIME', workModality: 'ONSITE', startDate: '', endDate: null,
      baseSalary: 0, currency: 'BOB', paymentFrequency: 'MONTHLY', weeklyHours: 48,
      probationEndDate: null, terms: null };
  }
  private fromContract(c: EmploymentContract): ContractRequest {
    return { employeeId: c.employeeId, employeeAssignmentId: c.employeeAssignmentId,
      contractNumber: c.contractNumber, contractType: c.contractType, contractForm: c.contractForm,
      workSchedule: c.workSchedule, workModality: c.workModality, startDate: c.startDate,
      endDate: c.endDate, baseSalary: c.baseSalary, currency: c.currency,
      paymentFrequency: c.paymentFrequency, weeklyHours: c.weeklyHours,
      probationEndDate: c.probationEndDate, terms: c.terms };
  }
  private addDay(date: string): string { const d = new Date(`${date}T12:00:00`); d.setDate(d.getDate() + 1); return d.toISOString().slice(0, 10); }
  private handleError(error: unknown, fallback: string): void {
    const message = error instanceof HttpErrorResponse
      ? (error.error?.message || error.error?.error || fallback) : fallback;
    this.toast.error(String(message));
  }
}
