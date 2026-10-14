import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import {
  EmployeeFileListUseCase,
  DocumentCategoryFilterComponent,
  DocumentCardGridComponent,
  DocumentTimelineComponent,
  DocumentUploadDialogComponent,
  DocumentPreviewModalComponent,
  EmployeeProfileBannerComponent,
  EmployeeProfile
} from '../../features/employee-file-management';
import { PlatformPaginationComponent } from '../../features/platform/ui/platform-pagination/platform-pagination.component';
import { MOCK_DOSSIERS } from '../../shared/api/interceptors/data/mock-dossiers.data';

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
    DocumentPreviewModalComponent,
    EmployeeProfileBannerComponent
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
                Centraliza y clasifica la documentación legal, contractual y personal del trabajador con trazabilidad inmutable y control de acceso sensible.
              </p>
            </div>
          </div>

          <div class="flex flex-wrap gap-3">
            <button
              type="button"
              (click)="reload()"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-border bg-card px-4 py-2 text-sm font-semibold text-primary transition-colors hover:bg-accent">
              <lucide-icon name="refresh-ccw" [size]="16"></lucide-icon>
              Recargar
            </button>

            <button
              type="button"
              (click)="openUploadModal()"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground shadow-md transition-all hover:bg-primary/90">
              <lucide-icon name="upload" [size]="16"></lucide-icon>
              Adjuntar Documento
            </button>
          </div>
        </div>
      </section>

      <!-- Ficha del Colaborador Activo y Selector de Expediente -->
      <app-employee-profile-banner
        [employee]="currentEmployee()"
        [employees]="employeeProfiles"
        (employeeChange)="onEmployeeSelected($event)">
      </app-employee-profile-banner>

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
          (delete)="onDeleteDocument($event)">
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

      <!-- Modal de Carga de Documentos -->
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

  activeView: 'GRID' | 'TIMELINE' = 'GRID';
  isUploadOpen = false;

  readonly employeeProfiles: EmployeeProfile[] = [
    {
      id: '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
      name: 'Carlos Mamani Quispe',
      position: 'Ingeniero de Software Senior',
      department: 'Tecnología & Desarrollo',
      ci: '7483920-LP',
      contractType: 'Indefinido',
      branch: 'Sede Central La Paz',
      tenure: '2 años, 4 meses',
      status: 'ACTIVO'
    },
    {
      id: '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6e',
      name: 'Ana Morales Condori',
      position: 'Analista de Finanzas y Contabilidad',
      department: 'Administración & Finanzas',
      ci: '6192841-SC',
      contractType: 'Indefinido',
      branch: 'Sucursal Santa Cruz',
      tenure: '1 año, 8 meses',
      status: 'ACTIVO'
    },
    {
      id: '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6f',
      name: 'Roberto Gómez Flores',
      position: 'Operador de Turno Continuo',
      department: 'Operaciones & Logística',
      ci: '5328901-CB',
      contractType: 'Plazo Fijo',
      branch: 'Planta Cochabamba',
      tenure: '7 meses',
      status: 'ACTIVO'
    }
  ];

  readonly currentEmployee = computed(() => {
    const id = this.fileListUseCase.selectedEmployeeId();
    return this.employeeProfiles.find((e) => e.id === id) || this.employeeProfiles[0];
  });

  ngOnInit(): void {
    this.fileListUseCase.loadDocumentTypes();
    this.fileListUseCase.loadDocuments();
  }

  getAudits() {
    return MOCK_DOSSIERS.documentAudits;
  }

  reload(): void {
    this.fileListUseCase.loadDocuments();
  }

  onEmployeeSelected(empId: string): void {
    this.fileListUseCase.setEmployeeId(empId);
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

  onDeleteDocument(docId: string): void {
    this.fileListUseCase.deleteDocument(docId);
  }
}
