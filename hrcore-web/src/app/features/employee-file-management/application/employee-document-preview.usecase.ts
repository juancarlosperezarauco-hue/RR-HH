import { inject, Injectable, signal } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { firstValueFrom } from 'rxjs';
import { EmployeeFileService, EmployeeDocumentResponse } from '../../../entities/employee-file';
import { ToastService } from '../../../shared/ui/toast/toast.service';

@Injectable({ providedIn: 'root' })
export class EmployeeDocumentPreviewUseCase {
  private readonly fileService = inject(EmployeeFileService);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly toast = inject(ToastService);

  readonly selectedDoc = signal<EmployeeDocumentResponse | null>(null);
  readonly previewUrl = signal<SafeResourceUrl | null>(null);
  readonly rawUrl = signal<string | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  private createdBlobUrl: string | null = null;

  async openPreview(doc: EmployeeDocumentResponse): Promise<void> {
    this.cleanupBlobUrl();
    this.selectedDoc.set(doc);
    this.loading.set(true);
    this.error.set(null);

    try {
      // 1. Intentar descarga binaria nativa (StreamingResponseBody Blob) si tenemos IDs
      if (doc.employeeFileId && doc.id) {
        try {
          const blob = await firstValueFrom(
            this.fileService.downloadVersion(doc.employeeFileId, doc.id, 'latest')
          );
          if (blob && blob.size > 0) {
            this.createdBlobUrl = URL.createObjectURL(blob);
            this.rawUrl.set(this.createdBlobUrl);
            this.previewUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(this.createdBlobUrl));
            return;
          }
        } catch {
          // Si el endpoint binario no está disponible, intentar con URL firmada/legada
        }
      }

      // 2. Fallback a URL firmada o URL simulada en mocks
      if (doc.downloadUrl) {
        this.rawUrl.set(doc.downloadUrl);
        this.previewUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(doc.downloadUrl));
        return;
      }

      const res = await firstValueFrom(this.fileService.getPreviewUrl(doc.id));
      const url = res.data.downloadUrl;
      this.rawUrl.set(url);
      this.previewUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(url));
    } catch (err: unknown) {
      const errorResponse = err as { error?: { message?: string } };
      const msg = errorResponse?.error?.message || 'No se pudo generar la vista previa del documento';
      this.error.set(msg);
      this.toast.error(msg);
    } finally {
      this.loading.set(false);
    }
  }

  private cleanupBlobUrl(): void {
    if (this.createdBlobUrl) {
      URL.revokeObjectURL(this.createdBlobUrl);
      this.createdBlobUrl = null;
    }
  }

  closePreview(): void {
    this.cleanupBlobUrl();
    this.selectedDoc.set(null);
    this.previewUrl.set(null);
    this.rawUrl.set(null);
    this.error.set(null);
  }
}
