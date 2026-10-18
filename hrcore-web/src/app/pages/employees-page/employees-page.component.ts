import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { forkJoin, Observable } from 'rxjs';
import { HasPermissionPipe } from '../../shared/api';
import { ToastService } from '../../shared/ui/toast/toast.service';
import { ContractsService, DepartmentOption, PositionOption } from '../../entities/contracts';
import { UserService, TenantUserResponse } from '../../entities/user';
import { PermissionService } from '../../shared/lib/auth/permission.service';
import {
  EmergencyContact, EmergencyContactRequest, Employee, EmployeeAssignment,
  EmployeeAssignmentRequest, EmployeeRequest, EmployeeSummary, EmployeesService,
  EmploymentStatus, EmploymentPeriod, EmployeeTermination
} from '../../entities/employees';

type FormMode = 'create' | 'edit';
type ConfirmationMode = 'none' | 'reason' | 'date' | 'rehire';

@Component({
  selector: 'app-employees-page',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule, HasPermissionPipe],
  templateUrl: './employees-page.component.html',
  styles: [`
    :host input,:host select,:host textarea{font-size:.875rem;font-weight:400;color:var(--foreground)}
    :host input::placeholder,:host textarea::placeholder{color:var(--muted-foreground);font-weight:400}
  `]
})
export class EmployeesPageComponent implements OnInit {
  private readonly service = inject(EmployeesService);
  private readonly related = inject(ContractsService);
  private readonly usersService = inject(UserService);
  private readonly permissions = inject(PermissionService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  goToDossier(employeeId: string): void {
    this.router.navigate(['/dashboard/dossiers'], { queryParams: { employeeId } });
  }

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly employees = signal<EmployeeSummary[]>([]);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly currentPage = signal(0);
  readonly showForm = signal(false);
  readonly showProfile = signal(false);
  readonly showAssignmentForm = signal(false);
  readonly showContactForm = signal(false);
  readonly formMode = signal<FormMode>('create');
  readonly selected = signal<Employee | null>(null);
  readonly assignments = signal<EmployeeAssignment[]>([]);
  readonly contacts = signal<EmergencyContact[]>([]);
  readonly periods = signal<EmploymentPeriod[]>([]);
  readonly terminations = signal<EmployeeTermination[]>([]);
  readonly userActivationRequired = signal(false);
  readonly departments = signal<DepartmentOption[]>([]);
  readonly positions = signal<PositionOption[]>([]);
  readonly tenantUsers = signal<TenantUserResponse[]>([]);
  readonly nationalities = ['Boliviana', 'Argentina', 'Brasileña', 'Chilena', 'Colombiana', 'Ecuatoriana', 'Mexicana', 'Paraguaya', 'Peruana', 'Uruguaya', 'Estadounidense', 'Española'];
  confirmationOpen = false;
  confirmationTitle = '';
  confirmationMessage = '';
  confirmationMode: ConfirmationMode = 'none';
  confirmationValue = '';
  confirmationPlaceholder = '';
  confirmationDateValue = '';
  confirmationDateMinimum = '';
  pendingConfirmation: ((value: string, date?: string) => void) | null = null;

  nameFilter = '';
  numberFilter = '';
  statusFilter = '';
  form: EmployeeRequest = this.emptyEmployee();
  assignmentForm: EmployeeAssignmentRequest = this.emptyAssignment();
  contactForm: EmergencyContactRequest = this.emptyContact();

  ngOnInit(): void {
    forkJoin({ departments: this.related.departments(), positions: this.related.positions() }).subscribe({
      next: result => {
        this.departments.set(result.departments.data.content.filter(item => item.status === 'ACTIVE'));
        this.positions.set(result.positions.data.content.filter(item => item.status === 'ACTIVE'));
      },
      error: () => { this.departments.set([]); this.positions.set([]); }
    });
    this.loadTenantUsers();
    this.load();
  }

  private loadTenantUsers(): void {
    if (!this.permissions.hasPermission('users.list')) return;
    this.usersService.getUsers(0, 200).subscribe({
      next: response => this.tenantUsers.set(response.data?.content ?? []),
      error: () => this.tenantUsers.set([])
    });
  }

  userLabel(user: TenantUserResponse): string {
    const name = [user.firstName, user.lastName].filter(Boolean).join(' ');
    return name ? `${name} · ${user.email}` : user.email;
  }

  canLinkUser(user: TenantUserResponse): boolean {
    const status = String(user.status ?? '').toUpperCase();
    return status === 'ACTIVE' || status === 'PENDING' || (user.active && !status);
  }

  userAccessStatus(user: TenantUserResponse): string {
    const status = String(user.status ?? '').toUpperCase();
    if (status === 'PENDING') return 'Pendiente de verificación';
    if (status === 'ACTIVE' || user.active) return 'Activa';
    return 'Inactiva';
  }

  load(page = 0): void {
    this.loading.set(true);
    this.service.list(page, 25, { name: this.nameFilter, employeeNumber: this.numberFilter, status: this.statusFilter }).subscribe({
      next: response => {
        this.employees.set(response.data.content);
        this.totalElements.set(response.data.totalElements);
        this.totalPages.set(response.data.totalPages);
        this.currentPage.set(response.data.number);
        this.loading.set(false);
      },
      error: error => { this.loading.set(false); this.handleError(error, 'No se pudieron cargar los empleados'); }
    });
  }

  clearFilters(): void { this.nameFilter = ''; this.numberFilter = ''; this.statusFilter = ''; this.load(0); }
  activeCount(): number { return this.employees().filter(item => item.employmentStatus === 'ACTIVE').length; }

  openCreate(): void {
    this.formMode.set('create'); this.selected.set(null); this.form = this.emptyEmployee();
    this.assignmentForm = this.emptyAssignment(); this.showForm.set(true);
  }
  openEdit(id: string): void {
    this.service.get(id).subscribe({
      next: response => { this.selected.set(response.data); this.form = this.toRequest(response.data); this.formMode.set('edit'); this.showForm.set(true); },
      error: error => this.handleError(error, 'No se pudo consultar el empleado')
    });
  }
  closeForm(): void { if (!this.saving()) this.showForm.set(false); }
  save(): void {
    if (!this.form.firstName.trim() || !this.form.lastName.trim() || !this.form.documentType.trim() || !this.form.documentNumber.trim() || !this.form.hireDate) {
      this.toast.warning('Complete nombre, apellido, documento y fecha de ingreso.'); return;
    }
    if (this.form.birthDate && !this.isAdult(this.form.birthDate)) {
      this.toast.warning('El empleado debe tener al menos 18 años.'); return;
    }
    const creating = this.formMode() === 'create';
    if (creating && (!this.assignmentForm.departmentId || !this.assignmentForm.positionId || !this.assignmentForm.startDate)) {
      this.toast.warning('Seleccione el departamento, cargo y fecha de inicio de la asignación laboral.'); return;
    }
    this.saving.set(true);
    const selected = this.selected();
    const request = this.cleanEmployee(this.form);
    const assignment = this.cleanAssignment(this.assignmentForm);
    const operation: Observable<unknown> = !creating && selected
      ? this.service.update(selected.id, request)
      : this.service.createWithAssignment({ employee: request, assignment });
    operation.subscribe({
      next: () => {
        this.saving.set(false); this.showForm.set(false);
        this.toast.success(creating ? 'Empleado y asignación laboral guardados correctamente.' : 'Empleado actualizado correctamente.');
        this.load(this.currentPage());
      },
      error: (error: unknown) => { this.saving.set(false); this.handleError(error, 'No se pudo guardar el empleado'); }
    });
  }

  changeStatus(employee: EmployeeSummary): void {
    const target: EmploymentStatus = employee.employmentStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    this.openConfirmation(
      `${target === 'ACTIVE' ? 'Activar' : 'Desactivar'} empleado`,
      `Confirma el cambio de estado de ${employee.firstName} ${employee.lastName}.`,
      'reason', 'Motivo del cambio', reason => {
        if (!reason) return;
        this.service.changeStatus(employee.id, target, reason).subscribe({
          next: () => { this.toast.success('Estado del empleado actualizado.'); this.load(this.currentPage()); },
          error: error => this.handleError(error, 'No se pudo cambiar el estado')
        });
      });
  }

  openProfile(id: string): void {
    this.service.get(id).subscribe({
      next: response => { this.selected.set(response.data); this.userActivationRequired.set(false); this.showProfile.set(true); this.loadRelations(id); },
      error: error => this.handleError(error, 'No se pudo consultar la ficha del empleado')
    });
  }
  loadRelations(employeeId: string): void {
    this.service.assignments(employeeId).subscribe({ next: r => this.assignments.set(r.data.content), error: () => this.assignments.set([]) });
    this.service.contacts(employeeId).subscribe({ next: r => this.contacts.set(r.data.content), error: () => this.contacts.set([]) });
    this.service.periods(employeeId).subscribe({ next: r => this.periods.set(r.data), error: () => this.periods.set([]) });
    this.service.terminations(employeeId).subscribe({ next: r => this.terminations.set(r.data), error: () => this.terminations.set([]) });
  }

  rehireSelected(): void {
    const employee = this.selected();
    if (!employee || employee.employmentStatus !== 'TERMINATED') return;
    const latestEndDate = this.periods().find(period => period.status === 'TERMINATED')?.endDate;
    const startDate = latestEndDate && latestEndDate >= this.today() ? this.addDays(latestEndDate, 1) : this.today();
    this.openConfirmation('Recontratar empleado', 'La recontratación abrirá un nuevo período laboral desde la fecha indicada.', 'rehire', 'Motivo de la recontratación', (reason, date) => {
      if (!reason || !date) return;
      this.saving.set(true);
      this.service.rehire(employee.id, { startDate: date, reason, assignment: null }).subscribe({
        next: response => {
          this.saving.set(false);
          this.selected.set(response.data.employee);
          this.userActivationRequired.set(response.data.userActivationRequired === true);
          this.loadRelations(employee.id);
          this.load(this.currentPage());
          if (response.data.userActivationRequired) {
            this.toast.warning('Empleado recontratado. Un administrador debe activar la cuenta vinculada para permitir el acceso.');
          } else {
            this.toast.success('Empleado recontratado correctamente.');
          }
        },
        error: error => { this.saving.set(false); this.handleError(error, 'No se pudo recontratar al empleado'); }
      });
    });
  }

  openNewAssignment(): void { this.assignmentForm = this.emptyAssignment(); this.showAssignmentForm.set(true); }
  saveAssignment(): void {
    const employee = this.selected();
    if (!employee || !this.assignmentForm.departmentId || !this.assignmentForm.positionId || !this.assignmentForm.startDate) {
      this.toast.warning('Seleccione departamento, cargo y fecha de inicio.'); return;
    }
    this.saving.set(true);
    this.service.createAssignment(employee.id, { ...this.assignmentForm, supervisorEmployeeId: this.nullable(this.assignmentForm.supervisorEmployeeId), endDate: this.nullable(this.assignmentForm.endDate), reason: this.nullable(this.assignmentForm.reason) }).subscribe({
      next: () => { this.saving.set(false); this.showAssignmentForm.set(false); this.toast.success('Asignación laboral registrada.'); this.loadRelations(employee.id); },
      error: error => { this.saving.set(false); this.handleError(error, 'No se pudo registrar la asignación'); }
    });
  }
  closeAssignment(assignment: EmployeeAssignment): void {
    const employee = this.selected(); if (!employee) return;
    this.openConfirmation('Cerrar asignación', 'Define la fecha efectiva de finalización.', 'date', '', endDate => {
      if (!endDate) return;
      this.service.closeAssignment(employee.id, assignment.id, endDate).subscribe({
        next: () => { this.toast.success('Asignación laboral cerrada.'); this.loadRelations(employee.id); },
        error: error => this.handleError(error, 'No se pudo cerrar la asignación')
      });
    }, this.today());
  }
  cancelAssignment(assignment: EmployeeAssignment): void {
    const employee = this.selected();
    if (!employee || assignment.status !== 'ACTIVE') return;
    this.openConfirmation('Cancelar asignación', '¿Deseas cancelar esta asignación futura?', 'none', '', () => {
      this.service.cancelAssignment(employee.id, assignment.id).subscribe({
        next: () => { this.toast.success('Asignación futura cancelada.'); this.loadRelations(employee.id); },
        error: error => this.handleError(error, 'No se pudo cancelar la asignación')
      });
    });
  }
  terminateSelected(): void {
    const employee = this.selected();
    if (!employee || employee.employmentStatus === 'TERMINATED') return;
    this.openConfirmation('Desvincular empleado', 'Esta acción finaliza la relación laboral y no se puede revertir.', 'reason', 'Motivo de desvinculación', reason => {
      if (!reason) return;
      this.service.terminate(employee.id, { effectiveDate: this.today(), reason }).subscribe({
        next: response => { this.selected.set((response.data as any).employee); this.toast.success('Relación laboral finalizada.'); this.load(); },
        error: error => this.handleError(error, 'No se pudo desvincular al empleado')
      });
    });
  }

  openNewContact(): void { this.contactForm = this.emptyContact(); this.showContactForm.set(true); }
  saveContact(): void {
    const employee = this.selected();
    if (!employee || !this.contactForm.fullName.trim() || !this.contactForm.phone.trim()) { this.toast.warning('Complete nombre y teléfono.'); return; }
    this.saving.set(true);
    this.service.createContact(employee.id, { ...this.contactForm, relationship: this.nullable(this.contactForm.relationship), alternativePhone: this.nullable(this.contactForm.alternativePhone), address: this.nullable(this.contactForm.address) }).subscribe({
      next: () => { this.saving.set(false); this.showContactForm.set(false); this.toast.success('Contacto de emergencia registrado.'); this.loadRelations(employee.id); },
      error: error => { this.saving.set(false); this.handleError(error, 'No se pudo registrar el contacto'); }
    });
  }
  deleteContact(contact: EmergencyContact): void {
    const employee = this.selected(); if (!employee) return;
    this.openConfirmation('Eliminar contacto', `¿Eliminar el contacto ${contact.fullName}?`, 'none', '', () => {
      this.service.deleteContact(employee.id, contact.id).subscribe({
        next: () => { this.toast.success('Contacto eliminado.'); this.loadRelations(employee.id); },
        error: error => this.handleError(error, 'No se pudo eliminar el contacto')
      });
    });
  }

  departmentName(id: string): string { return this.departments().find(item => item.id === id)?.name ?? 'Departamento'; }
  positionName(id: string): string { return this.positions().find(item => item.id === id)?.name ?? 'Cargo'; }
  statusLabel(status: EmploymentStatus): string { return ({ ACTIVE: 'Activo', ON_LEAVE: 'Con licencia', SUSPENDED: 'Suspendido', TERMINATED: 'Desvinculado', INACTIVE: 'Inactivo' })[status]; }
  statusClass(status: EmploymentStatus): string { return ({ ACTIVE: 'bg-primary/10 text-primary', ON_LEAVE: 'bg-accent text-accent-foreground', SUSPENDED: 'bg-muted text-muted-foreground', TERMINATED: 'bg-destructive/10 text-destructive', INACTIVE: 'bg-muted text-muted-foreground' })[status]; }
  assignmentTypeLabel(type: string): string { return ({ PRIMARY: 'Principal', TEMPORARY: 'Temporal', ADDITIONAL: 'Adicional' } as Record<string, string>)[type] ?? type; }

  private emptyEmployee(): EmployeeRequest { return { tenantUserId: null, firstName: '', middleName: null, lastName: '', secondLastName: null, documentType: 'CI', documentNumber: '', birthDate: null, gender: null, nationality: 'Boliviana', personalEmail: null, workEmail: null, phone: null, mobilePhone: null, address: null, hireDate: this.today() }; }
  private emptyAssignment(): EmployeeAssignmentRequest { return { departmentId: '', positionId: '', supervisorEmployeeId: null, startDate: this.today(), endDate: null, assignmentType: 'PRIMARY', reason: null }; }
  private emptyContact(): EmergencyContactRequest { return { fullName: '', relationship: null, phone: '', alternativePhone: null, address: null, primary: true }; }
  private toRequest(e: Employee): EmployeeRequest { const { id: _id, terminationDate: _terminationDate, employmentStatus: _status, createdAt: _createdAt, updatedAt: _updatedAt, ...request } = e; return request; }
  private cleanEmployee(e: EmployeeRequest): EmployeeRequest { return { ...e, tenantUserId: this.nullable(e.tenantUserId), middleName: this.nullable(e.middleName), secondLastName: this.nullable(e.secondLastName), birthDate: this.nullable(e.birthDate), gender: this.nullable(e.gender), nationality: this.nullable(e.nationality), personalEmail: this.nullable(e.personalEmail), workEmail: this.nullable(e.workEmail), phone: this.nullable(e.phone), mobilePhone: this.nullable(e.mobilePhone), address: this.nullable(e.address) }; }
  private cleanAssignment(a: EmployeeAssignmentRequest): EmployeeAssignmentRequest { return { ...a, supervisorEmployeeId: this.nullable(a.supervisorEmployeeId), endDate: this.nullable(a.endDate), reason: this.nullable(a.reason) }; }
  private nullable(value: string | null): string | null { return value?.trim() || null; }
  private today(): string { return new Date().toISOString().slice(0, 10); }
  maxBirthDate(): string {
    const today = new Date();
    today.setFullYear(today.getFullYear() - 18);
    return today.toISOString().slice(0, 10);
  }
  private isAdult(birthDate: string): boolean {
    return birthDate <= this.maxBirthDate();
  }
  private openConfirmation(title: string, message: string, mode: ConfirmationMode,
                          placeholder: string, action: (value: string, date?: string) => void, value = '', date = ''): void {
    this.confirmationTitle = title;
    this.confirmationMessage = message;
    this.confirmationMode = mode;
    this.confirmationPlaceholder = placeholder;
    this.confirmationValue = value;
    this.confirmationDateValue = date;
    this.confirmationDateMinimum = date;
    this.pendingConfirmation = action;
    this.confirmationOpen = true;
  }
  closeConfirmation(): void { this.confirmationOpen = false; this.pendingConfirmation = null; this.confirmationValue = ''; this.confirmationDateValue = ''; this.confirmationDateMinimum = ''; }
  acceptConfirmation(): void {
    const value = this.confirmationValue.trim();
    if (this.confirmationMode !== 'none' && !value) {
      this.toast.warning(this.confirmationMode === 'date' ? 'Seleccione una fecha.' : 'Ingrese un motivo.');
      return;
    }
    if (this.confirmationMode === 'rehire' && !this.confirmationDateValue) {
      this.toast.warning('Seleccione la fecha de recontratación.');
      return;
    }
    const action = this.pendingConfirmation;
    const date = this.confirmationDateValue;
    this.closeConfirmation();
    action?.(value, date);
  }
  private addDays(value: string, days: number): string {
    const date = new Date(`${value}T00:00:00`);
    date.setDate(date.getDate() + days);
    return date.toISOString().slice(0, 10);
  }
  private handleError(error: unknown, fallback: string): void {
    const body = error instanceof HttpErrorResponse ? error.error : null;
    const message = body?.errors?.[0] || body?.message || body?.error || fallback;
    this.toast.error(String(message));
  }
}
