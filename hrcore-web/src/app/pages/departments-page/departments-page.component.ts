import { CommonModule } from '@angular/common';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { finalize, Observable, Subscription } from 'rxjs';
import { HasPermissionPipe, ApiResponse } from '../../shared/api';
import { PermissionService } from '../../shared/lib/auth/permission.service';
import { ToastService } from '../../shared/ui/toast/toast.service';
import { Department, DepartmentFilters, DepartmentHistory, DepartmentRequest, DepartmentsService } from '../../entities/departments';
import { ContractsService, EmployeeOption } from '../../entities/contracts';
import { PageResponse } from '../../entities/access';
import { environment } from '../../../environments/environment';

@Component({ selector: 'app-departments-page', standalone: true, imports: [CommonModule, FormsModule, HasPermissionPipe, LucideAngularModule], templateUrl: './departments-page.component.html' })
export class DepartmentsPageComponent implements OnInit {
  private readonly service = inject(DepartmentsService);
  private readonly contracts = inject(ContractsService);
  private readonly http = inject(HttpClient);
  private readonly destroy = inject(DestroyRef);
  private readonly toast = inject(ToastService);
  readonly permissions = inject(PermissionService);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly relatedLoading = signal(false);
  readonly items = signal<Department[]>([]);
  readonly parents = signal<Department[]>([]);
  readonly employees = signal<EmployeeOption[]>([]);
  readonly historyItems = signal<DepartmentHistory[]>([]);
  readonly error = signal('');
  readonly relatedError = signal('');
  readonly panelError = signal('');
  readonly historyLoading = signal(false);
  page = 0; totalPages = 0; total = 0; size = 25; sort = 'name,asc';
  private parentRequest = 0; private employeeRequest = 0;
  private listRequest?: Subscription;
  parentPage = 0; parentPages = 0; parentSearch = '';
  employeePage = 0; employeePages = 0; employeeSearch = '';
  historyPage = 0; historyPages = 0;
  filters: DepartmentFilters = { name: '', code: '', status: '', parentDepartmentId: '' };
  selected: Department | null = null;
  form: DepartmentRequest = this.emptyForm();
  mode: 'create' | 'edit' | 'detail' | 'history' | null = null;
  readonly uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
  ngOnInit() { this.load(0); }
  load(page = this.page, refresh = false) {
    if (this.loading() && !refresh) return;
    if (this.filters.parentDepartmentId && !this.uuidPattern.test(this.filters.parentDepartmentId)) { this.toast.warning('El filtro de departamento superior debe ser un UUID válido.'); return; }
    // A successful mutation replaces an older query instead of losing its refresh.
    this.listRequest?.unsubscribe();
    this.loading.set(true); this.error.set('');
    this.listRequest = this.service.list(page, this.size, this.filters, this.sort).pipe(takeUntilDestroyed(this.destroy), finalize(() => this.loading.set(false))).subscribe({
      next: r => { this.items.set(r.data.content); this.page = r.data.number; this.totalPages = r.data.totalPages; this.total = r.data.totalElements; },
      error: e => { this.items.set([]); this.error.set(this.message(e)); this.toast.error(this.error()); }
    });
  }
  showChildren(item: Department) { this.filters = { name: '', code: '', status: '', parentDepartmentId: item.id }; this.load(0); }
  openCreate() {
    if (this.saving() || !this.permissions.hasPermission('departments.create')) return;
    this.selected = null; this.form = this.emptyForm(); this.mode = 'create'; this.panelError.set(''); this.loadParents(0); this.loadEmployees(0);
  }
  open(item: Department, mode: 'edit' | 'detail') {
    if (this.saving() || !this.permissions.hasPermission(`departments.${mode === 'edit' ? 'update' : 'detail'}`)) return;
    this.selected = item; this.form = this.fromDepartment(item); this.mode = mode; this.panelError.set('');
    if (mode === 'edit') { this.loadParents(0); this.loadEmployees(0); }
    if (this.permissions.hasPermission('departments.detail')) {
      this.relatedLoading.set(true);
      this.service.get(item.id).pipe(takeUntilDestroyed(this.destroy), finalize(() => this.relatedLoading.set(false))).subscribe({
        next: r => { this.selected = r.data; this.form = this.fromDepartment(r.data); }, error: e => this.panelError.set(this.message(e))
      });
    }
  }
  loadParents(page = 0) {
    const requestId = ++this.parentRequest;
    this.relatedError.set('');
    this.service.list(page, 100, { name: this.parentSearch }).pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: r => { if (requestId !== this.parentRequest) return; this.parents.set(r.data.content); this.parentPage = r.data.number; this.parentPages = r.data.totalPages; },
      error: e => { if (requestId !== this.parentRequest) return; this.parents.set([]); this.relatedError.set(this.message(e)); }
    });
  }
  loadEmployees(page = 0) {
    if (!this.permissions.hasPermission('employees.list')) return;
    const requestId = ++this.employeeRequest;
    // Reuse the existing operation for the first page; subsequent/search pages use its verified DTO.
    const request = page === 0 && !this.employeeSearch ? this.contracts.employees() : this.http.get<ApiResponse<PageResponse<EmployeeOption>>>(`${environment.apiUrl}/api/employees`, {
      params: new HttpParams().set('page', page).set('size', 100).set('sort', 'lastName,asc').set('name', this.employeeSearch.trim())
    });
    request.pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: r => { if (requestId !== this.employeeRequest) return; if (!r.success) { this.relatedError.set(r.message); return; } this.employees.set(r.data.content.filter(e => e.employmentStatus === 'ACTIVE')); this.employeePage = r.data.number; this.employeePages = r.data.totalPages; },
      error: e => { if (requestId !== this.employeeRequest) return; this.employees.set([]); this.relatedError.set(this.message(e)); }
    });
  }
  validParent(d: Department) {
    if (d.status !== 'ACTIVE' || d.id === this.selected?.id) return false;
    const visited = new Set<string>(); let cursor: Department | undefined = d;
    while (cursor) { if (cursor.id === this.selected?.id || visited.has(cursor.id)) return false; visited.add(cursor.id); cursor = this.parents().find(p => p.id === cursor?.parentDepartmentId); }
    return true;
  }
  parentLabel(id: string | null) { if (!id) return 'Sin superior'; const d = [...this.items(), ...this.parents()].find(p => p.id === id); return d ? `${d.code} · ${d.name}` : id; }
  employeeLabel(id: string | null) { const e = this.employees().find(p => p.id === id); return e ? `${e.firstName} ${e.lastName}` : id || 'Sin responsable'; }
  sensitiveChange() { return !!this.selected && (this.selected.parentDepartmentId !== (this.form.parentDepartmentId || null) || this.selected.managerEmployeeId !== (this.form.managerEmployeeId || null)); }
  save() {
    if (this.saving() || this.relatedLoading() || !this.permissions.hasPermission(`departments.${this.mode === 'create' ? 'create' : 'update'}`)) return;
    const f = this.form;
    if (!f.code.trim() || f.code.length > 50 || !f.name.trim() || f.name.length > 150 || (f.costCenterCode?.length || 0) > 50) { this.panelError.set('Complete código (máximo 50) y nombre (máximo 150). Centro de costo: máximo 50.'); return; }
    if ([f.parentDepartmentId, f.managerEmployeeId].some(id => id && !this.uuidPattern.test(id)) || (f.parentDepartmentId && f.parentDepartmentId === this.selected?.id)) { this.panelError.set('Las referencias deben ser UUID válidos; el departamento no puede ser su propio padre.'); return; }
    if (this.sensitiveChange() && !f.reason?.trim()) { this.panelError.set('Indique el motivo del cambio de superior o responsable.'); return; }
    const request: DepartmentRequest = { code: f.code.trim(), name: f.name.trim(), description: f.description?.trim() || null, costCenterCode: f.costCenterCode?.trim() || null, parentDepartmentId: f.parentDepartmentId || null, managerEmployeeId: f.managerEmployeeId || null, reason: f.reason?.trim() || null };
    this.mutate(this.mode === 'create' ? this.service.create(request) : this.service.update(this.selected!.id, request), () => { this.mode = null; this.load(0, true); });
  }
  transition(item: Department) {
    const activate = item.status !== 'ACTIVE';
    if (this.saving() || !this.permissions.hasPermission(`departments.${activate ? 'activate' : 'deactivate'}`)) return;
    const reason = window.prompt(activate ? 'Motivo de activación (opcional):' : 'Motivo obligatorio de desactivación:');
    if (reason === null || (!activate && !reason.trim())) return;
    if (!window.confirm(`¿${activate ? 'Activar' : 'Desactivar'} ${item.name}?`)) return;
    this.mutate(this.service.changeStatus(item.id, activate, reason.trim() || null), () => this.load(0, true));
  }
  viewHistory(item: Department, page = 0) {
    if (this.historyLoading() || this.saving() || !this.permissions.hasPermission('departments.history.read')) return;
    this.selected = item; this.mode = 'history'; this.panelError.set(''); this.historyItems.set([]); this.historyLoading.set(true);
    this.service.history(item.id, page).pipe(takeUntilDestroyed(this.destroy), finalize(() => this.historyLoading.set(false))).subscribe({
      next: r => { this.historyItems.set(r.data.content); this.historyPage = r.data.number; this.historyPages = r.data.totalPages; }, error: e => this.panelError.set(this.message(e))
    });
  }
  close() { if (!this.saving() && !this.relatedLoading() && !this.historyLoading()) this.mode = null; }
  private mutate<T>(operation: Observable<ApiResponse<T>>, done: () => void) {
    this.saving.set(true); this.panelError.set('');
    operation.pipe(takeUntilDestroyed(this.destroy), finalize(() => this.saving.set(false))).subscribe({ next: () => { this.toast.success('Departamento actualizado correctamente.'); done(); }, error: e => { this.panelError.set(this.message(e)); this.toast.error(this.message(e)); } });
  }
  private emptyForm(): DepartmentRequest { return { code: '', name: '', description: null, parentDepartmentId: null, managerEmployeeId: null, costCenterCode: null, reason: null }; }
  private fromDepartment(d: Department): DepartmentRequest { return { code: d.code, name: d.name, description: d.description, parentDepartmentId: d.parentDepartmentId, managerEmployeeId: d.managerEmployeeId, costCenterCode: d.costCenterCode, reason: null }; }
  private message(e: unknown): string { return e instanceof HttpErrorResponse ? (e.status === 0 ? 'No se pudo conectar con el servidor.' : [e.error?.message || (e.status === 403 ? 'No tiene permiso para esta operación.' : 'No se pudo completar la operación.'), ...(Array.isArray(e.error?.errors) ? e.error.errors : [])].join(' · ')) : e instanceof Error ? e.message : 'No se pudo completar la operación.'; }
}
