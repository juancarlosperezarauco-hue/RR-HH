import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule } from 'lucide-angular';
import { EmployeeDocumentPreviewUseCase } from '../../application/employee-document-preview.usecase';

@Component({
  selector: 'app-document-preview-modal',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  template: `
    <div
      *ngIf="previewUseCase.selectedDoc()"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm">
      <div
        class="relative flex h-[90vh] w-full max-w-5xl flex-col rounded-[24px] border border-border bg-card shadow-2xl overflow-hidden">
        
        <!-- Header del Modal -->
        <div class="flex items-center justify-between border-b border-border bg-muted/40 px-6 py-4">
          <div class="flex items-center gap-3 min-w-0">
            <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary">
              <lucide-icon name="file-text" [size]="20"></lucide-icon>
            </div>
            <div class="min-w-0">
              <h3 class="truncate text-base font-bold text-accent-foreground">
                {{ previewUseCase.selectedDoc()?.fileName }}
              </h3>
              <p class="text-xs text-muted-foreground">
                {{ previewUseCase.selectedDoc()?.documentTypeName }} • 
                {{ formatBytes(previewUseCase.selectedDoc()?.fileSizeBytes || 0) }}
              </p>
            </div>
          </div>

          <div class="flex items-center gap-2">
            <!-- Botón Descargar -->
            <a
              *ngIf="previewUseCase.rawUrl()"
              [href]="previewUseCase.rawUrl()"
              target="_blank"
              download
              class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-border bg-card px-3.5 py-1.5 text-xs font-semibold text-primary transition-colors hover:bg-accent">
              <lucide-icon name="download" [size]="14"></lucide-icon>
              Descargar
            </a>

            <!-- Botón Cerrar -->
            <button
              type="button"
              (click)="previewUseCase.closePreview()"
              class="inline-flex h-8 w-8 cursor-pointer items-center justify-center rounded-full border border-border bg-card text-muted-foreground transition-colors hover:bg-destructive/10 hover:text-destructive">
              <lucide-icon name="x" [size]="16"></lucide-icon>
            </button>
          </div>
        </div>

        <!-- Contenedor del Visor -->
        <div class="relative flex-1 bg-muted/20 p-4 overflow-hidden">
          <!-- Spinner de Carga -->
          <div
            *ngIf="previewUseCase.loading()"
            class="absolute inset-0 flex flex-col items-center justify-center gap-3 bg-card/80">
            <lucide-icon name="loader-circle" [size]="32" class="animate-spin text-primary"></lucide-icon>
            <p class="text-sm font-medium text-muted-foreground">Cargando vista previa segura...</p>
          </div>

          <!-- Error al cargar -->
          <div
            *ngIf="previewUseCase.error()"
            class="flex h-full flex-col items-center justify-center gap-3 text-center p-6">
            <lucide-icon name="circle-alert" [size]="40" class="text-destructive"></lucide-icon>
            <p class="text-sm font-semibold text-destructive">{{ previewUseCase.error() }}</p>
            <button
              type="button"
              (click)="previewUseCase.closePreview()"
              class="rounded-full border border-border bg-card px-4 py-2 text-xs font-semibold text-primary hover:bg-accent">
              Cerrar
            </button>
          </div>

          <!-- Visor PDF embebido -->
          <iframe
            *ngIf="!previewUseCase.loading() && !previewUseCase.error() && isPdf(previewUseCase.selectedDoc()?.mimeType)"
            [src]="previewUseCase.previewUrl()"
            class="h-full w-full rounded-xl border border-border bg-card shadow-inner"
            title="Vista previa del documento">
          </iframe>

          <!-- Visor de Imagen -->
          <div
            *ngIf="!previewUseCase.loading() && !previewUseCase.error() && isImage(previewUseCase.selectedDoc()?.mimeType)"
            class="flex h-full w-full items-center justify-center overflow-auto rounded-xl border border-border bg-black/40 p-4">
            <img
              [src]="previewUseCase.rawUrl()"
              [alt]="previewUseCase.selectedDoc()?.fileName"
              class="max-h-full max-w-full rounded-lg object-contain shadow-lg" />
          </div>
        </div>
      </div>
    </div>
  `
})
export class DocumentPreviewModalComponent {
  readonly previewUseCase = inject(EmployeeDocumentPreviewUseCase);

  isPdf(mime?: string): boolean {
    return !!mime && mime.includes('pdf');
  }

  isImage(mime?: string): boolean {
    return !!mime && (mime.includes('image') || mime.includes('png') || mime.includes('jpeg'));
  }

  formatBytes(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }
}
