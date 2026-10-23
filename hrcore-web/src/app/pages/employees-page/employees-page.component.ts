import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { catchError, forkJoin, Observable, of } from 'rxjs';
import { HasPermissionPipe } from '../../shared/api';
import { ToastService } from '../../shared/ui/toast/toast.service';
import { ContractsService, DepartmentOption, PositionOption } from '../../entities/contracts';
import { UserService, TenantUserResponse } from '../../entities/user';
import { PermissionService } from '../../shared/lib/auth/permission.service';
import { AuthFacade } from '../../shared/lib/auth/auth.facade';
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
  private readonly auth = inject(AuthFacade);
  private readonly toast = inject(ToastService);

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
  readonly formSubmitted = signal(false);
  readonly assignmentSubmitted = signal(false);
  readonly contactSubmitted = signal(false);
  readonly touchedFields = signal<Set<string>>(new Set());
  readonly formMode = signal<FormMode>('create');
  readonly selected = signal<Employee | null>(null);
  readonly assignments = signal<EmployeeAssignment[]>([]);
  readonly contacts = signal<EmergencyContact[]>([]);
  readonly periods = signal<EmploymentPeriod[]>([]);
  readonly terminations = signal<EmployeeTermination[]>([]);
  readonly userActivationRequired = signal(false);
  readonly relationsLoading = signal(false);
  readonly relationErrors = signal<string[]>([]);
  readonly departments = signal<DepartmentOption[]>([]);
  readonly positions = signal<PositionOption[]>([]);
  readonly tenantUsers = signal<TenantUserResponse[]>([]);
  readonly tenantUserPage = signal(0);
  readonly tenantUserTotalPages = signal(1);
  readonly tenantUserTotalElements = signal(0);
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
  tenantUserSearch = '';
  readonly tenantUserPageSize = 25;
  readonly editingAssignment = signal<EmployeeAssignment | null>(null);
  readonly editingContact = signal<EmergencyContact | null>(null);
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

  private loadTenantUsers(page = 0): void {
    if (!this.permissions.hasPermission('users.list')) return;
    this.usersService.getUsers(page, this.tenantUserPageSize, { search: this.tenantUserSearch }).subscribe({
      next: response => {
        this.tenantUsers.set((response.data?.content ?? []).filter(user => !this.isOwnerUser(user)));
        this.tenantUserPage.set(response.data?.number ?? page);
        this.tenantUserTotalPages.set(Math.max(1, response.data?.totalPages ?? 1));
        this.tenantUserTotalElements.set(response.data?.totalElements ?? response.data?.content?.length ?? 0);
      },
      error: () => this.tenantUsers.set([])
    });
  }

  searchTenantUsers(): void { this.loadTenantUsers(0); }
  onTenantUserSearchEnter(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.searchTenantUsers();
  }
  changeTenantUserPage(delta: number): void {
    const next = this.tenantUserPage() + delta;
    if (next < 0 || next >= this.tenantUserTotalPages()) return;
    this.loadTenantUsers(next);
  }
  private ensureTenantUser(id: string | null): void {
    if (!id || this.tenantUsers().some(user => user.id === id) || !this.permissions.hasPermission('users.detail')) return;
    this.usersService.getUserById(id).subscribe({
      next: response => {
        if (this.isOwnerUser(response.data)) return;
        this.tenantUsers.update(users => users.some(user => user.id === id) ? users : [response.data, ...users]);
      }
    });
  }

  userLabel(user: TenantUserResponse): string {
    const name = [user.firstName, user.lastName].filter(Boolean).join(' ');
    return name ? `${name} · ${user.email}` : user.email;
  }

  canLinkUser(user: TenantUserResponse): boolean {
    if (this.isOwnerUser(user)) return false;
    const status = String(user.status ?? '').toUpperCase();
    return status === 'ACTIVE' || status === 'PENDING' || (user.active && !status);
  }

  private isOwnerUser(user: TenantUserResponse): boolean {
    const hasOwnerRole = user.roles?.some(role => role.code?.toUpperCase() === 'OWNER_ADMIN') ?? false;
    const currentOwner = this.permissions.isCurrentOwner() && this.auth.currentUser()?.id === user.id;
    return user.owner === true || hasOwnerRole || currentOwner;
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
    this.assignmentForm = this.emptyAssignment(); this.formSubmitted.set(false); this.touchedFields.set(new Set()); this.showForm.set(true);
  }
  openEdit(id: string): void {
    this.service.get(id).subscribe({
      next: response => { this.selected.set(response.data); this.form = this.toRequest(response.data); this.ensureTenantUser(response.data.tenantUserId); this.formMode.set('edit'); this.formSubmitted.set(false); this.touchedFields.set(new Set()); this.showForm.set(true); },
      error: error => this.handleError(error, 'No se pudo consultar el empleado')
    });
  }
  closeForm(): void { if (!this.saving()) { this.formSubmitted.set(false); this.touchedFields.set(new Set()); this.showForm.set(false); } }
  touchField(field: string): void {
    this.touchedFields.update(fields => new Set(fields).add(field));
  }
  fieldHasError(field: string, invalid: boolean): boolean {
    return invalid && (this.formSubmitted() || this.touchedFields().has(field));
  }
  isBirthDateInvalid(): boolean {
    return Boolean(this.form.birthDate && !this.isAdult(this.form.birthDate));
  }
  isEmailInvalid(value: string | null | undefined): boolean {
    if (!value || !value.trim()) return false;
    return !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim());
  }
  hasFormValidationErrors(): boolean {
    if (!this.formSubmitted() && this.touchedFields().size === 0) return false;
    if (!this.form.firstName.trim() || !this.form.lastName.trim() || !this.form.documentType.trim() || !this.form.documentNumber.trim() || !this.form.hireDate) {
      return true;
    }
    if (this.isBirthDateInvalid() || this.fieldHasError('personalEmail', this.isEmailInvalid(this.form.personalEmail)) || this.fieldHasError('workEmail', this.isEmailInvalid(this.form.workEmail))) {
      return true;
    }
    if (this.formMode() === 'create' && (!this.assignmentForm.departmentId || !this.assignmentForm.positionId || !this.assignmentForm.startDate)) {
      return true;
    }
    return false;
  }
  save(): void {
    this.formSubmitted.set(true);
    if (!this.form.firstName.trim() || !this.form.lastName.trim() || !this.form.documentType.trim() || !this.form.documentNumber.trim() || !this.form.hireDate) {
      this.toast.warning('Complete los campos obligatorios: nombre, apellido, documento y fecha de ingreso.'); return;
    }
    if (this.isBirthDateInvalid()) {
      this.toast.warning('El empleado debe tener al menos 18 años.'); return;
    }
    if (this.isEmailInvalid(this.form.personalEmail) || this.isEmailInvalid(this.form.workEmail)) {
      this.toast.warning('Verifica el formato de los correos electrónicos ingresados.'); return;
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
        this.saving.set(false); this.formSubmitted.set(false); this.showForm.set(false);
        this.toast.success(creating ? 'Empleado y asignación laboral guardados correctamente.' : 'Empleado actualizado correctamente.');
        this.load(this.currentPage());
      },
      error: (error: unknown) => { this.saving.set(false); this.handleError(error, 'No se pudo guardar el empleado'); }
    });
  }

  changeStatus(employee: EmployeeSummary): void {
    if (employee.employmentStatus === 'TERMINATED') return;
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
    this.relationsLoading.set(true);
    this.relationErrors.set([]);
    this.assignments.set([]);
    this.contacts.set([]);
    this.periods.set([]);
    this.terminations.set([]);
    forkJoin({
      assignments: this.service.assignments(employeeId).pipe(catchError(() => of(null))),
      contacts: this.service.contacts(employeeId).pipe(catchError(() => of(null))),
      periods: this.service.periods(employeeId).pipe(catchError(() => of(null))),
      terminations: this.service.terminations(employeeId).pipe(catchError(() => of(null)))
    }).subscribe(result => {
      const errors: string[] = [];
      if (result.assignments) this.assignments.set(result.assignments.data.content); else errors.push('asignaciones');
      if (result.contacts) this.contacts.set(result.contacts.data.content); else errors.push('contactos');
      if (result.periods) this.periods.set(result.periods.data); else errors.push('períodos');
      if (result.terminations) this.terminations.set(result.terminations.data); else errors.push('desvinculaciones');
      this.relationErrors.set(errors);
      this.relationsLoading.set(false);
    });
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

  openNewAssignment(): void { this.editingAssignment.set(null); this.assignmentForm = this.emptyAssignment(); this.assignmentSubmitted.set(false); this.showAssignmentForm.set(true); }
  openEditAssignment(assignment: EmployeeAssignment): void {
    if (!this.isFutureAssignment(assignment)) return;
    this.editingAssignment.set(assignment);
    this.assignmentForm = { departmentId: assignment.departmentId, positionId: assignment.positionId, supervisorEmployeeId: assignment.supervisorEmployeeId, startDate: assignment.startDate, endDate: assignment.endDate, assignmentType: assignment.assignmentType, reason: assignment.reason };
    this.assignmentSubmitted.set(false);
    this.showAssignmentForm.set(true);
  }
  saveAssignment(): void {
    this.assignmentSubmitted.set(true);
    const employee = this.selected();
    if (!employee || !this.assignmentForm.departmentId || !this.assignmentForm.positionId || !this.assignmentForm.startDate) {
      this.toast.warning('Seleccione departamento, cargo y fecha de inicio.'); return;
    }
    this.saving.set(true);
    const payload = { ...this.assignmentForm, supervisorEmployeeId: this.nullable(this.assignmentForm.supervisorEmployeeId), endDate: this.nullable(this.assignmentForm.endDate), reason: this.nullable(this.assignmentForm.reason) };
    const editing = this.editingAssignment();
    const operation = editing ? this.service.updateAssignment(employee.id, editing.id, payload) : this.service.createAssignment(employee.id, payload);
    operation.subscribe({
      next: () => { this.saving.set(false); this.assignmentSubmitted.set(false); this.showAssignmentForm.set(false); this.editingAssignment.set(null); this.toast.success(editing ? 'Asignación laboral actualizada.' : 'Asignación laboral registrada.'); this.loadRelations(employee.id); },
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
  isFutureAssignment(assignment: EmployeeAssignment): boolean { return assignment.startDate > this.today(); }
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

  openNewContact(): void { this.editingContact.set(null); this.contactForm = this.emptyContact(); this.contactSubmitted.set(false); this.showContactForm.set(true); }
  openEditContact(contact: EmergencyContact): void {
    this.editingContact.set(contact);
    this.contactForm = { fullName: contact.fullName, relationship: contact.relationship, phone: contact.phone, alternativePhone: contact.alternativePhone, address: contact.address, primary: contact.primary };
    this.contactSubmitted.set(false);
    this.showContactForm.set(true);
  }
  saveContact(): void {
    this.contactSubmitted.set(true);
    const employee = this.selected();
    if (!employee || !this.contactForm.fullName.trim() || !this.contactForm.phone.trim()) { this.toast.warning('Complete nombre y teléfono.'); return; }
    this.saving.set(true);
    const payload = { ...this.contactForm, relationship: this.nullable(this.contactForm.relationship), alternativePhone: this.nullable(this.contactForm.alternativePhone), address: this.nullable(this.contactForm.address) };
    const editing = this.editingContact();
    const operation = editing ? this.service.updateContact(employee.id, editing.id, payload) : this.service.createContact(employee.id, payload);
    operation.subscribe({
      next: () => { this.saving.set(false); this.contactSubmitted.set(false); this.showContactForm.set(false); this.editingContact.set(null); this.toast.success(editing ? 'Contacto actualizado.' : 'Contacto de emergencia registrado.'); this.loadRelations(employee.id); },
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
  private today(): string { const date = new Date(); return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`; }
  maxBirthDate(): string {
    const today = new Date();
    today.setFullYear(today.getFullYear() - 18);
    return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
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
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }
  private handleError(error: unknown, fallback: string): void {
    const body = error instanceof HttpErrorResponse ? error.error : null;
    const message = body?.errors?.[0] || body?.message || body?.error || fallback;
    this.toast.error(String(message));
  }
}
