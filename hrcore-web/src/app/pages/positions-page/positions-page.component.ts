import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { finalize, Observable, Subscription } from 'rxjs';
import { ApiResponse, HasPermissionPipe } from '../../shared/api';
import { PermissionService } from '../../shared/lib/auth/permission.service';
import { ToastService } from '../../shared/ui/toast/toast.service';
import { ORGANIZATIONAL_LEVELS, REQUIREMENT_TYPES, Position, PositionFilters, PositionRequest, PositionRequirement, PositionRequirementRequest, PositionsService } from '../../entities/positions';
import { compareSalaries, normalizeSalary } from '../../entities/positions/model/position-salary';

@Component({ selector: 'app-positions-page', standalone: true, imports: [CommonModule, FormsModule, HasPermissionPipe, LucideAngularModule], templateUrl: './positions-page.component.html' })
export class PositionsPageComponent implements OnInit {
  private readonly service = inject(PositionsService);
  private readonly destroy = inject(DestroyRef);
  private readonly toast = inject(ToastService);
  readonly permissions = inject(PermissionService);
  readonly loading = signal(false); readonly saving = signal(false); readonly panelLoading = signal(false);
  readonly items = signal<Position[]>([]); readonly requirements = signal<PositionRequirement[]>([]);
  readonly error = signal(''); readonly panelError = signal('');
  readonly levels = ORGANIZATIONAL_LEVELS; readonly types = REQUIREMENT_TYPES;
  page = 0; totalPages = 0; total = 0; size = 25; sort = 'name,asc';
  requirementPage = 0; requirementPages = 0;
  filters: PositionFilters = { name: '', code: '', status: '', organizationalLevel: '' };
  selected: Position | null = null;
  mode: 'create' | 'edit' | 'detail' | 'requirements' | null = null;
  form: PositionRequest = this.emptyForm();
  requirementForm: PositionRequirementRequest = this.emptyRequirement();
  editingRequirement: string | null = null;
  showRequirementForm = false;
  private listRequest?: Subscription;
  ngOnInit() { this.load(0); }
  label(value: string | null) { return ({ EXECUTIVE: 'Ejecutivo', MANAGEMENT: 'Gerencia', SUPERVISORY: 'Supervisión', PROFESSIONAL: 'Profesional', TECHNICAL: 'Técnico', OPERATIONAL: 'Operativo', EDUCATION: 'Educación', EXPERIENCE: 'Experiencia', SKILL: 'Habilidad', CERTIFICATION: 'Certificación', OTHER: 'Otro' } as Record<string, string>)[value || ''] || value || '—'; }
  load(page = this.page, refresh = false) {
    if (this.loading() && !refresh) return;
    // A successful mutation replaces an older query instead of losing its refresh.
    this.listRequest?.unsubscribe();
    this.loading.set(true); this.error.set('');
    this.listRequest = this.service.list(page, this.size, this.filters, this.sort).pipe(takeUntilDestroyed(this.destroy), finalize(() => this.loading.set(false))).subscribe({
      next: r => { this.items.set(r.data.content); this.page = r.data.number; this.totalPages = r.data.totalPages; this.total = r.data.totalElements; }, error: e => { this.items.set([]); this.error.set(this.message(e)); this.toast.error(this.error()); }
    });
  }
  openCreate() { if (this.saving() || !this.permissions.hasPermission('positions.create')) return; this.selected = null; this.form = this.emptyForm(); this.mode = 'create'; this.panelError.set(''); }
  open(item: Position, mode: 'detail' | 'edit') {
    if (this.saving() || !this.permissions.hasPermission(`positions.${mode === 'edit' ? 'update' : 'detail'}`)) return;
    this.selected = item; this.form = this.fromPosition(item); this.mode = mode; this.panelError.set('');
    if (this.permissions.hasPermission('positions.detail')) {
      this.panelLoading.set(true);
      this.service.get(item.id).pipe(takeUntilDestroyed(this.destroy), finalize(() => this.panelLoading.set(false))).subscribe({ next: r => { this.selected = r.data; this.form = this.fromPosition(r.data); }, error: e => this.panelError.set(this.message(e)) });
    }
  }
  save() {
    if (this.saving() || this.panelLoading() || !this.permissions.hasPermission(`positions.${this.mode === 'create' ? 'create' : 'update'}`)) return;
    const f = this.form;
    if (!f.code.trim() || f.code.length > 50 || !f.name.trim() || f.name.length > 150) { this.panelError.set('Complete código (máximo 50) y nombre (máximo 150).'); return; }
    let salaryMin: string | null, salaryMax: string | null;
    try { salaryMin = normalizeSalary(f.salaryMin); salaryMax = normalizeSalary(f.salaryMax); }
    catch (e) { this.panelError.set(this.message(e)); return; }
    if (salaryMin !== null && salaryMax !== null && compareSalaries(salaryMin, salaryMax) > 0) { this.panelError.set('El salario mínimo no puede superar al máximo.'); return; }
    if (f.currency?.trim() && !/^[A-Z]{3}$/.test(f.currency.trim())) { this.panelError.set('La moneda debe tener tres letras mayúsculas.'); return; }
    const request: PositionRequest = { code: f.code.trim(), name: f.name.trim(), description: f.description?.trim() || null, responsibilities: f.responsibilities?.trim() || null, organizationalLevel: f.organizationalLevel, salaryMin, salaryMax, currency: f.currency?.trim() || null };
    this.mutate(this.mode === 'create' ? this.service.create(request) : this.service.update(this.selected!.id, request), () => { this.mode = null; this.load(0, true); });
  }
  transition(item: Position) {
    const activate = item.status !== 'ACTIVE';
    if (this.saving() || !this.permissions.hasPermission(`positions.${activate ? 'activate' : 'deactivate'}`) || !window.confirm(`¿${activate ? 'Activar' : 'Desactivar'} ${item.name}?`)) return;
    this.mutate(this.service.changeStatus(item.id, activate), () => this.load(0, true));
  }
  openRequirements(item: Position) {
    if (this.saving() || !this.permissions.hasAnyPermission('positions.requirements.list', 'positions.requirements.create', 'positions.requirements.update', 'positions.requirements.delete')) return;
    this.selected = item; this.mode = 'requirements'; this.showRequirementForm = false; this.requirements.set([]); this.panelError.set(''); this.requirementPage = 0; this.requirementPages = 0;
    if (this.permissions.hasPermission('positions.requirements.list')) this.loadRequirements(0);
  }
  loadRequirements(page = this.requirementPage) {
    if (this.panelLoading() || !this.selected || !this.permissions.hasPermission('positions.requirements.list')) return;
    this.panelLoading.set(true); this.panelError.set('');
    this.service.requirements(this.selected.id, page).pipe(takeUntilDestroyed(this.destroy), finalize(() => this.panelLoading.set(false))).subscribe({ next: r => { this.requirements.set(r.data.content); this.requirementPage = r.data.number; this.requirementPages = r.data.totalPages; }, error: e => { this.requirements.set([]); this.panelError.set(this.message(e)); } });
  }
  editRequirement(item?: PositionRequirement) {
    if (this.saving() || !this.permissions.hasPermission(`positions.requirements.${item ? 'update' : 'create'}`)) return;
    this.editingRequirement = item?.id || null; this.showRequirementForm = true;
    this.requirementForm = item ? { requirementType: item.requirementType, description: item.description, mandatory: item.mandatory, minimumYearsExperience: item.minimumYearsExperience } : this.emptyRequirement();
  }
  saveRequirement() {
    if (this.saving() || !this.selected || !this.permissions.hasPermission(`positions.requirements.${this.editingRequirement ? 'update' : 'create'}`)) return;
    const f = this.requirementForm;
    if (!f.description.trim() || !this.types.includes(f.requirementType) || typeof f.mandatory !== 'boolean' || (f.minimumYearsExperience !== null && (!Number.isInteger(f.minimumYearsExperience) || f.minimumYearsExperience < 0 || f.minimumYearsExperience > 2147483647))) { this.panelError.set('Complete la descripción y el tipo. La experiencia debe ser un entero no negativo (máximo 2147483647).'); return; }
    const request = { requirementType: f.requirementType, description: f.description.trim(), mandatory: f.mandatory, minimumYearsExperience: f.minimumYearsExperience };
    this.mutate(this.editingRequirement ? this.service.updateRequirement(this.selected.id, this.editingRequirement, request) : this.service.createRequirement(this.selected.id, request), () => { this.showRequirementForm = false; this.loadRequirements(0); });
  }
  deleteRequirement(item: PositionRequirement) {
    if (this.saving() || !this.selected || !this.permissions.hasPermission('positions.requirements.delete') || !window.confirm('¿Eliminar este requisito?')) return;
    this.mutate(this.service.deleteRequirement(this.selected.id, item.id), () => { if (this.editingRequirement === item.id) this.showRequirementForm = false; this.loadRequirements(0); });
  }
  close() { if (!this.saving() && !this.panelLoading()) this.mode = null; }
  private mutate<T>(operation: Observable<ApiResponse<T>>, done: () => void) {
    this.saving.set(true); this.panelError.set('');
    operation.pipe(takeUntilDestroyed(this.destroy), finalize(() => this.saving.set(false))).subscribe({ next: () => { this.toast.success('Operación realizada correctamente.'); done(); }, error: e => { this.panelError.set(this.message(e)); this.toast.error(this.message(e)); } });
  }
  private emptyForm(): PositionRequest { return { code: '', name: '', description: null, responsibilities: null, organizationalLevel: 'OPERATIONAL', salaryMin: null, salaryMax: null, currency: null }; }
  private emptyRequirement(): PositionRequirementRequest { return { requirementType: 'EDUCATION', description: '', mandatory: true, minimumYearsExperience: null }; }
  private fromPosition(p: Position): PositionRequest { return { code: p.code, name: p.name, description: p.description, responsibilities: p.responsibilities, organizationalLevel: p.organizationalLevel, salaryMin: p.salaryMin, salaryMax: p.salaryMax, currency: p.currency }; }
  private message(e: unknown): string { return e instanceof HttpErrorResponse ? (e.status === 0 ? 'No se pudo conectar con el servidor.' : [e.error?.message || (e.status === 403 ? 'No tiene permiso para esta operación.' : 'No se pudo completar la operación.'), ...(Array.isArray(e.error?.errors) ? e.error.errors : [])].join(' · ')) : e instanceof Error ? e.message : 'No se pudo completar la operación.'; }
}
