import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { finalize, Observable, Subscription } from 'rxjs';
import { HasPermissionPipe, ApiResponse } from '../../shared/api';
import { PermissionService } from '../../shared/lib/auth/permission.service';
import { ToastService } from '../../shared/ui/toast/toast.service';
import { Department, DepartmentFilters, DepartmentHistory, DepartmentRequest, DepartmentsService } from '../../entities/departments';
import { EmployeeSummary } from '../../entities/employees/model/employees.model';
import { EmployeesService } from '../../entities/employees/api/employees.service';
import { UserService } from '../../entities/user/api/user.service';

@Component({ selector: 'app-departments-page', standalone: true, imports: [CommonModule, FormsModule, HasPermissionPipe, LucideAngularModule], templateUrl: './departments-page.component.html', styles: [`
  :host [role=dialog] form input,:host [role=dialog] form select,:host [role=dialog] form textarea{font-size:.875rem!important;font-weight:400!important;background:var(--muted)!important;border-color:var(--input)!important;color:var(--foreground)!important;border-radius:1rem!important}:host [role=dialog] form label>span{font-size:.875rem!important;font-weight:600!important;color:var(--muted-foreground)!important}:host [role=dialog] form input::placeholder,:host [role=dialog] form textarea::placeholder{color:var(--muted-foreground);font-weight:400}
`] })
export class DepartmentsPageComponent implements OnInit {
  private readonly service = inject(DepartmentsService);
  private readonly employeeService = inject(EmployeesService);
  private readonly users = inject(UserService);
  private readonly destroy = inject(DestroyRef);
  private readonly toast = inject(ToastService);
  readonly permissions = inject(PermissionService);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly relatedLoading = signal(false);
  readonly parentLoading = signal(false);
  readonly parentOpen = signal(false);
  readonly items = signal<Department[]>([]);
  readonly parents = signal<Department[]>([]);
  readonly employees = signal<EmployeeSummary[]>([]);
  readonly employeeLoading = signal(false);
  readonly suggestionsOpen = signal(false);
  readonly userNames = signal<Record<string, string>>({});
  readonly parentNames = signal<Record<string, string>>({});
  readonly employeeNames = signal<Record<string, string>>({});
  private readonly userNameRequests = new Set<string>();
  private readonly parentNameRequests = new Set<string>();
  private readonly employeeNameRequests = new Set<string>();
  private employeeTimer?: ReturnType<typeof setTimeout>;
  private parentTimer?: ReturnType<typeof setTimeout>;
  activeParent = 0;
  activeSuggestion = 0;
  expandedHistoryId: string | null = null;
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
  statusTarget: Department | null = null;
  statusReason = '';
  readonly uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
  ngOnInit() { this.destroy.onDestroy(() => { clearTimeout(this.employeeTimer); clearTimeout(this.parentTimer); }); this.load(0); }
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
    this.selected = null; this.form = this.emptyForm(); this.mode = 'create'; this.panelError.set(''); this.relatedError.set(''); this.employeeSearch = ''; this.employees.set([]); this.parentSearch = ''; this.parents.set([]); this.loadParents(0);
  }
  open(item: Department, mode: 'edit' | 'detail') {
    if (this.saving() || !this.permissions.hasPermission(`departments.${mode === 'edit' ? 'update' : 'detail'}`)) return;
    this.selected = item; this.form = this.fromDepartment(item); this.mode = mode; this.panelError.set(''); this.relatedError.set('');
    if (mode === 'edit') { this.parentSearch = ''; this.parents.set([]); this.loadParents(0); this.resolveParent(item.parentDepartmentId); this.prepareEmployee(item.managerEmployeeId); }
    if (mode === 'detail') this.resolveUserIds([item.createdBy, item.updatedBy]);
    if (this.permissions.hasPermission('departments.detail')) {
      this.relatedLoading.set(true);
      this.service.get(item.id).pipe(takeUntilDestroyed(this.destroy), finalize(() => this.relatedLoading.set(false))).subscribe({
        next: r => { this.selected = r.data; this.form = this.fromDepartment(r.data); this.resolveParent(r.data.parentDepartmentId); this.prepareEmployee(r.data.managerEmployeeId); if (mode === 'detail') this.resolveUserIds([r.data.createdBy, r.data.updatedBy]); }, error: e => this.panelError.set(this.message(e))
      });
    }
  }
  loadParents(page = 0) {
    const requestId = ++this.parentRequest;
    this.relatedError.set('');
    this.parentLoading.set(true);
    this.service.list(page, 100, { name: this.parentSearch, status: 'ACTIVE' }).pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: r => { if (requestId !== this.parentRequest) return; this.parents.set(page ? [...this.parents(), ...r.data.content.filter(p => !this.parents().some(existing => existing.id === p.id))] : r.data.content); this.parentPage = r.data.number; this.parentPages = r.data.totalPages; this.parentLoading.set(false); },
      error: e => { if (requestId !== this.parentRequest) return; if (!page) this.parents.set([]); this.parentLoading.set(false); this.relatedError.set(this.message(e)); }
    });
  }
  parentInput() {
    clearTimeout(this.parentTimer); ++this.parentRequest;
    this.activeParent = 0; this.parentOpen.set(true); this.parentLoading.set(false); this.parents.set([]); this.parentPage = 0; this.parentPages = 0;
    this.parentTimer = setTimeout(() => this.loadParents(0), 250);
  }
  toggleParent() { this.parentOpen.set(!this.parentOpen()); if (this.parentOpen() && !this.parents().length && !this.parentLoading()) this.loadParents(0); }
  availableParents() { return this.parents().filter(p => this.validParent(p)); }
  chooseParent(parent: Department | null) { this.form.parentDepartmentId = parent?.id || null; this.parentOpen.set(false); this.parentSearch = ''; }
  parentKeydown(event: KeyboardEvent) {
    if (event.key === 'Escape') { this.parentOpen.set(false); return; }
    if (!this.parentOpen()) { if (event.key === 'ArrowDown' || event.key === 'Enter') { event.preventDefault(); this.toggleParent(); } return; }
    const options = this.availableParents();
    if (event.key === 'ArrowDown') { event.preventDefault(); this.activeParent = Math.min(this.activeParent + 1, options.length); }
    if (event.key === 'ArrowUp') { event.preventDefault(); this.activeParent = Math.max(this.activeParent - 1, 0); }
    if (event.key === 'Enter') { event.preventDefault(); if (this.activeParent === 0) this.chooseParent(null); else if (options[this.activeParent - 1]) this.chooseParent(options[this.activeParent - 1]); }
  }
  prepareEmployee(id: string | null) {
    this.employeeSearch = id ? this.employeeLabel(id) : '';
    this.suggestionsOpen.set(false);
    if (id && this.permissions.hasPermission('employees.detail') && !this.employeeNames()[id] && !this.employeeNameRequests.has(id)) {
      this.employeeNameRequests.add(id);
      this.employeeService.get(id).pipe(takeUntilDestroyed(this.destroy)).subscribe({
        next: r => { this.employeeNames.update(names => ({ ...names, [id]: `${r.data.firstName} ${r.data.lastName}` })); if (this.form.managerEmployeeId === id) this.employeeSearch = this.employeeLabel(id); },
        error: () => { /* Keep the identifier if the name is unavailable. */ }
      });
    }
  }
  private resolveParent(id: string | null) {
    if (!id || !this.uuidPattern.test(id) || this.parentNames()[id] || this.parents().some(p => p.id === id) || this.parentNameRequests.has(id) || !this.permissions.hasPermission('departments.detail')) return;
    this.parentNameRequests.add(id);
    this.service.get(id).pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: r => this.parentNames.update(names => ({ ...names, [id]: r.data.name })),
      error: () => { /* Keep the identifier when the name is unavailable. */ }
    });
  }
  employeeInput() {
    clearTimeout(this.employeeTimer);
    ++this.employeeRequest;
    this.form.managerEmployeeId = null;
    this.relatedError.set('');
    this.activeSuggestion = 0;
    this.suggestionsOpen.set(true);
    this.employees.set([]);
    this.employeePage = 0; this.employeePages = 0;
    if (!this.employeeSearch.trim() || !this.permissions.hasPermission('employees.list')) { this.employeeLoading.set(false); return; }
    this.employeeLoading.set(true);
    this.employeeTimer = setTimeout(() => this.loadEmployees(), 300);
  }
  loadEmployees(page = 0) {
    const requestId = this.employeeRequest;
    this.employeeLoading.set(true);
    this.employeeService.list(page, 25, { name: this.employeeSearch, status: 'ACTIVE' }).pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: r => { if (requestId !== this.employeeRequest) return; const active = r.data.content.filter(e => e.employmentStatus === 'ACTIVE'); this.employees.set(page ? [...this.employees(), ...active.filter(e => !this.employees().some(existing => existing.id === e.id))] : active); this.employeePage = r.data.number; this.employeePages = r.data.totalPages; this.employeeLoading.set(false); },
      error: e => { if (requestId !== this.employeeRequest) return; this.employees.set([]); this.employeeLoading.set(false); this.relatedError.set(this.message(e)); }
    });
  }
  chooseEmployee(employee: EmployeeSummary) {
    clearTimeout(this.employeeTimer); ++this.employeeRequest;
    this.form.managerEmployeeId = employee.id;
    this.employeeSearch = `${employee.firstName} ${employee.lastName}`;
    this.employeeNames.update(names => ({ ...names, [employee.id]: this.employeeSearch }));
    this.suggestionsOpen.set(false);
    this.employeeLoading.set(false);
  }
  employeeKeydown(event: KeyboardEvent) {
    if (!this.suggestionsOpen() || !this.employees().length) return;
    if (event.key === 'ArrowDown') { event.preventDefault(); this.activeSuggestion = (this.activeSuggestion + 1) % this.employees().length; }
    if (event.key === 'ArrowUp') { event.preventDefault(); this.activeSuggestion = (this.activeSuggestion - 1 + this.employees().length) % this.employees().length; }
    if (event.key === 'Enter') { event.preventDefault(); this.chooseEmployee(this.employees()[this.activeSuggestion]); }
    if (event.key === 'Escape') this.suggestionsOpen.set(false);
  }
  clearEmployee() { clearTimeout(this.employeeTimer); ++this.employeeRequest; this.form.managerEmployeeId = null; this.employeeSearch = ''; this.employees.set([]); this.suggestionsOpen.set(false); this.employeeLoading.set(false); }
  validParent(d: Department) {
    if (d.status !== 'ACTIVE' || d.id === this.selected?.id || d.parentDepartmentId === this.selected?.id) return false;
    const visited = new Set<string>(); let cursor: Department | undefined = d;
    while (cursor) { if (cursor.id === this.selected?.id || visited.has(cursor.id)) return false; visited.add(cursor.id); cursor = this.parents().find(p => p.id === cursor?.parentDepartmentId); }
    return true;
  }
  parentLabel(id: string | null) { if (!id) return 'Sin superior'; const d = [...this.items(), ...this.parents()].find(p => p.id === id); return d?.name || this.parentNames()[id] || id; }
  employeeLabel(id: string | null) { if (!id) return 'Sin especificar'; const e = this.employees().find(p => p.id === id); return e ? `${e.firstName} ${e.lastName}` : this.employeeNames()[id] || id; }
  userLabel(id: string) { return this.userNames()[id] || id; }
  detailUserLabel(id: string) { return this.userNames()[id] || 'Usuario no disponible'; }
  historyAction(action: DepartmentHistory['action']) { return ({ CREATED: 'Creado', UPDATED: 'Actualizado', ACTIVATED: 'Activado', DEACTIVATED: 'Desactivado', REORGANIZED: 'Reorganizado', MANAGER_CHANGED: 'Responsable cambiado' } as Record<string, string>)[action] || action; }
  historyRows(item: DepartmentHistory) {
    const before = item.previousData || {};
    const after = item.newData || {};
    return [...new Set([...Object.keys(before), ...Object.keys(after)])].map(key => ({ key, label: this.historyField(key), previous: this.historyValue(key, before[key]), next: this.historyValue(key, after[key]) }));
  }
  historyField(key: string) { return ({ code: 'Código', name: 'Nombre', description: 'Descripción', parentDepartmentId: 'Departamento superior', managerEmployeeId: 'Responsable', costCenterCode: 'Centro de costos', status: 'Estado' } as Record<string, string>)[key] || key; }
  historyValue(key: string, value: unknown): string {
    if (value === null || value === undefined || value === '') return 'Sin especificar';
    if (key === 'status') return value === 'ACTIVE' ? 'Activo' : value === 'INACTIVE' ? 'Inactivo' : String(value);
    if (key === 'parentDepartmentId' && typeof value === 'string') return this.parentLabel(value);
    if (key === 'managerEmployeeId' && typeof value === 'string') return this.employeeLabel(value);
    return String(value);
  }
  private resolveHistoryReferences(items: DepartmentHistory[]) {
    for (const item of items) for (const data of [item.previousData, item.newData]) {
      if (!data) continue;
      if (typeof data['parentDepartmentId'] === 'string') this.resolveParent(data['parentDepartmentId']);
      if (typeof data['managerEmployeeId'] === 'string') this.resolveEmployeeName(data['managerEmployeeId']);
    }
  }
  private resolveEmployeeName(id: string) {
    if (!this.uuidPattern.test(id) || this.employeeNames()[id] || this.employeeNameRequests.has(id) || !this.permissions.hasPermission('employees.detail')) return;
    this.employeeNameRequests.add(id);
    this.employeeService.get(id).pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: r => this.employeeNames.update(names => ({ ...names, [id]: `${r.data.firstName} ${r.data.lastName}` })),
      error: () => { /* Keep the identifier when the name is unavailable. */ }
    });
  }
  private resolveUsers(items: DepartmentHistory[]) {
    this.resolveUserIds(items.map(item => item.performedBy));
  }
  private resolveUserIds(ids: string[]) {
    if (!this.permissions.hasPermission('users.list')) return;
    for (const id of new Set(ids.filter(Boolean))) {
      if (!this.uuidPattern.test(id) || this.userNameRequests.has(id)) continue;
      this.userNameRequests.add(id);
      this.users.getUserById(id).pipe(takeUntilDestroyed(this.destroy)).subscribe({
        next: r => { const name = [r.data.firstName, r.data.lastName].filter(Boolean).join(' ').trim(); if (name) this.userNames.update(names => ({ ...names, [id]: name })); },
        error: () => { /* The history keeps the original identifier. */ }
      });
    }
  }
  sensitiveChange() { return !!this.selected && (this.selected.parentDepartmentId !== (this.form.parentDepartmentId || null) || this.selected.managerEmployeeId !== (this.form.managerEmployeeId || null)); }
  save() {
    if (this.saving() || this.relatedLoading() || !this.permissions.hasPermission(`departments.${this.mode === 'create' ? 'create' : 'update'}`)) return;
    const f = this.form;
    if (this.employeeSearch.trim() && !f.managerEmployeeId) { this.panelError.set('Seleccione un empleado de las sugerencias o borre la búsqueda para dejar el responsable sin especificar.'); return; }
    if (!f.name.trim() || f.name.length > 150 || (f.costCenterCode?.length || 0) > 50) { this.panelError.set('Complete el nombre (máximo 150). Centro de costo: máximo 50.'); return; }
    if ([f.parentDepartmentId, f.managerEmployeeId].some(id => id && !this.uuidPattern.test(id)) || (f.parentDepartmentId && f.parentDepartmentId === this.selected?.id)) { this.panelError.set('Las referencias deben ser UUID válidos; el departamento no puede ser su propio padre.'); return; }
    if (this.sensitiveChange() && !f.reason?.trim()) { this.panelError.set('Indique el motivo del cambio de superior o responsable.'); return; }
    const request: DepartmentRequest = { name: f.name.trim(), description: f.description?.trim() || null, costCenterCode: f.costCenterCode?.trim() || null, parentDepartmentId: f.parentDepartmentId || null, managerEmployeeId: f.managerEmployeeId || null, reason: f.reason?.trim() || null };
    this.mutate(this.mode === 'create' ? this.service.create(request) : this.service.update(this.selected!.id, request), () => { this.mode = null; this.load(0, true); });
  }
  transition(item: Department) {
    const activate = item.status !== 'ACTIVE';
    if (this.saving() || !this.permissions.hasPermission(`departments.${activate ? 'activate' : 'deactivate'}`)) return;
    this.statusTarget = item; this.statusReason = ''; this.panelError.set('');
  }
  confirmTransition() {
    const item = this.statusTarget;
    if (!item || this.saving()) return;
    const activate = item.status !== 'ACTIVE';
    if (!activate && !this.statusReason.trim()) { this.panelError.set('Indique el motivo de desactivación.'); return; }
    this.mutate(this.service.changeStatus(item.id, activate, this.statusReason.trim() || null), () => { this.statusTarget = null; this.load(0, true); });
  }
  viewHistory(item: Department, page = 0) {
    if (this.historyLoading() || this.saving() || !this.permissions.hasPermission('departments.history.read')) return;
    this.selected = item; this.mode = 'history'; this.panelError.set(''); this.historyItems.set([]); this.expandedHistoryId = null; this.historyLoading.set(true);
    this.service.history(item.id, page).pipe(takeUntilDestroyed(this.destroy), finalize(() => this.historyLoading.set(false))).subscribe({
      next: r => { this.historyItems.set(r.data.content); this.historyPage = r.data.number; this.historyPages = r.data.totalPages; this.resolveUsers(r.data.content); this.resolveHistoryReferences(r.data.content); }, error: e => this.panelError.set(this.message(e))
    });
  }
  close() { if (!this.saving() && !this.relatedLoading() && !this.historyLoading()) { clearTimeout(this.employeeTimer); clearTimeout(this.parentTimer); ++this.employeeRequest; ++this.parentRequest; this.mode = null; this.suggestionsOpen.set(false); this.parentOpen.set(false); } }
  private mutate<T>(operation: Observable<ApiResponse<T>>, done: () => void) {
    this.saving.set(true); this.panelError.set('');
    operation.pipe(takeUntilDestroyed(this.destroy), finalize(() => this.saving.set(false))).subscribe({ next: () => { this.toast.success('Departamento actualizado correctamente.'); done(); }, error: e => { this.panelError.set(this.message(e)); this.toast.error(this.message(e)); } });
  }
  private emptyForm(): DepartmentRequest { return { name: '', description: null, parentDepartmentId: null, managerEmployeeId: null, costCenterCode: null, reason: null }; }
  private fromDepartment(d: Department): DepartmentRequest { return { name: d.name, description: d.description, parentDepartmentId: d.parentDepartmentId, managerEmployeeId: d.managerEmployeeId, costCenterCode: d.costCenterCode, reason: null }; }
  private message(e: unknown): string { return e instanceof HttpErrorResponse ? (e.status === 0 ? 'No se pudo conectar con el servidor.' : [e.error?.message || (e.status === 403 ? 'No tiene permiso para esta operación.' : 'No se pudo completar la operación.'), ...(Array.isArray(e.error?.errors) ? e.error.errors : [])].join(' · ')) : e instanceof Error ? e.message : 'No se pudo completar la operación.'; }
}
