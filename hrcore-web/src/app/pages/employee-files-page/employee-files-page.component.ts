import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
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
import { firstValueFrom } from 'rxjs';

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
              Gestión de Talento Humano
            </div>
            <div>
              <h1 class="text-3xl font-black tracking-tight text-accent-foreground sm:text-4xl">
                Expedientes Laborales
              </h1>
              <p class="mt-2 max-w-3xl text-sm leading-6 text-muted-foreground">
                Centraliza y clasifica la documentación legal, contractual y personal del trabajador con custodia inmutable, archivo legal y control RBAC.
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
              class="inline-flex cursor-pointer items-center gap-2 rounded-full bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground shadow-md transition-all hover:bg-primary/90">
              <lucide-icon name="upload" [size]="16"></lucide-icon>
              Adjuntar Documento
            </button>
          </div>
        </div>
      </section>

      <section class="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <div class="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div class="flex-1 max-w-xl">
            <label class="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
              Empleado del expediente
            </label>
            <select
              [(ngModel)]="selectedEmployeeId"
              (ngModelChange)="onEmployeeSelected($event)"
              class="w-full rounded-xl border border-border bg-background px-3.5 py-2.5 text-sm font-medium text-foreground">
              <option value="" disabled>Seleccione un empleado</option>
              <option *ngFor="let employee of employees" [value]="employee.id">
                {{ employee.employeeNumber }} · {{ employee.firstName }} {{ employee.lastName }}
              </option>
            </select>
          </div>
          <button
            *ngIf="selectedEmployeeId && !fileListUseCase.activeFile()"
            type="button"
            (click)="createEmployeeFile()"
            class="inline-flex items-center justify-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground">
            <lucide-icon name="folder-plus" [size]="16"></lucide-icon>
            Crear expediente
          </button>
        </div>
      </section>

      <!-- Selector y Estado del Expediente Activo -->
      <section
        *ngIf="fileListUseCase.activeFile()"
        class="rounded-2xl border border-border bg-card p-5 shadow-sm flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div class="flex items-center gap-4">
          <div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-primary/10 text-primary font-black text-base border border-primary/20">
            <lucide-icon name="folder" [size]="22"></lucide-icon>
          </div>
          <div>
            <div class="flex flex-wrap items-center gap-2">
              <h2 class="text-base font-bold text-accent-foreground">
                Expediente N° {{ fileListUseCase.activeFile()?.fileNumber }}
              </h2>
              <span class="inline-flex items-center gap-1 rounded-full bg-emerald-500/10 px-2.5 py-0.5 text-[11px] font-bold text-emerald-600 border border-emerald-500/20">
                <span class="h-1.5 w-1.5 rounded-full bg-emerald-500"></span>
                {{ fileListUseCase.activeFile()?.status }}
              </span>
            </div>
            <p class="text-xs text-muted-foreground mt-0.5">
              {{ fileListUseCase.activeFile()?.notes || 'Expediente digital activo en el sistema' }}
            </p>
          </div>
        </div>

        <!-- Selector si hay múltiples expedientes -->
        <div *ngIf="fileListUseCase.files().length > 1" class="min-w-[220px]">
          <label class="block text-[11px] font-bold uppercase tracking-wider text-muted-foreground mb-1">
            Cambiar Expediente
          </label>
          <select
            [ngModel]="fileListUseCase.activeFile()?.id"
            (ngModelChange)="onFileSelected($event)"
            class="w-full rounded-xl border border-border bg-muted/40 px-3 py-1.5 text-xs font-semibold text-foreground outline-none focus:border-primary focus:ring-1 focus:ring-primary cursor-pointer">
            <option *ngFor="let f of fileListUseCase.files()" [value]="f.id">
              Expediente: {{ f.fileNumber }}
            </option>
          </select>
        </div>
      </section>

      <!-- Estado: Cargando -->
      <div *ngIf="fileListUseCase.loading()" class="rounded-2xl border border-border bg-card p-12 text-center flex flex-col items-center justify-center space-y-3">
        <lucide-icon name="loader-circle" [size]="32" class="animate-spin text-primary"></lucide-icon>
        <p class="text-xs font-semibold text-muted-foreground">Consultando expediente laboral y documentos...</p>
      </div>

      <!-- Estado Vacío cuando no hay documentos registrados -->
      <div
        *ngIf="!fileListUseCase.loading() && fileListUseCase.documents().length === 0"
        class="rounded-[24px] border-2 border-dashed border-border bg-card/60 p-12 text-center space-y-5 shadow-sm">
        <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-primary/10 text-primary">
          <lucide-icon name="folder-open" [size]="32"></lucide-icon>
        </div>
        <div class="max-w-md mx-auto space-y-2">
          <h3 class="text-xl font-bold text-accent-foreground">
            {{ fileListUseCase.activeFile() ? 'No hay documentos en el expediente' : 'El empleado no tiene expediente' }}
          </h3>
          <p class="text-xs text-muted-foreground leading-relaxed">
            {{ fileListUseCase.activeFile()
              ? 'Adjunta el primer documento para comenzar a gestionar el expediente digital.'
              : 'Crea el expediente laboral del empleado seleccionado antes de adjuntar documentación.' }}
          </p>
        </div>
        <div class="pt-2">
          <button
            type="button"
            (click)="fileListUseCase.activeFile() ? openUploadModal() : createEmployeeFile()"
            class="inline-flex cursor-pointer items-center gap-2 rounded-full bg-primary px-6 py-2.5 text-xs font-semibold text-primary-foreground shadow-md transition-all hover:bg-primary/90">
            <lucide-icon name="upload" [size]="16"></lucide-icon>
            {{ fileListUseCase.activeFile() ? 'Adjuntar Primer Documento' : 'Crear Expediente' }}
          </button>
        </div>
      </div>

      <!-- Contenido Principal cuando sí hay documentos -->
      <ng-container *ngIf="!fileListUseCase.loading() && fileListUseCase.documents().length > 0">
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
                placeholder="Buscar por nombre, tipo o nro de folio..."
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

        <!-- Vista: Línea de Tiempo -->
        <app-document-timeline
          *ngIf="activeView === 'TIMELINE'"
          [audits]="getAudits()">
        </app-document-timeline>
      </ng-container>

      <!-- Modal de Carga de Documentos (Flujo Canónico 2 Pasos Spring Boot) -->
      <app-document-upload-dialog
        [isOpen]="isUploadOpen"
        [employeeId]="fileListUseCase.selectedEmployeeId()"
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

  employees: EmployeeSummary[] = [];
  selectedEmployeeId = '';

  activeView: 'GRID' | 'TIMELINE' = 'GRID';
  isUploadOpen = false;

  async ngOnInit(): Promise<void> {
    this.fileListUseCase.loadDocumentTypes();
    try {
      const response = await firstValueFrom(this.employeesService.list(0, 100));
      this.employees = response.data?.content || [];
      this.selectedEmployeeId = this.employees[0]?.id || '';
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
    this.fileListUseCase.setEmployeeId(employeeId);
  }

  async createEmployeeFile(): Promise<void> {
    const employee = this.employees.find((item) => item.id === this.selectedEmployeeId);
    if (!employee) return;
    await this.fileListUseCase.createFileForEmployee(employee.id, employee.employeeNumber);
  }

  getAudits() {
    return this.fileListUseCase.audits();
  }

  reload(): void {
    this.fileListUseCase.loadDocuments();
  }

  onFileSelected(fileId: string): void {
    const file = this.fileListUseCase.files().find((f) => f.id === fileId);
    if (file) {
      this.fileListUseCase.setActiveFile(file);
    }
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
