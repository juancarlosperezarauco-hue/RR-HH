import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { EmployeesService } from '../../entities/employees';
import {
  IncidentAction,
  IncidentHistory,
  IncidentsService,
  IncidentResponse,
  IncidentSeverity,
  IncidentStatus,
  IncidentType
} from '../../entities/incidents';
import { ToastService } from '../../shared/ui/toast/toast.service';
import { extractHttpErrorMessage } from '../../shared/api/http-error-handler';

@Component({
  selector: 'app-incidents-page',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  template: `
    <div class="animate-fade-in space-y-6 p-4 sm:p-6 lg:p-8">
      <!-- Encabezado ejecutivo -->
      <section class="rounded-[28px] border border-border bg-gradient-to-br from-card via-background to-muted p-6 shadow-lg">
        <div class="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <div class="mb-3 inline-flex items-center gap-2 rounded-full border border-border bg-card px-3 py-1 text-xs font-bold uppercase tracking-[0.16em] text-primary">
              <span class="h-2 w-2 rounded-full bg-primary"></span> Gestión laboral
            </div>
            <h1 class="text-3xl font-black tracking-tight text-accent-foreground sm:text-4xl">Incidencias laborales</h1>
            <p class="mt-2 max-w-3xl text-sm leading-6 text-muted-foreground">
              Registra, documenta y da seguimiento formal a eventos disciplinarios, reconocimientos y situaciones laborales.
            </p>
          </div>
          <div class="flex flex-wrap gap-3">
            <button type="button" (click)="load()" class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-border bg-card px-4 py-2 text-sm font-bold text-primary transition-colors hover:bg-accent">
              <lucide-icon name="refresh-ccw" [size]="16"></lucide-icon> Actualizar
            </button>
            <button type="button" (click)="openCreate()" class="inline-flex cursor-pointer items-center gap-2 rounded-full bg-primary px-5 py-2 text-sm font-bold text-primary-foreground shadow-sm transition-all hover:bg-primary/90">
              <lucide-icon name="plus" [size]="17"></lucide-icon> Registrar incidencia
            </button>
          </div>
        </div>
      </section>

      <!-- Filtros -->
      <div class="grid gap-3 rounded-[24px] border border-border bg-card p-5 shadow-sm md:grid-cols-4">
        <label class="space-y-1.5 text-xs font-bold text-muted-foreground">
          <span>Estado</span>
          <select class="h-11 w-full rounded-xl border border-border bg-background px-3.5 text-sm font-normal text-foreground outline-none focus:border-primary" [(ngModel)]="status" (ngModelChange)="load()">
            <option value="">Todos los estados</option>
            <option *ngFor="let x of statuses" [value]="x">{{ statusLabel(x) }}</option>
          </select>
        </label>
        <label class="space-y-1.5 text-xs font-bold text-muted-foreground">
          <span>Severidad</span>
          <select class="h-11 w-full rounded-xl border border-border bg-background px-3.5 text-sm font-normal text-foreground outline-none focus:border-primary" [(ngModel)]="severity" (ngModelChange)="load()">
            <option value="">Todas las severidades</option>
            <option *ngFor="let x of severities" [value]="x">{{ severityLabel(x) }}</option>
          </select>
        </label>
        <label class="space-y-1.5 text-xs font-bold text-muted-foreground">
          <span>Tipo de incidencia</span>
          <select class="h-11 w-full rounded-xl border border-border bg-background px-3.5 text-sm font-normal text-foreground outline-none focus:border-primary" [(ngModel)]="type" (ngModelChange)="load()">
            <option value="">Todos los tipos</option>
            <option *ngFor="let x of types" [value]="x">{{ typeLabel(x) }}</option>
          </select>
        </label>
        <div class="flex items-end">
          <button type="button" class="h-11 w-full cursor-pointer rounded-xl border border-border px-4 text-sm font-bold text-muted-foreground transition-colors hover:bg-muted hover:text-foreground" (click)="clearFilters()">
            Limpiar filtros
          </button>
        </div>
      </div>

      <!-- Tabla y panel de detalle -->
      <div class="grid gap-6 xl:grid-cols-[minmax(0,1fr)_28rem]">
        <div class="overflow-x-auto rounded-[24px] border border-border bg-card shadow-sm">
          <table class="w-full text-sm">
            <thead>
              <tr class="border-b border-border bg-muted/50 text-left text-xs uppercase tracking-wider text-muted-foreground">
                <th class="px-4 py-3.5">Incidencia</th>
                <th class="px-4 py-3.5">Empleado</th>
                <th class="px-4 py-3.5">Clasificación</th>
                <th class="px-4 py-3.5">Estado</th>
                <th class="px-4 py-3.5">Fecha</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-border">
              <tr *ngFor="let x of items()" (click)="select(x)" class="cursor-pointer transition-colors hover:bg-muted/40" [class.bg-primary/5]="selected()?.id === x.id">
                <td class="px-4 py-3.5">
                  <p class="font-black text-primary">{{ x.incidentNumber }}</p>
                  <p class="text-xs text-accent-foreground font-semibold">{{ x.title }}</p>
                </td>
                <td class="px-4 py-3.5 font-semibold text-accent-foreground">{{ employeeName(x.employeeId) }}</td>
                <td class="px-4 py-3.5 text-xs">
                  <span class="font-bold text-foreground">{{ typeLabel(x.incidentType) }}</span>
                  <span class="mx-1 text-muted-foreground">·</span>
                  <span class="font-semibold text-muted-foreground">{{ severityLabel(x.severity) }}</span>
                </td>
                <td class="px-4 py-3.5">
                  <span class="rounded-full bg-muted px-2.5 py-1 text-xs font-bold text-accent-foreground">{{ statusLabel(x.status) }}</span>
                </td>
                <td class="px-4 py-3.5 text-xs text-muted-foreground">{{ x.occurredAt | date:'dd/MM/yyyy' }}</td>
              </tr>
              <tr *ngIf="!loading() && !items().length">
                <td colspan="5" class="p-10 text-center text-sm text-muted-foreground">No se encontraron incidencias con los filtros seleccionados.</td>
              </tr>
            </tbody>
          </table>
          <p *ngIf="loading()" class="p-5 text-center text-sm font-semibold text-muted-foreground">Cargando incidencias…</p>
        </div>

        <aside *ngIf="selected() as x" class="animate-fade-in space-y-5 rounded-[24px] border border-border bg-card p-6 shadow-sm">
          <div class="flex items-start justify-between gap-3 border-b border-border pb-4">
            <div>
              <p class="text-xs font-black uppercase tracking-wider text-primary">{{ x.incidentNumber }}</p>
              <h2 class="mt-0.5 text-lg font-black text-accent-foreground">{{ x.title }}</h2>
            </div>
            <span class="rounded-full bg-muted px-3 py-1 text-xs font-bold text-accent-foreground">{{ statusLabel(x.status) }}</span>
          </div>

          <dl class="grid grid-cols-2 gap-3 rounded-2xl bg-muted/40 p-4 text-xs">
            <div>
              <dt class="font-semibold text-muted-foreground">Empleado</dt>
              <dd class="mt-0.5 font-black text-accent-foreground">{{ employeeName(x.employeeId) }}</dd>
            </div>
            <div>
              <dt class="font-semibold text-muted-foreground">Severidad</dt>
              <dd class="mt-0.5 font-black text-accent-foreground">{{ severityLabel(x.severity) }}</dd>
            </div>
            <div>
              <dt class="font-semibold text-muted-foreground">Tipo</dt>
              <dd class="mt-0.5 font-black text-accent-foreground">{{ typeLabel(x.incidentType) }}</dd>
            </div>
            <div>
              <dt class="font-semibold text-muted-foreground">Fecha del evento</dt>
              <dd class="mt-0.5 font-black text-accent-foreground">{{ x.occurredAt | date:'dd/MM/yyyy HH:mm' }}</dd>
            </div>
          </dl>

          <div class="space-y-1">
            <p class="text-xs font-bold uppercase tracking-wider text-muted-foreground">Descripción de los hechos</p>
            <p class="whitespace-pre-wrap text-sm leading-relaxed text-foreground">{{ x.description }}</p>
          </div>
          <p *ngIf="x.location" class="text-xs font-semibold text-muted-foreground">Lugar: <span class="text-foreground">{{ x.location }}</span></p>

          <div class="flex flex-wrap gap-2 border-t border-border pt-4">
            <button *ngIf="x.status === 'OPEN'" type="button" class="cursor-pointer rounded-full border border-border px-3.5 py-1.5 text-xs font-bold hover:bg-muted" (click)="change('review')">Iniciar revisión</button>
            <button *ngIf="x.status === 'UNDER_REVIEW'" type="button" class="cursor-pointer rounded-full bg-primary px-4 py-1.5 text-xs font-bold text-primary-foreground" (click)="resolve()">Resolver</button>
            <button *ngIf="x.status === 'OPEN' || x.status === 'UNDER_REVIEW'" type="button" class="cursor-pointer rounded-full border border-destructive/30 px-3.5 py-1.5 text-xs font-bold text-destructive hover:bg-destructive/10" (click)="change('cancel')">Cancelar</button>
            <button *ngIf="x.status === 'RESOLVED'" type="button" class="cursor-pointer rounded-full border border-border px-3.5 py-1.5 text-xs font-bold hover:bg-muted" (click)="change('close')">Cerrar expediente</button>
          </div>

          <div class="border-t border-border pt-4">
            <div class="flex items-center justify-between">
              <h3 class="text-xs font-black uppercase tracking-wider text-muted-foreground">Acciones administrativas</h3>
              <button type="button" class="cursor-pointer rounded-full border border-border px-3 py-1 text-xs font-bold text-primary hover:bg-accent" (click)="addAction()">+ Agregar acción</button>
            </div>
            <div *ngFor="let a of actions()" class="mt-2.5 rounded-xl border border-border bg-muted/20 p-3 text-xs">
              <div class="flex items-center justify-between">
                <b class="text-accent-foreground">{{ a.actionType === 'OTHER' ? 'Acción administrativa' : a.actionType }}</b>
                <span class="rounded-full bg-card px-2 py-0.5 text-[10px] font-bold">{{ actionStatusLabel(a.status) }}</span>
              </div>
              <p class="mt-1 text-muted-foreground">{{ a.description }}</p>
              <div class="mt-2 flex gap-2">
                <button *ngIf="a.status === 'PENDING'" type="button" class="cursor-pointer font-bold text-primary underline" (click)="updateAction(a, 'start')">Iniciar</button>
                <button *ngIf="a.status === 'IN_PROGRESS'" type="button" class="cursor-pointer font-bold text-primary underline" (click)="updateAction(a, 'complete')">Completar</button>
              </div>
            </div>
          </div>

          <div class="border-t border-border pt-4">
            <h3 class="text-xs font-black uppercase tracking-wider text-muted-foreground">Historial de seguimiento</h3>
            <div *ngFor="let h of history()" class="mt-2 rounded-xl bg-muted/30 p-2.5 text-xs">
              <div class="flex justify-between">
                <b class="text-accent-foreground">{{ h.action }}</b>
                <span class="text-[11px] text-muted-foreground">{{ h.createdAt | date:'dd/MM/yyyy HH:mm' }}</span>
              </div>
              <p *ngIf="h.comment" class="mt-1 text-muted-foreground">{{ h.comment }}</p>
            </div>
          </div>
        </aside>
      </div>

      <!-- Modal de registro de incidencia -->
      <div *ngIf="showForm()" class="app-modal-overlay fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
        <div class="app-modal-panel flex max-h-[92vh] w-full max-w-2xl flex-col overflow-hidden rounded-[28px] border border-border bg-card shadow-2xl">
          <div class="flex shrink-0 items-center justify-between border-b border-border px-6 py-4">
            <div>
              <h2 class="text-xl font-black text-accent-foreground">Registrar incidencia laboral</h2>
              <p class="text-xs text-muted-foreground">Documenta los hechos, clasificación y empleado involucrado. Los campos con (*) son obligatorios.</p>
            </div>
            <button type="button" aria-label="Cerrar formulario" title="Cerrar" (click)="closeForm()" class="inline-flex h-9 w-9 cursor-pointer items-center justify-center rounded-full border border-border bg-card text-muted-foreground hover:bg-muted hover:text-foreground">
              <lucide-icon name="x" [size]="18"></lucide-icon>
            </button>
          </div>

          <form #incidentForm="ngForm" class="flex min-h-0 flex-1 flex-col overflow-hidden" (ngSubmit)="create(incidentForm)" novalidate>
            <div class="grid flex-1 gap-4 overflow-y-auto p-6 md:grid-cols-2">
              <label class="space-y-1.5 text-xs font-bold text-accent-foreground">
                <span>Empleado *</span>
                <select required [class.border-destructive]="formSubmitted() && !form.employeeId" class="h-11 w-full rounded-xl border border-border bg-background px-3.5 text-sm font-normal text-foreground outline-none focus:border-primary" [(ngModel)]="form.employeeId" name="employeeId">
                  <option value="">Seleccione un empleado</option>
                  <option *ngFor="let e of employees()" [value]="e.id">{{ employeeName(e.id) }}</option>
                </select>
                <small *ngIf="formSubmitted() && !form.employeeId" class="block text-xs font-medium text-destructive">Selecciona al empleado involucrado.</small>
              </label>

              <label class="space-y-1.5 text-xs font-bold text-accent-foreground">
                <span>Fecha y hora del suceso *</span>
                <input required type="datetime-local" [class.border-destructive]="formSubmitted() && !form.occurredAt" class="h-11 w-full rounded-xl border border-border bg-background px-3.5 text-sm font-normal text-foreground outline-none focus:border-primary" [(ngModel)]="form.occurredAt" name="occurredAt" />
                <small *ngIf="formSubmitted() && !form.occurredAt" class="block text-xs font-medium text-destructive">Indica la fecha y hora en que ocurrió.</small>
              </label>

              <label class="space-y-1.5 text-xs font-bold text-accent-foreground">
                <span>Tipo de incidencia *</span>
                <select required class="h-11 w-full rounded-xl border border-border bg-background px-3.5 text-sm font-normal text-foreground outline-none focus:border-primary" [(ngModel)]="form.incidentType" name="incidentType">
                  <option *ngFor="let x of types" [value]="x">{{ typeLabel(x) }}</option>
                </select>
              </label>

              <label class="space-y-1.5 text-xs font-bold text-accent-foreground">
                <span>Nivel de severidad *</span>
                <select required class="h-11 w-full rounded-xl border border-border bg-background px-3.5 text-sm font-normal text-foreground outline-none focus:border-primary" [(ngModel)]="form.severity" name="severity">
                  <option *ngFor="let x of severities" [value]="x">{{ severityLabel(x) }}</option>
                </select>
              </label>

              <label class="space-y-1.5 text-xs font-bold text-accent-foreground md:col-span-2">
                <span>Título descriptivo *</span>
                <input required maxlength="200" [class.border-destructive]="formSubmitted() && !form.title.trim()" class="h-11 w-full rounded-xl border border-border bg-background px-3.5 text-sm font-normal text-foreground outline-none focus:border-primary" [(ngModel)]="form.title" name="title" placeholder="Ej. Incumplimiento de procedimiento de seguridad" />
                <small *ngIf="formSubmitted() && !form.title.trim()" class="block text-xs font-medium text-destructive">Ingresa un título breve para la incidencia.</small>
              </label>

              <label class="space-y-1.5 text-xs font-bold text-accent-foreground md:col-span-2">
                <span>Descripción detallada *</span>
                <textarea required rows="3" [class.border-destructive]="formSubmitted() && !form.description.trim()" class="w-full resize-none rounded-xl border border-border bg-background p-3.5 text-sm font-normal text-foreground outline-none focus:border-primary" [(ngModel)]="form.description" name="description" placeholder="Describe objetivamente los hechos y antecedentes..."></textarea>
                <small *ngIf="formSubmitted() && !form.description.trim()" class="block text-xs font-medium text-destructive">Describe los hechos de la incidencia.</small>
              </label>

              <label class="space-y-1.5 text-xs font-semibold text-muted-foreground md:col-span-2">
                <span>Lugar o área (opcional)</span>
                <input class="h-11 w-full rounded-xl border border-border bg-background px-3.5 text-sm font-normal text-foreground outline-none focus:border-primary" [(ngModel)]="form.location" name="location" placeholder="Ej. Área de producción / Oficina central" />
              </label>
            </div>

            <div class="flex shrink-0 justify-end gap-3 border-t border-border bg-card px-6 py-4">
              <button type="button" class="cursor-pointer rounded-full border border-border px-5 py-2 text-sm font-bold text-muted-foreground hover:bg-muted" (click)="closeForm()">Cancelar</button>
              <button type="submit" [disabled]="saving()" class="cursor-pointer rounded-full bg-primary px-6 py-2 text-sm font-black text-primary-foreground shadow-sm hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50">
                {{ saving() ? 'Guardando…' : 'Guardar incidencia' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  `
})
export class IncidentsPageComponent implements OnInit {
  private readonly service = inject(IncidentsService);
  private readonly employeesService = inject(EmployeesService);
  private readonly toast = inject(ToastService);

  readonly items = signal<IncidentResponse[]>([]);
  readonly employees = signal<any[]>([]);
  readonly selected = signal<IncidentResponse | null>(null);
  readonly actions = signal<IncidentAction[]>([]);
  readonly history = signal<IncidentHistory[]>([]);
  readonly loading = signal(false);
  readonly showForm = signal(false);
  readonly saving = signal(false);
  readonly formSubmitted = signal(false);

  readonly statuses: IncidentStatus[] = ['OPEN', 'UNDER_REVIEW', 'RESOLVED', 'CLOSED', 'CANCELLED'];
  readonly severities: IncidentSeverity[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
  readonly types: IncidentType[] = ['MEMORANDUM', 'WARNING', 'DISCIPLINARY', 'MISCONDUCT', 'PERFORMANCE', 'SAFETY', 'RECOGNITION', 'OTHER'];

  status = '';
  severity = '';
  type = '';
  form = this.emptyForm();

  ngOnInit() {
    this.employeesService.list(0, 100).subscribe((r) => this.employees.set(r.data?.content ?? []));
    this.load();
  }

  load() {
    this.loading.set(true);
    const f: any = {};
    if (this.status) f.status = this.status;
    if (this.severity) f.severity = this.severity;
    if (this.type) f.incidentType = this.type;
    this.service.list(f).subscribe({
      next: (r) => {
        this.items.set(r.data?.content ?? []);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.toast.error(extractHttpErrorMessage(e, 'No se pudieron cargar las incidencias'));
      }
    });
  }

  clearFilters() {
    this.status = '';
    this.severity = '';
    this.type = '';
    this.load();
  }

  employeeName(id: string) {
    const e = this.employees().find((x) => x.id === id);
    return (e?.fullName ?? [e?.firstName, e?.lastName].filter(Boolean).join(' ')) || id;
  }

  statusLabel(status: string): string {
    return ({
      OPEN: 'Abierta',
      UNDER_REVIEW: 'En revisión',
      RESOLVED: 'Resuelta',
      CLOSED: 'Cerrada',
      CANCELLED: 'Cancelada'
    } as Record<string, string>)[status] ?? status;
  }

  severityLabel(severity: string): string {
    return ({
      LOW: 'Baja',
      MEDIUM: 'Media',
      HIGH: 'Alta',
      CRITICAL: 'Crítica'
    } as Record<string, string>)[severity] ?? severity;
  }

  typeLabel(type: string): string {
    return ({
      MEMORANDUM: 'Memorándum',
      WARNING: 'Llamada de atención',
      DISCIPLINARY: 'Disciplinaria',
      MISCONDUCT: 'Falta de conducta',
      PERFORMANCE: 'Desempeño',
      SAFETY: 'Seguridad laboral',
      RECOGNITION: 'Reconocimiento',
      OTHER: 'Otro'
    } as Record<string, string>)[type] ?? type;
  }

  actionStatusLabel(status: string): string {
    return ({
      PENDING: 'Pendiente',
      IN_PROGRESS: 'En curso',
      COMPLETED: 'Completada',
      CANCELLED: 'Cancelada'
    } as Record<string, string>)[status] ?? status;
  }

  select(x: IncidentResponse) {
    this.selected.set(x);
    this.service.actions(x.id).subscribe((r) => this.actions.set(r.data ?? []));
    this.service.history(x.id).subscribe((r) => this.history.set(r.data ?? []));
  }

  openCreate() {
    this.form = this.emptyForm();
    this.formSubmitted.set(false);
    this.showForm.set(true);
  }

  closeForm() {
    if (!this.saving()) {
      this.formSubmitted.set(false);
      this.showForm.set(false);
    }
  }

  create(form?: any) {
    this.formSubmitted.set(true);
    if (!this.form.employeeId || !this.form.title.trim() || !this.form.description.trim() || !this.form.occurredAt) {
      this.toast.warning('Completa empleado, fecha, título y descripción para registrar la incidencia');
      form?.form?.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.service.create({
      ...this.form,
      title: this.form.title.trim(),
      description: this.form.description.trim(),
      location: this.form.location?.trim() || null,
      occurredAt: new Date(this.form.occurredAt).toISOString()
    }).subscribe({
      next: (r) => {
        this.saving.set(false);
        if (!r.success || !r.data) {
          this.toast.error(r.message || 'No se pudo registrar la incidencia');
          return;
        }
        this.formSubmitted.set(false);
        this.showForm.set(false);
        this.load();
        this.select(r.data);
        this.toast.success('Incidencia registrada correctamente');
      },
      error: (e) => {
        this.saving.set(false);
        this.toast.error(extractHttpErrorMessage(e, 'No se pudo registrar la incidencia'));
      }
    });
  }

  change(a: 'review' | 'cancel' | 'close') {
    const x = this.selected();
    if (!x) return;
    const call = a === 'review' ? this.service.startReview(x.id) : a === 'cancel' ? this.service.cancel(x.id) : this.service.close(x.id);
    call.subscribe({
      next: (r) => {
        if (r.data) this.select(r.data);
        this.load();
      },
      error: (e) => this.toast.error(extractHttpErrorMessage(e, 'No se pudo actualizar el estado'))
    });
  }

  resolve() {
    const x = this.selected();
    if (!x) return;
    const c = window.prompt('Indique la resolución');
    if (!c?.trim()) return;
    this.service.resolve(x.id, c.trim()).subscribe({
      next: (r) => {
        if (r.data) this.select(r.data);
        this.load();
      },
      error: (e) => this.toast.error(extractHttpErrorMessage(e, 'No se pudo resolver la incidencia'))
    });
  }

  addAction() {
    const x = this.selected();
    if (!x) return;
    const d = window.prompt('Describa la acción administrativa');
    if (!d?.trim()) return;
    this.service.addAction(x.id, { actionType: 'OTHER', description: d.trim(), actionDate: new Date().toISOString().slice(0, 10) }).subscribe({
      next: () => this.select(x),
      error: (e) => this.toast.error(extractHttpErrorMessage(e, 'No se pudo agregar la acción'))
    });
  }

  updateAction(a: IncidentAction, target: 'start' | 'complete') {
    const x = this.selected();
    if (!x) return;
    const call = target === 'start' ? this.service.startAction(x.id, a.id) : this.service.completeAction(x.id, a.id);
    call.subscribe({
      next: () => this.select(x),
      error: (e) => this.toast.error(extractHttpErrorMessage(e, 'No se pudo actualizar la acción'))
    });
  }

  private emptyForm() {
    return {
      employeeId: '',
      incidentType: 'OTHER' as IncidentType,
      severity: 'MEDIUM' as IncidentSeverity,
      title: '',
      description: '',
      occurredAt: '',
      location: ''
    };
  }
}
