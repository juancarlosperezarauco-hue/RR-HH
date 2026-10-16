import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule } from 'lucide-angular';
import { EmployeeDocumentPreviewUseCase } from '../../application/employee-document-preview.usecase';
import { EmployeeDocumentResponse } from '../../../../entities/employee-file';

@Component({
  selector: 'app-document-preview-modal',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  template: `
    <div
      *ngIf="previewUseCase.selectedDoc()"
      (click)="previewUseCase.closePreview()"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-3 sm:p-4 backdrop-blur-sm">
      <div
        (click)="$event.stopPropagation()"
        class="relative flex h-[92vh] w-full max-w-5xl flex-col rounded-[24px] border border-border bg-card shadow-2xl overflow-hidden">
        
        <!-- Header del Modal: Solo Título, Tipo y Controles Principales (sin repetir la metadata del recuadro) -->
        <div class="flex items-center justify-between border-b border-border bg-muted/40 px-6 py-3.5 shrink-0">
          <div class="flex items-center gap-3 min-w-0">
            <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary">
              <lucide-icon name="file-text" [size]="20"></lucide-icon>
            </div>
            <div class="min-w-0">
              <h3 class="truncate text-base font-bold text-accent-foreground" [title]="previewUseCase.selectedDoc()?.title || previewUseCase.selectedDoc()?.fileName">
                {{ previewUseCase.selectedDoc()?.title || previewUseCase.selectedDoc()?.fileName }}
              </h3>
              <p class="text-xs text-muted-foreground flex items-center gap-1.5">
                <span>{{ previewUseCase.selectedDoc()?.documentTypeName }}</span>
                <span *ngIf="previewUseCase.selectedDoc()?.versionNumber" class="text-foreground/80 font-medium">
                  • v{{ previewUseCase.selectedDoc()?.versionNumber }}
                </span>
              </p>
            </div>
          </div>

          <div class="flex items-center gap-2">
            <!-- Botón Alternar Información / Metadatos -->
            <button
              type="button"
              (click)="showMetadata = !showMetadata"
              class="inline-flex cursor-pointer items-center gap-1.5 rounded-full border border-border bg-card px-3 py-1.5 text-xs font-semibold text-muted-foreground transition-colors hover:bg-accent hover:text-foreground"
              [title]="showMetadata ? 'Ocultar información del formulario' : 'Mostrar información del formulario'">
              <lucide-icon name="info" [size]="14"></lucide-icon>
              <span class="hidden sm:inline">{{ showMetadata ? 'Ocultar datos' : 'Ver datos' }}</span>
            </button>

            <!-- Botón Descargar -->
            <button
              type="button"
              (click)="previewUseCase.downloadCurrent()"
              class="inline-flex cursor-pointer items-center gap-1.5 rounded-full border border-border bg-card px-3.5 py-1.5 text-xs font-semibold text-primary transition-colors hover:bg-accent"
              title="Descargar archivo en su equipo">
              <lucide-icon name="download" [size]="14"></lucide-icon>
              <span>Descargar</span>
            </button>

            <!-- Botón Cerrar -->
            <button
              type="button"
              (click)="previewUseCase.closePreview()"
              class="inline-flex h-8 w-8 cursor-pointer items-center justify-center rounded-full border border-border bg-card text-muted-foreground transition-colors hover:bg-destructive/10 hover:text-destructive"
              title="Cerrar vista previa">
              <lucide-icon name="x" [size]="16"></lucide-icon>
            </button>
          </div>
        </div>

        <!-- Contenedor Principal del Visor -->
        <div class="relative flex flex-1 flex-col bg-muted/20 p-4 gap-3 overflow-hidden min-h-0">
          
          <!-- Recuadro Superior de Metadatos del Formulario (dentro del visor, sin repetir el header) -->
          <div
            *ngIf="showMetadata && previewUseCase.selectedDoc() as doc"
            class="rounded-xl border border-border bg-card/95 p-3 shadow-xs shrink-0 backdrop-blur-xs">
            <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
              
              <!-- 1. Nombre y Tamaño del Archivo Físico -->
              <div class="min-w-0 space-y-0.5">
                <span class="text-[10px] font-bold uppercase tracking-wider text-muted-foreground block truncate">
                  Archivo Digital
                </span>
                <p class="truncate text-xs font-semibold text-accent-foreground" [title]="doc.fileName">
                  {{ doc.fileName }}
                </p>
                <p class="text-[10px] text-muted-foreground">
                  {{ formatBytes(doc.fileSizeBytes || 0) }}
                </p>
              </div>

              <!-- 2. N° de Folio / Documento -->
              <div class="min-w-0 space-y-0.5">
                <span class="text-[10px] font-bold uppercase tracking-wider text-muted-foreground block truncate">
                  Folio / N° Doc
                </span>
                <p class="text-xs font-semibold text-foreground font-mono truncate" [title]="doc.documentNumber || 'Sin folio'">
                  {{ doc.documentNumber || 'Sin folio' }}
                </p>
                <p class="text-[10px] text-muted-foreground">
                  Identificador
                </p>
              </div>

              <!-- 3. Fecha de Emisión -->
              <div class="min-w-0 space-y-0.5">
                <span class="text-[10px] font-bold uppercase tracking-wider text-muted-foreground block truncate">
                  Fecha Emisión
                </span>
                <p class="text-xs font-semibold text-foreground">
                  {{ doc.issueDate || doc.issuedAt || 'No especificada' }}
                </p>
                <p class="text-[10px] text-muted-foreground">
                  Expedición legal
                </p>
              </div>

              <!-- 4. Fecha de Caducidad con Indicador de Vigencia -->
              <div class="min-w-0 space-y-0.5">
                <span class="text-[10px] font-bold uppercase tracking-wider text-muted-foreground block truncate">
                  Fecha Caducidad
                </span>
                <div class="flex items-center gap-1.5 flex-wrap">
                  <span class="text-xs font-semibold text-foreground">
                    {{ doc.expirationDate || doc.expiresAt || 'Permanente' }}
                  </span>
                  <span
                    *ngIf="doc.expirationDate || doc.expiresAt"
                    [class]="getExpirationBadge(doc.expirationDate || doc.expiresAt).class"
                    class="inline-flex items-center rounded-full px-1.5 py-0.2 text-[9px] font-bold uppercase tracking-wider border">
                    {{ getExpirationBadge(doc.expirationDate || doc.expiresAt).label }}
                  </span>
                </div>
                <p class="text-[10px] text-muted-foreground">
                  {{ (doc.expirationDate || doc.expiresAt) ? 'Vencimiento' : 'Sin fecha límite' }}
                </p>
              </div>

              <!-- 5. Confidencialidad -->
              <div class="min-w-0 space-y-0.5">
                <span class="text-[10px] font-bold uppercase tracking-wider text-muted-foreground block truncate">
                  Confidencialidad
                </span>
                <div class="pt-0.5">
                  <span
                    *ngIf="doc.confidential || doc.documentCategory === 'HEALTH'; else standardDocTpl"
                    class="inline-flex items-center gap-1 rounded-full bg-amber-500/10 px-2 py-0.5 text-[10px] font-bold text-amber-600 border border-amber-500/20"
                    title="Restringido para RRHH / Admin">
                    <lucide-icon name="lock" [size]="11"></lucide-icon>
                    Confidencial
                  </span>
                  <ng-template #standardDocTpl>
                    <span class="inline-flex items-center gap-1 rounded-full bg-muted/60 px-2 py-0.5 text-[10px] font-semibold text-muted-foreground border border-border">
                      <lucide-icon name="shield" [size]="11"></lucide-icon>
                      Estándar
                    </span>
                  </ng-template>
                </div>
              </div>

              <!-- 6. Estado de Verificación -->
              <div class="min-w-0 space-y-0.5">
                <span class="text-[10px] font-bold uppercase tracking-wider text-muted-foreground block truncate">
                  Estado Validación
                </span>
                <div class="pt-0.5">
                  <span
                    [class]="getVerificationBadge(doc.verificationStatus).class"
                    class="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-bold border">
                    <lucide-icon [name]="getVerificationBadge(doc.verificationStatus).icon" [size]="11"></lucide-icon>
                    {{ getVerificationBadge(doc.verificationStatus).label }}
                  </span>
                </div>
              </div>

            </div>
          </div>

          <!-- Spinner de Carga -->
          <div
            *ngIf="previewUseCase.loading()"
            class="absolute inset-0 z-10 flex flex-col items-center justify-center gap-3 bg-card/80 backdrop-blur-xs">
            <lucide-icon name="loader-circle" [size]="36" class="animate-spin text-primary"></lucide-icon>
            <p class="text-sm font-medium text-muted-foreground">Descargando y preparando vista previa segura...</p>
          </div>

          <!-- Error al cargar -->
          <div
            *ngIf="previewUseCase.error()"
            class="flex flex-1 flex-col items-center justify-center gap-3 text-center p-6 bg-card rounded-xl border border-border">
            <lucide-icon name="circle-alert" [size]="40" class="text-destructive"></lucide-icon>
            <p class="text-sm font-semibold text-destructive max-w-md">{{ previewUseCase.error() }}</p>
            <div class="flex items-center gap-2 mt-2">
              <button
                type="button"
                (click)="previewUseCase.downloadCurrent()"
                class="rounded-full bg-primary px-4 py-2 text-xs font-semibold text-primary-foreground hover:bg-primary/90 transition-colors cursor-pointer">
                Intentar Descargar
              </button>
              <button
                type="button"
                (click)="previewUseCase.closePreview()"
                class="rounded-full border border-border bg-card px-4 py-2 text-xs font-semibold text-muted-foreground hover:bg-accent transition-colors cursor-pointer">
                Cerrar
              </button>
            </div>
          </div>

          <!-- Recuadro del Visor de Archivo (PDF, Imagen, o Alternativo) -->
          <div
            *ngIf="!previewUseCase.loading() && !previewUseCase.error()"
            class="relative flex-1 min-h-0 w-full rounded-xl border border-border bg-card overflow-hidden shadow-inner">
            
            <!-- Visor PDF embebido -->
            <iframe
              *ngIf="isPdf(previewUseCase.selectedDoc())"
              [src]="previewUseCase.previewUrl()"
              class="h-full w-full border-0 bg-card"
              title="Vista previa del documento PDF">
            </iframe>

            <!-- Visor de Imagen -->
            <div
              *ngIf="isImage(previewUseCase.selectedDoc())"
              class="flex h-full w-full items-center justify-center overflow-auto bg-black/40 p-4">
              <img
                [src]="previewUseCase.rawUrl()"
                [alt]="previewUseCase.selectedDoc()?.fileName"
                class="max-h-full max-w-full rounded-lg object-contain shadow-lg" />
            </div>

            <!-- Visor para otros formatos no previsualizables en navegador -->
            <div
              *ngIf="!isPdf(previewUseCase.selectedDoc()) && !isImage(previewUseCase.selectedDoc())"
              class="flex h-full w-full flex-col items-center justify-center gap-4 text-center p-6">
              <div class="flex h-16 w-16 items-center justify-center rounded-2xl bg-primary/10 text-primary">
                <lucide-icon name="file-text" [size]="32"></lucide-icon>
              </div>
              <div class="space-y-1">
                <h4 class="text-base font-bold text-accent-foreground">Vista previa no disponible para este formato</h4>
                <p class="text-xs text-muted-foreground">Puedes descargar el archivo a tu dispositivo para visualizarlo.</p>
              </div>
              <button
                type="button"
                (click)="previewUseCase.downloadCurrent()"
                class="inline-flex cursor-pointer items-center gap-2 rounded-full bg-primary px-4 py-2 text-xs font-semibold text-primary-foreground hover:bg-primary/90 transition-colors">
                <lucide-icon name="download" [size]="14"></lucide-icon>
                Descargar archivo
              </button>
            </div>

          </div>

        </div>
      </div>
    </div>
  `
})
export class DocumentPreviewModalComponent {
  readonly previewUseCase = inject(EmployeeDocumentPreviewUseCase);

  showMetadata = true;

  isPdf(doc?: EmployeeDocumentResponse | null): boolean {
    if (!doc) return false;
    const mime = (doc.mimeType || '').toLowerCase();
    const name = (doc.fileName || doc.title || '').toLowerCase();
    return mime.includes('pdf') || name.endsWith('.pdf');
  }

  isImage(doc?: EmployeeDocumentResponse | null): boolean {
    if (!doc) return false;
    const mime = (doc.mimeType || '').toLowerCase();
    const name = (doc.fileName || doc.title || '').toLowerCase();
    return (
      mime.includes('image') ||
      mime.includes('png') ||
      mime.includes('jpeg') ||
      name.endsWith('.png') ||
      name.endsWith('.jpg') ||
      name.endsWith('.jpeg') ||
      name.endsWith('.webp')
    );
  }

  formatBytes(bytes: number): string {
    if (!bytes || bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  getExpirationBadge(expDate?: string | null): { label: string; class: string } {
    if (!expDate) {
      return { label: 'Sin caducidad', class: 'bg-muted text-muted-foreground border-border' };
    }
    const diff = (new Date(expDate).getTime() - Date.now()) / (1000 * 60 * 60 * 24);
    if (diff < 0) {
      return { label: 'Caducado', class: 'bg-rose-500/10 text-rose-600 border-rose-500/20' };
    }
    if (diff <= 60) {
      return { label: 'Por vencer', class: 'bg-amber-500/10 text-amber-600 border-amber-500/20' };
    }
    return { label: 'Vigente', class: 'bg-emerald-500/10 text-emerald-600 border-emerald-500/20' };
  }

  getVerificationBadge(status?: string | null): { label: string; class: string; icon: string } {
    if (status === 'VERIFIED') {
      return { label: 'Verificado', class: 'bg-emerald-500/10 text-emerald-600 border-emerald-500/20', icon: 'check-circle' };
    }
    if (status === 'REJECTED') {
      return { label: 'Rechazado', class: 'bg-rose-500/10 text-rose-600 border-rose-500/20', icon: 'circle-x' };
    }
    return { label: 'Pendiente', class: 'bg-amber-500/10 text-amber-600 border-amber-500/20', icon: 'clock' };
  }
}
