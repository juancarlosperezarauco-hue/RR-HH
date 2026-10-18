import { Component, EventEmitter, inject, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule } from 'lucide-angular';
import { EmployeeDocumentResponse } from '../../../../entities/employee-file';
import { EmployeeDocumentPreviewUseCase } from '../../application/employee-document-preview.usecase';
import { PermissionService } from '../../../../shared/lib/auth/permission.service';

@Component({
  selector: 'app-document-card-grid',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  template: `
    <div *ngIf="documents.length === 0" class="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border p-12 text-center bg-card/50">
      <div class="flex h-14 w-14 items-center justify-center rounded-2xl bg-muted text-muted-foreground mb-3">
        <lucide-icon name="file-text" [size]="28"></lucide-icon>
      </div>
      <h4 class="text-base font-bold text-accent-foreground">No se encontraron documentos</h4>
      <p class="text-xs text-muted-foreground max-w-sm mt-1">No hay documentos registrados para esta categoría o criterio de búsqueda en el expediente.</p>
    </div>

    <div *ngIf="documents.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
      <div
        *ngFor="let doc of documents"
        class="group relative flex flex-col justify-between rounded-2xl border border-border bg-card p-5 shadow-sm transition-all hover:shadow-md hover:border-primary/40">
        
        <!-- Header de la Tarjeta -->
        <div class="space-y-3">
          <div class="flex items-start justify-between gap-2">
            <!-- Badges: Categoría y Estado de Verificación -->
            <div class="flex flex-wrap items-center gap-1.5">
              <span
                [class]="getCategoryBadgeClass(doc.documentCategory)"
                class="inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-[11px] font-bold uppercase tracking-wider">
                {{ doc.documentCategory }}
              </span>

              <!-- Badge de Verificación Spring Boot -->
              <span
                *ngIf="doc.verificationStatus === 'VERIFIED'"
                class="inline-flex items-center gap-1 rounded-full bg-emerald-500/10 px-2 py-0.5 text-[10px] font-bold text-emerald-600 border border-emerald-500/20"
                title="Documento formalmente validado por RRHH">
                <lucide-icon name="check-circle" [size]="10"></lucide-icon>
                VERIFICADO
              </span>
              <span
                *ngIf="doc.verificationStatus === 'REJECTED'"
                class="inline-flex items-center gap-1 rounded-full bg-rose-500/10 px-2 py-0.5 text-[10px] font-bold text-rose-600 border border-rose-500/20"
                title="Documento observado o rechazado">
                <lucide-icon name="circle-x" [size]="10"></lucide-icon>
                RECHAZADO
              </span>
              <span
                *ngIf="!doc.verificationStatus || doc.verificationStatus === 'PENDING'"
                class="inline-flex items-center gap-1 rounded-full bg-amber-500/10 px-2 py-0.5 text-[10px] font-bold text-amber-600 border border-amber-500/20"
                title="Pendiente de revisión documental">
                <lucide-icon name="clock" [size]="10"></lucide-icon>
                PENDIENTE
              </span>
            </div>

            <!-- Badge Confidencial / Sensible -->
            <span
              *ngIf="doc.confidential || doc.documentCategory === 'HEALTH'"
              class="inline-flex items-center gap-1 rounded-full bg-red-500/10 px-2 py-0.5 text-[10px] font-bold text-red-600 border border-red-500/20 shrink-0"
              title="Documento confidencial con acceso RBAC restringido">
              <lucide-icon name="lock" [size]="10"></lucide-icon>
              CONFIDENCIAL
            </span>
          </div>

          <!-- Título y Tipo -->
          <div>
            <h4 class="text-sm font-bold text-accent-foreground group-hover:text-primary transition-colors line-clamp-1" [title]="doc.fileName">
              {{ doc.fileName }}
            </h4>
            <p class="text-xs text-muted-foreground font-medium mt-0.5">
              {{ doc.documentTypeName }}
              <span *ngIf="doc.documentNumber" class="text-foreground"> • Folio: {{ doc.documentNumber }}</span>
            </p>
          </div>
        </div>

        <!-- Metadatos de Archivo y Vigencia -->
        <div class="my-4 space-y-1.5 border-t border-b border-border/60 py-3 text-[11px] text-muted-foreground">
          <div class="flex items-center justify-between">
            <span>Tamaño:</span>
            <span class="font-medium text-foreground">{{ formatBytes(doc.fileSizeBytes) }}</span>
          </div>
          <div class="flex items-center justify-between">
            <span>Versión:</span>
            <span class="font-medium text-foreground">v{{ doc.versionNumber }}</span>
          </div>
          <div *ngIf="doc.issueDate" class="flex items-center justify-between">
            <span>Emisión:</span>
            <span class="font-medium text-foreground">{{ doc.issueDate }}</span>
          </div>

          <!-- Alerta de Vencimiento -->
          <div *ngIf="doc.expirationDate" class="flex items-center justify-between pt-1">
            <span>Vencimiento:</span>
            <span [class]="getExpirationClass(doc.expirationDate)" class="font-bold">
              {{ doc.expirationDate }}
            </span>
          </div>
        </div>

        <!-- Acciones Inferiores (Protegidas por RBAC si es documento confidencial) -->
        <div *ngIf="canAccessDocument(doc); else sensitiveBlocked" class="space-y-2 pt-1">
          <div class="flex items-center justify-between gap-2">
            <button
              type="button"
              (click)="previewUseCase.openPreview(doc)"
              class="inline-flex flex-1 cursor-pointer items-center justify-center gap-1.5 rounded-full border border-border bg-muted/40 py-1.5 text-xs font-semibold text-primary transition-colors hover:bg-primary hover:text-primary-foreground">
              <lucide-icon name="eye" [size]="14"></lucide-icon>
              Ver
            </button>

            <a
              *ngIf="doc.downloadUrl"
              [href]="doc.downloadUrl"
              target="_blank"
              download
              class="inline-flex h-8 w-8 cursor-pointer items-center justify-center rounded-full border border-border bg-card text-muted-foreground transition-colors hover:bg-accent hover:text-foreground"
              title="Descargar">
              <lucide-icon name="download" [size]="14"></lucide-icon>
            </a>

            <button
              type="button"
              (click)="onArchive(doc.id)"
              class="inline-flex h-8 w-8 cursor-pointer items-center justify-center rounded-full border border-border bg-card text-muted-foreground transition-colors hover:bg-amber-500/10 hover:text-amber-600"
              title="Archivar documento (cumplimiento legal)">
              <lucide-icon name="archive" [size]="14"></lucide-icon>
            </button>
          </div>

          <!-- Acciones de Verificación / Auditoría (RRHH / Admin) -->
          <div *ngIf="canVerify()" class="flex items-center gap-1.5 pt-1.5 border-t border-border/40">
            <button
              *ngIf="doc.verificationStatus !== 'VERIFIED'"
              type="button"
              (click)="onVerify(doc.id, 'VERIFIED')"
              class="inline-flex flex-1 items-center justify-center gap-1 rounded-lg bg-emerald-500/10 py-1 text-[11px] font-semibold text-emerald-600 hover:bg-emerald-500 hover:text-white transition-colors cursor-pointer"
              title="Validar y aprobar formalmente el documento">
              <lucide-icon name="check" [size]="12"></lucide-icon>
              Verificar
            </button>
            <button
              *ngIf="doc.verificationStatus !== 'REJECTED'"
              type="button"
              (click)="onVerify(doc.id, 'REJECTED')"
              class="inline-flex flex-1 items-center justify-center gap-1 rounded-lg bg-rose-500/10 py-1 text-[11px] font-semibold text-rose-600 hover:bg-rose-500 hover:text-white transition-colors cursor-pointer"
              title="Rechazar documento con observación">
              <lucide-icon name="x" [size]="12"></lucide-icon>
              Rechazar
            </button>
          </div>
        </div>

        <ng-template #sensitiveBlocked>
          <div class="flex items-center justify-center gap-1.5 rounded-xl bg-amber-500/10 px-3 py-2 text-center text-[11px] font-semibold text-amber-600 border border-amber-500/20 w-full" title="Requiere permiso dossiers.sensitive.read">
            <lucide-icon name="lock" [size]="13"></lucide-icon>
            <span>Documento Confidencial - Requiere Autorización Especial</span>
          </div>
        </ng-template>
      </div>
    </div>
  `
})
export class DocumentCardGridComponent {
  @Input() documents: EmployeeDocumentResponse[] = [];
  @Output() archive = new EventEmitter<string>();
  @Output() delete = new EventEmitter<string>();
  @Output() verify = new EventEmitter<{ id: string; status: 'VERIFIED' | 'REJECTED' }>();

  readonly previewUseCase = inject(EmployeeDocumentPreviewUseCase);
  readonly permissionService = inject(PermissionService);

  canAccessDocument(doc: EmployeeDocumentResponse): boolean {
    if (!doc.confidential && doc.documentCategory !== 'HEALTH') return true;
    return (
      this.permissionService.hasPermission('dossiers.sensitive.read') ||
      this.permissionService.hasPermission('employee_files.documents.read') ||
      this.permissionService.hasRole('ADMIN') ||
      this.permissionService.hasRole('OWNER_ADMIN')
    );
  }

  canVerify(): boolean {
    return (
      this.permissionService.hasPermission('employee_files.documents.verify') ||
      this.permissionService.hasPermission('dossiers.manage') ||
      this.permissionService.hasRole('ADMIN') ||
      this.permissionService.hasRole('OWNER_ADMIN')
    );
  }

  onVerify(id: string, status: 'VERIFIED' | 'REJECTED'): void {
    this.verify.emit({ id, status });
  }

  onArchive(id: string): void {
    if (confirm('¿Está seguro de archivar este documento laboral? Conforme a normativas legales, se conservará en el sistema con estado ARCHIVADO.')) {
      this.archive.emit(id);
      this.delete.emit(id);
    }
  }

  onDelete(id: string): void {
    this.onArchive(id);
  }

  formatBytes(bytes: number): string {
    if (!bytes) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  getCategoryBadgeClass(category: string): string {
    switch (category) {
      case 'CONTRACT':
        return 'bg-blue-500/10 text-blue-600 border border-blue-500/20';
      case 'IDENTIFICATION':
        return 'bg-purple-500/10 text-purple-600 border border-purple-500/20';
      case 'EDUCATION':
        return 'bg-emerald-500/10 text-emerald-600 border border-emerald-500/20';
      case 'HEALTH':
        return 'bg-amber-500/10 text-amber-600 border border-amber-500/20';
      case 'DISCIPLINARY':
        return 'bg-rose-500/10 text-rose-600 border border-rose-500/20';
      default:
        return 'bg-muted text-muted-foreground border border-border';
    }
  }

  getExpirationClass(expDate: string): string {
    const diff = (new Date(expDate).getTime() - Date.now()) / (1000 * 60 * 60 * 24);
    if (diff < 0) return 'text-destructive'; // Vencido
    if (diff <= 60) return 'text-amber-500'; // Vence pronto
    return 'text-emerald-600';
  }
}
