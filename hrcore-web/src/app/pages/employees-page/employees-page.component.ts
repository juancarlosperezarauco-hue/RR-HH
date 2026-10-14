import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { forkJoin, Observable } from 'rxjs';
import { HasPermissionPipe } from '../../shared/api';
import { ToastService } from '../../shared/ui/toast/toast.service';
import { ContractsService, DepartmentOption, PositionOption } from '../../entities/contracts';
import {
  EmergencyContact, EmergencyContactRequest, Employee, EmployeeAssignment,
  EmployeeAssignmentRequest, EmployeeRequest, EmployeeSummary, EmployeesService,
  EmploymentStatus
} from '../../entities/employees';

type FormMode = 'create' | 'edit';

@Component({
  selector: 'app-employees-page',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule, HasPermissionPipe],
  templateUrl: './employees-page.component.html'
})
export class EmployeesPageComponent implements OnInit {
  private readonly service = inject(EmployeesService);
  private readonly related = inject(ContractsService);
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
  readonly formMode = signal<FormMode>('create');
  readonly selected = signal<Employee | null>(null);
  readonly assignments = signal<EmployeeAssignment[]>([]);
  readonly contacts = signal<EmergencyContact[]>([]);
  readonly departments = signal<DepartmentOption[]>([]);
  readonly positions = signal<PositionOption[]>([]);

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
    this.load();
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
    if (!this.form.employeeNumber.trim() || !this.form.firstName.trim() || !this.form.lastName.trim() || !this.form.documentType.trim() || !this.form.documentNumber.trim() || !this.form.hireDate) {
      this.toast.warning('Complete el código, nombre, apellido, documento y fecha de ingreso.'); return;
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
    if (!window.confirm(`¿${target === 'ACTIVE' ? 'Activar' : 'Desactivar'} al empleado ${employee.firstName} ${employee.lastName}?`)) return;
    this.service.changeStatus(employee.id, target).subscribe({
      next: () => { this.toast.success('Estado del empleado actualizado.'); this.load(this.currentPage()); },
      error: error => this.handleError(error, 'No se pudo cambiar el estado')
    });
  }

  openProfile(id: string): void {
    this.service.get(id).subscribe({
      next: response => { this.selected.set(response.data); this.showProfile.set(true); this.loadRelations(id); },
      error: error => this.handleError(error, 'No se pudo consultar la ficha del empleado')
    });
  }
  loadRelations(employeeId: string): void {
    this.service.assignments(employeeId).subscribe({ next: r => this.assignments.set(r.data.content), error: () => this.assignments.set([]) });
    this.service.contacts(employeeId).subscribe({ next: r => this.contacts.set(r.data.content), error: () => this.contacts.set([]) });
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
    const endDate = window.prompt('Fecha de finalización (AAAA-MM-DD):', this.today());
    if (!endDate) return;
    this.service.closeAssignment(employee.id, assignment.id, endDate).subscribe({
      next: () => { this.toast.success('Asignación laboral cerrada.'); this.loadRelations(employee.id); },
      error: error => this.handleError(error, 'No se pudo cerrar la asignación')
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
    const employee = this.selected(); if (!employee || !window.confirm(`¿Eliminar el contacto ${contact.fullName}?`)) return;
    this.service.deleteContact(employee.id, contact.id).subscribe({
      next: () => { this.toast.success('Contacto eliminado.'); this.loadRelations(employee.id); },
      error: error => this.handleError(error, 'No se pudo eliminar el contacto')
    });
  }

  departmentName(id: string): string { return this.departments().find(item => item.id === id)?.name ?? 'Departamento'; }
  positionName(id: string): string { return this.positions().find(item => item.id === id)?.name ?? 'Cargo'; }
  statusLabel(status: EmploymentStatus): string { return ({ ACTIVE: 'Activo', ON_LEAVE: 'Con licencia', SUSPENDED: 'Suspendido', TERMINATED: 'Desvinculado', INACTIVE: 'Inactivo' })[status]; }
  statusClass(status: EmploymentStatus): string { return ({ ACTIVE: 'bg-emerald-100 text-emerald-800', ON_LEAVE: 'bg-blue-100 text-blue-800', SUSPENDED: 'bg-amber-100 text-amber-800', TERMINATED: 'bg-red-100 text-red-800', INACTIVE: 'bg-slate-100 text-slate-700' })[status]; }
  assignmentTypeLabel(type: string): string { return ({ PRIMARY: 'Principal', TEMPORARY: 'Temporal', ADDITIONAL: 'Adicional' } as Record<string, string>)[type] ?? type; }

  private emptyEmployee(): EmployeeRequest { return { tenantUserId: null, employeeNumber: '', firstName: '', middleName: null, lastName: '', secondLastName: null, documentType: 'CI', documentNumber: '', birthDate: null, gender: null, nationality: 'Boliviana', personalEmail: null, workEmail: null, phone: null, mobilePhone: null, address: null, hireDate: this.today() }; }
  private emptyAssignment(): EmployeeAssignmentRequest { return { departmentId: '', positionId: '', supervisorEmployeeId: null, startDate: this.today(), endDate: null, assignmentType: 'PRIMARY', reason: null }; }
  private emptyContact(): EmergencyContactRequest { return { fullName: '', relationship: null, phone: '', alternativePhone: null, address: null, primary: true }; }
  private toRequest(e: Employee): EmployeeRequest { const { id: _id, terminationDate: _terminationDate, employmentStatus: _status, createdAt: _createdAt, updatedAt: _updatedAt, ...request } = e; return request; }
  private cleanEmployee(e: EmployeeRequest): EmployeeRequest { return { ...e, tenantUserId: this.nullable(e.tenantUserId), middleName: this.nullable(e.middleName), secondLastName: this.nullable(e.secondLastName), birthDate: this.nullable(e.birthDate), gender: this.nullable(e.gender), nationality: this.nullable(e.nationality), personalEmail: this.nullable(e.personalEmail), workEmail: this.nullable(e.workEmail), phone: this.nullable(e.phone), mobilePhone: this.nullable(e.mobilePhone), address: this.nullable(e.address) }; }
  private cleanAssignment(a: EmployeeAssignmentRequest): EmployeeAssignmentRequest { return { ...a, supervisorEmployeeId: this.nullable(a.supervisorEmployeeId), endDate: this.nullable(a.endDate), reason: this.nullable(a.reason) }; }
  private nullable(value: string | null): string | null { return value?.trim() || null; }
  private today(): string { return new Date().toISOString().slice(0, 10); }
  private handleError(error: unknown, fallback: string): void { const message = error instanceof HttpErrorResponse ? (error.error?.message || error.error?.error || fallback) : fallback; this.toast.error(String(message)); }
}
