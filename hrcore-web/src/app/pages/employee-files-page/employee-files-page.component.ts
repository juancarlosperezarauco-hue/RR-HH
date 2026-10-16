import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { firstValueFrom } from 'rxjs';
import {
  EmployeeFileListUseCase,
  DocumentCategoryFilterComponent,
  DocumentCardGridComponent,
  DocumentTimelineComponent,
  DocumentUploadDialogComponent,
  DocumentPreviewModalComponent
} from '../../features/employee-file-management';
import { PlatformPaginationComponent } from '../../features/platform/ui/platform-pagination/platform-pagination.component';
import { EmployeesService, EmployeeSummary } from '../../entities/employees';
import { EmployeeDocumentAuditResponse } from '../../entities/employee-file';

@Component({
  selector: 'app-employee-files-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    LucideAngularModule,
    PlatformPaginationComponent,
    DocumentCategoryFilterComponent,
    DocumentCardGridComponent,
    DocumentTimelineComponent,
    DocumentUploadDialogComponent,
    DocumentPreviewModalComponent
  ],
  template: `
    <div class="space-y-6 relative">
      <!-- Encabezado Principal -->
      <section class="rounded-[28px] border border-border bg-gradient-to-br from-card via-background to-muted p-6 shadow-lg">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div class="space-y-3">
            <div class="inline-flex items-center gap-2 rounded-full border border-border bg-card/80 px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-primary">
              <span class="h-2 w-2 rounded-full bg-secondary"></span>
              Gestión de Talento Humano · HU15
            </div>
            <div>
              <h1 class="text-3xl font-black tracking-tight text-accent-foreground sm:text-4xl">
                Expedientes Laborales Digitales
              </h1>
              <p class="mt-2 max-w-3xl text-sm leading-6 text-muted-foreground">
                Centraliza y clasifica la documentación legal, contractual y personal del trabajador con custodia inmutable, archivo legal y control RBAC en Spring Boot.
              </p>
            </div>
          </div>

          <div class="flex flex-wrap gap-3">
            <button
              type="button"
              (click)="reload()"
              [disabled]="fileListUseCase.loading()"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-border bg-card px-4 py-2 text-sm font-semibold text-primary transition-colors hover:bg-accent disabled:opacity-50">
              <lucide-icon name="refresh-ccw" [size]="16" [class.animate-spin]="fileListUseCase.loading()"></lucide-icon>
              Recargar
            </button>

            <button
              type="button"
              (click)="openUploadModal()"
              [disabled]="!fileListUseCase.activeFile()"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground shadow-md transition-all hover:bg-primary/90 disabled:opacity-40 disabled:cursor-not-allowed">
              <lucide-icon name="upload" [size]="16"></lucide-icon>
              Adjuntar Documento
            </button>
          </div>
        </div>
      </section>

      <!-- Selector de Empleado y Tarjeta del Titular -->
      <section class="rounded-2xl border border-border bg-card p-5 shadow-sm space-y-4">
        <div class="flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
          <div class="flex-1 max-w-2xl">
            <label class="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
              Seleccionar Colaborador Titular *
            </label>
            <select
              [(ngModel)]="selectedEmployeeId"
              (ngModelChange)="onEmployeeSelected($event)"
              class="w-full rounded-xl border border-border bg-background px-3.5 py-2.5 text-sm font-medium text-foreground outline-none focus:border-primary focus:ring-1 focus:ring-primary cursor-pointer">
              <option value="" disabled>Seleccione un colaborador de la empresa...</option>
              <option *ngFor="let emp of employees" [value]="emp.id">
                {{ emp.employeeNumber }} · {{ emp.firstName }} {{ emp.lastName }}
              </option>
            </select>
          </div>

          <!-- Botón Rápido de Apertura si no tiene expediente -->
          <div *ngIf="selectedEmployee && !fileListUseCase.activeFile() && !fileListUseCase.loading()">
            <button
              type="button"
              (click)="openCreateModal()"
              class="inline-flex cursor-pointer items-center gap-2 rounded-xl bg-primary px-5 py-2.5 text-sm font-bold text-primary-foreground shadow-md transition-all hover:bg-primary/90">
              <lucide-icon name="folder-plus" [size]="18"></lucide-icon>
              Aperturar Expediente
            </button>
          </div>
        </div>

        <!-- Ficha Resumen del Colaborador Seleccionado -->
        <div *ngIf="selectedEmployee" class="flex flex-col gap-4 rounded-xl border border-border/80 bg-muted/20 p-4 sm:flex-row sm:items-center sm:justify-between">
          <div class="flex items-center gap-3.5">
            <div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-primary/10 text-primary font-black text-sm border border-primary/20">
              {{ getInitials(selectedEmployee.firstName, selectedEmployee.lastName) }}
            </div>
            <div>
              <div class="flex flex-wrap items-center gap-2">
                <h2 class="text-sm font-bold text-accent-foreground">
                  {{ selectedEmployee.firstName }} {{ selectedEmployee.lastName }}
                </h2>
                <span class="rounded-full bg-primary/10 px-2 py-0.5 text-[11px] font-bold text-primary">
                  {{ selectedEmployee.employeeNumber }}
                </span>
                <span class="rounded-full bg-emerald-500/10 px-2 py-0.5 text-[11px] font-bold text-emerald-600">
                  {{ selectedEmployee.employmentStatus || 'ACTIVO' }}
                </span>
              </div>
              <p class="text-xs text-muted-foreground mt-0.5">
                Ficha de colaborador registrada en el tenant
              </p>
            </div>
          </div>

          <!-- Estado del Expediente del Colaborador -->
          <div class="text-right sm:border-l sm:border-border sm:pl-4">
            <span class="text-[11px] font-bold uppercase tracking-wider text-muted-foreground block">
              Estado Expediente
            </span>
            <div *ngIf="fileListUseCase.activeFile(); else noFileStatusTpl" class="flex items-center gap-1.5 justify-end mt-0.5">
              <span class="h-2 w-2 rounded-full bg-emerald-500"></span>
              <span class="text-xs font-bold text-foreground">
                Folio: {{ fileListUseCase.activeFile()?.fileNumber }}
              </span>
            </div>
            <ng-template #noFileStatusTpl>
              <span class="text-xs font-semibold text-amber-600 mt-0.5 inline-block">
                Sin expediente aperturado
              </span>
            </ng-template>
          </div>
        </div>
      </section>

      <!-- Estado: Cargando -->
      <div *ngIf="fileListUseCase.loading()" class="rounded-2xl border border-border bg-card p-12 text-center flex flex-col items-center justify-center space-y-3">
        <lucide-icon name="loader-circle" [size]="32" class="animate-spin text-primary"></lucide-icon>
        <p class="text-xs font-semibold text-muted-foreground">Consultando expediente laboral y documentos en el backend...</p>
      </div>

      <!-- Estado A: El empleado NO tiene expediente digital aún -->
      <div
        *ngIf="!fileListUseCase.loading() && selectedEmployee && !fileListUseCase.activeFile()"
        class="rounded-[24px] border-2 border-dashed border-border bg-card/60 p-10 text-center space-y-4 shadow-sm">
        <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-amber-500/10 text-amber-600">
          <lucide-icon name="folder-plus" [size]="32"></lucide-icon>
        </div>
        <div class="max-w-md mx-auto space-y-2">
          <h3 class="text-xl font-bold text-accent-foreground">
            Expediente Digital No Aperturado
          </h3>
          <p class="text-xs text-muted-foreground leading-relaxed">
            El colaborador <strong>{{ selectedEmployee.firstName }} {{ selectedEmployee.lastName }}</strong> ({{ selectedEmployee.employeeNumber }}) aún no cuenta con un expediente laboral en el sistema.
            Apertura su expediente para comenzar a archivar y clasificar sus contratos, cédulas y certificados.
          </p>
        </div>
        <div class="pt-2">
          <button
            type="button"
            (click)="openCreateModal()"
            class="inline-flex cursor-pointer items-center gap-2 rounded-full bg-primary px-6 py-2.5 text-xs font-bold text-primary-foreground shadow-md transition-all hover:bg-primary/90">
            <lucide-icon name="folder-plus" [size]="16"></lucide-icon>
            Aperturar Expediente de {{ selectedEmployee.firstName }}
          </button>
        </div>
      </div>

      <!-- Estado B: Hay expediente activo pero sin documentos -->
      <div
        *ngIf="!fileListUseCase.loading() && fileListUseCase.activeFile() && fileListUseCase.documents().length === 0"
        class="rounded-[24px] border-2 border-dashed border-border bg-card/60 p-10 text-center space-y-4 shadow-sm">
        <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-primary/10 text-primary">
          <lucide-icon name="folder-open" [size]="32"></lucide-icon>
        </div>
        <div class="max-w-md mx-auto space-y-2">
          <h3 class="text-xl font-bold text-accent-foreground">
            No hay documentos en el expediente N° {{ fileListUseCase.activeFile()?.fileNumber }}
          </h3>
          <p class="text-xs text-muted-foreground leading-relaxed">
            El expediente digital está aperturado y activo. Adjunta el primer documento (contrato de trabajo, documento de identidad, títulos, etc.) para iniciar el registro.
          </p>
        </div>
        <div class="pt-2">
          <button
            type="button"
            (click)="openUploadModal()"
            class="inline-flex cursor-pointer items-center gap-2 rounded-full bg-primary px-6 py-2.5 text-xs font-bold text-primary-foreground shadow-md transition-all hover:bg-primary/90">
            <lucide-icon name="upload" [size]="16"></lucide-icon>
            Adjuntar Primer Documento
          </button>
        </div>
      </div>

      <!-- Contenido Principal cuando sí hay documentos registrados -->
      <ng-container *ngIf="!fileListUseCase.loading() && fileListUseCase.activeFile() && fileListUseCase.documents().length > 0">
        <!-- Fila de Tarjetas KPI -->
        <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <!-- KPI 1 -->
          <div class="rounded-2xl border border-border bg-card p-5 shadow-sm flex items-center justify-between">
            <div class="space-y-1">
              <p class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Total en Expediente</p>
              <p class="text-2xl font-black text-accent-foreground">{{ fileListUseCase.kpis().total }}</p>
            </div>
            <div class="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary/10 text-primary">
              <lucide-icon name="file-text" [size]="22"></lucide-icon>
            </div>
          </div>

          <!-- KPI 2 -->
          <div class="rounded-2xl border border-border bg-card p-5 shadow-sm flex items-center justify-between">
            <div class="space-y-1">
              <p class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Documentos Sensibles</p>
              <p class="text-2xl font-black text-amber-600">{{ fileListUseCase.kpis().sensitive }}</p>
            </div>
            <div class="flex h-12 w-12 items-center justify-center rounded-2xl bg-amber-500/10 text-amber-600">
              <lucide-icon name="shield-alert" [size]="22"></lucide-icon>
            </div>
          </div>

          <!-- KPI 3 -->
          <div class="rounded-2xl border border-border bg-card p-5 shadow-sm flex items-center justify-between">
            <div class="space-y-1">
              <p class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Próximos a Vencer (60d)</p>
              <p class="text-2xl font-black text-rose-600">{{ fileListUseCase.kpis().expiringSoon }}</p>
            </div>
            <div class="flex h-12 w-12 items-center justify-center rounded-2xl bg-rose-500/10 text-rose-600">
              <lucide-icon name="clock" [size]="22"></lucide-icon>
            </div>
          </div>
        </div>

        <!-- Barra de Filtros y Selector de Vista -->
        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm space-y-4">
          <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <!-- Búsqueda -->
            <div class="relative flex-1 max-w-md">
              <lucide-icon
                name="search"
                [size]="16"
                class="absolute left-3.5 top-1/2 -translate-y-1/2 text-muted-foreground"></lucide-icon>
              <input
                type="text"
                [ngModel]="fileListUseCase.searchQuery()"
                (ngModelChange)="fileListUseCase.setSearchQuery($event)"
                placeholder="Buscar por título, tipo o nro de folio..."
                class="w-full rounded-full border border-border bg-muted/40 pl-10 pr-4 py-2 text-xs font-medium text-foreground outline-none focus:border-primary focus:ring-1 focus:ring-primary" />
            </div>

            <!-- Selector de Modo de Vista (Galería vs Línea de Tiempo) -->
            <div class="inline-flex rounded-full border border-border bg-muted/30 p-1">
              <button
                type="button"
                (click)="activeView = 'GRID'"
                [class.bg-card]="activeView === 'GRID'"
                [class.text-primary]="activeView === 'GRID'"
                [class.shadow-sm]="activeView === 'GRID'"
                [class.text-muted-foreground]="activeView !== 'GRID'"
                class="inline-flex items-center gap-1.5 rounded-full px-4 py-1.5 text-xs font-semibold transition-all cursor-pointer">
                <lucide-icon name="file-text" [size]="14"></lucide-icon>
                Documentos
              </button>
              <button
                type="button"
                (click)="activeView = 'TIMELINE'"
                [class.bg-card]="activeView === 'TIMELINE'"
                [class.text-primary]="activeView === 'TIMELINE'"
                [class.shadow-sm]="activeView === 'TIMELINE'"
                [class.text-muted-foreground]="activeView !== 'TIMELINE'"
                class="inline-flex items-center gap-1.5 rounded-full px-4 py-1.5 text-xs font-semibold transition-all cursor-pointer">
                <lucide-icon name="clock" [size]="14"></lucide-icon>
                Línea de Tiempo
              </button>
            </div>
          </div>

          <!-- Filtro por Categorías (solo en vista de grilla) -->
          <app-document-category-filter
            *ngIf="activeView === 'GRID'"
            [activeCategory]="fileListUseCase.selectedCategory()"
            (categoryChange)="fileListUseCase.setCategory($event)">
          </app-document-category-filter>
        </div>

        <!-- Vista: Grilla de Tarjetas -->
        <div *ngIf="activeView === 'GRID'" class="space-y-4">
          <app-document-card-grid
            [documents]="fileListUseCase.filteredDocuments()"
            (verify)="onVerifyDocument($event)"
            (archive)="onArchiveDocument($event)"
            (delete)="onArchiveDocument($event)">
          </app-document-card-grid>

          <!-- Paginación -->
          <div *ngIf="fileListUseCase.totalPages() > 1" class="flex justify-end pt-2">
            <app-platform-pagination
              [currentPage]="fileListUseCase.currentPage()"
              [totalPages]="fileListUseCase.totalPages()"
              [totalElements]="fileListUseCase.totalElements()"
              (pageChange)="fileListUseCase.setPage($event)">
            </app-platform-pagination>
          </div>
        </div>

        <!-- Vista: Línea de Tiempo Real (Audit Trail) -->
        <app-document-timeline
          *ngIf="activeView === 'TIMELINE'"
          [audits]="audits()">
        </app-document-timeline>
      </ng-container>

      <!-- Modal: Apertura de Nuevo Expediente (POST /api/employee-files) -->
      <div *ngIf="showCreateFileDialog" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm">
        <div class="relative w-full max-w-md rounded-[24px] border border-border bg-card p-6 shadow-2xl space-y-4">
          <div class="flex items-center justify-between border-b border-border pb-3">
            <div>
              <h3 class="text-lg font-bold text-accent-foreground">Aperturar Expediente Digital</h3>
              <p class="text-xs text-muted-foreground">Crea la carpeta laboral maestra del colaborador en Spring Boot.</p>
            </div>
            <button
              type="button"
              (click)="showCreateFileDialog = false"
              class="rounded-full p-1.5 text-muted-foreground hover:bg-muted">
              <lucide-icon name="x" [size]="16"></lucide-icon>
            </button>
          </div>

          <form (ngSubmit)="submitCreateFile()" class="space-y-3.5">
            <div>
              <label class="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1">
                Colaborador Titular
              </label>
              <div class="rounded-xl border border-border bg-muted/40 px-3.5 py-2 text-xs font-bold text-foreground">
                {{ selectedEmployee?.firstName }} {{ selectedEmployee?.lastName }} ({{ selectedEmployee?.employeeNumber }})
              </div>
            </div>

            <div>
              <label class="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1">
                Número de Expediente / Folio *
              </label>
              <input
                type="text"
                [(ngModel)]="newFileNumber"
                name="newFileNumber"
                required
                maxlength="80"
                placeholder="Ej: EXP-EMP-00104"
                class="w-full rounded-xl border border-border bg-background px-3.5 py-2 text-sm text-foreground outline-none focus:border-primary focus:ring-1 focus:ring-primary font-mono" />
            </div>

            <div>
              <label class="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1">
                Observaciones / Notas Iniciales
              </label>
              <textarea
                [(ngModel)]="newFileNotes"
                name="newFileNotes"
                rows="2"
                placeholder="Notas sobre la apertura del expediente digital..."
                class="w-full rounded-xl border border-border bg-background px-3.5 py-2 text-sm text-foreground outline-none focus:border-primary focus:ring-1 focus:ring-primary"></textarea>
            </div>

            <div class="flex items-center justify-end gap-2.5 pt-3 border-t border-border">
              <button
                type="button"
                (click)="showCreateFileDialog = false"
                class="rounded-full border border-border px-4 py-2 text-xs font-semibold text-muted-foreground hover:bg-accent">
                Cancelar
              </button>
              <button
                type="submit"
                [disabled]="!newFileNumber || isCreatingFile"
                class="inline-flex cursor-pointer items-center gap-2 rounded-full bg-primary px-5 py-2 text-xs font-bold text-primary-foreground shadow-md transition-all hover:bg-primary/90 disabled:opacity-50">
                <lucide-icon *ngIf="isCreatingFile" name="loader-circle" [size]="14" class="animate-spin"></lucide-icon>
                {{ isCreatingFile ? 'Aperturando...' : 'Confirmar Apertura' }}
              </button>
            </div>
          </form>
        </div>
      </div>

      <!-- Modal de Carga de Documentos (Flujo Canónico 2 Pasos Spring Boot) -->
      <app-document-upload-dialog
        [isOpen]="isUploadOpen"
        [fileId]="fileListUseCase.activeFile()?.id || ''"
        [fileNumber]="fileListUseCase.activeFile()?.fileNumber || ''"
        [employeeId]="selectedEmployeeId"
        [employeeName]="selectedEmployeeName"
        [documentTypes]="fileListUseCase.documentTypes()"
        (closeDialog)="isUploadOpen = false"
        (uploaded)="onDocumentUploaded()">
      </app-document-upload-dialog>

      <!-- Modal de Previsualización Embebida -->
      <app-document-preview-modal></app-document-preview-modal>
    </div>
  `
})
export class EmployeeFilesPageComponent implements OnInit {
  readonly fileListUseCase = inject(EmployeeFileListUseCase);
  private readonly employeesService = inject(EmployeesService);
  private readonly route = inject(ActivatedRoute);

  employees: EmployeeSummary[] = [];
  selectedEmployeeId = '';

  activeView: 'GRID' | 'TIMELINE' = 'GRID';
  isUploadOpen = false;
  showCreateFileDialog = false;
  newFileNumber = '';
  newFileNotes = 'Expediente digital principal del colaborador';
  isCreatingFile = false;

  get selectedEmployee(): EmployeeSummary | undefined {
    return this.employees.find((item) => item.id === this.selectedEmployeeId);
  }

  get selectedEmployeeName(): string {
    const emp = this.selectedEmployee;
    return emp ? `${emp.firstName} ${emp.lastName}` : '';
  }

  async ngOnInit(): Promise<void> {
    this.fileListUseCase.loadDocumentTypes();

    const queryEmployeeId = this.route.snapshot.queryParamMap.get('employeeId');

    try {
      const response = await firstValueFrom(this.employeesService.list(0, 100));
      this.employees = response.data?.content || [];

      if (queryEmployeeId && this.employees.some((e) => e.id === queryEmployeeId)) {
        this.selectedEmployeeId = queryEmployeeId;
      } else {
        this.selectedEmployeeId = this.employees[0]?.id || '';
      }

      if (this.selectedEmployeeId) {
        this.fileListUseCase.setEmployeeId(this.selectedEmployeeId);
      } else {
        await this.fileListUseCase.loadDocuments();
      }
    } catch {
      this.employees = [];
      await this.fileListUseCase.loadDocuments();
    }
  }

  onEmployeeSelected(employeeId: string): void {
    this.selectedEmployeeId = employeeId;
    this.fileListUseCase.setEmployeeId(employeeId);
  }

  openCreateModal(): void {
    if (!this.selectedEmployee) return;
    this.newFileNumber = `EXP-${this.selectedEmployee.employeeNumber || this.selectedEmployee.id.substring(0, 8)}`.toUpperCase();
    this.newFileNotes = 'Expediente digital principal del colaborador';
    this.showCreateFileDialog = true;
  }

  async submitCreateFile(): Promise<void> {
    if (!this.selectedEmployeeId || !this.newFileNumber) return;
    this.isCreatingFile = true;
    try {
      const ok = await this.fileListUseCase.createFileForEmployee(
        this.selectedEmployeeId,
        this.newFileNumber,
        this.newFileNotes
      );
      if (ok) {
        this.showCreateFileDialog = false;
      }
    } finally {
      this.isCreatingFile = false;
    }
  }

  /**
   * Genera auditoría reactiva y memoizada a partir del expediente activo y sus documentos
   */
  readonly audits = computed<EmployeeDocumentAuditResponse[]>(() => {
    const list: EmployeeDocumentAuditResponse[] = [];
    const file = this.fileListUseCase.activeFile();
    const docs = this.fileListUseCase.documents();

    if (file?.createdAt) {
      list.push({
        id: `audit-file-${file.id}`,
        documentId: file.id,
        userId: 'SYS',
        userEmail: 'Gestión RRHH',
        action: 'UPLOAD',
        performedAt: file.createdAt,
        ipAddress: `Apertura de Expediente: ${file.fileNumber}`
      });
    }

    for (const doc of docs) {
      if (doc.createdAt) {
        list.push({
          id: `audit-create-${doc.id}`,
          documentId: doc.id,
          userId: 'RRHH',
          userEmail: 'Gestión RRHH',
          action: 'UPLOAD',
          performedAt: doc.createdAt,
          ipAddress: `Registro: ${doc.title || doc.documentTypeName}`
        });
      }
      if (doc.verifiedAt) {
        list.push({
          id: `audit-verify-${doc.id}`,
          documentId: doc.id,
          userId: doc.verifiedBy || 'Auditoría RRHH',
          userEmail: 'Auditoría RRHH',
          action: 'VIEW',
          performedAt: doc.verifiedAt,
          ipAddress: `Verificación: ${doc.verificationStatus} (${doc.title || doc.documentTypeName})`
        });
      }
    }

    return list.sort((a, b) => new Date(b.performedAt).getTime() - new Date(a.performedAt).getTime());
  });

  getAudits(): EmployeeDocumentAuditResponse[] {
    return this.audits();
  }

  getInitials(first = '', last = ''): string {
    const f = first ? first.charAt(0).toUpperCase() : '';
    const l = last ? last.charAt(0).toUpperCase() : '';
    return f + l || 'EX';
  }

  reload(): void {
    this.fileListUseCase.loadDocuments();
  }

  openUploadModal(): void {
    this.isUploadOpen = true;
  }

  onDocumentUploaded(): void {
    this.fileListUseCase.loadDocuments();
  }

  onVerifyDocument(event: { id: string; status: 'VERIFIED' | 'REJECTED' }): void {
    this.fileListUseCase.verifyDocument(event.id, event.status);
  }

  onArchiveDocument(docId: string): void {
    this.fileListUseCase.archiveDocument(docId);
  }
}
