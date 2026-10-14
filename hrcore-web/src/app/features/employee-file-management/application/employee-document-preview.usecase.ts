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
      // Resolver la versión física más reciente y descargarla desde el endpoint canónico.
      if (doc.employeeFileId && doc.id) {
        let versionId = doc.latestVersionId;
        if (!versionId) {
          const versions = await firstValueFrom(
            this.fileService.listVersions(doc.employeeFileId, doc.id, 0, 1)
          );
          const latest = versions.data?.content?.[0];
          if (!latest) throw new Error('El documento no tiene una versión física disponible.');
          versionId = latest.id;
          this.selectedDoc.set({
            ...doc,
            latestVersionId: latest.id,
            versionNumber: latest.versionNumber,
            fileName: latest.originalFilename,
            mimeType: latest.mimeType,
            fileSizeBytes: latest.fileSize
          });
        }
        const blob = await firstValueFrom(
          this.fileService.downloadVersion(doc.employeeFileId, doc.id, versionId)
        );
        if (!blob || blob.size === 0) throw new Error('El archivo descargado está vacío.');
        this.createdBlobUrl = URL.createObjectURL(blob);
        this.rawUrl.set(this.createdBlobUrl);
        this.previewUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(this.createdBlobUrl));
        return;
      }
      throw new Error('No se pudo identificar el expediente del documento.');
    } catch (err: unknown) {
      const errorResponse = err as { error?: { message?: string } };
      const msg = errorResponse?.error?.message || (err as Error)?.message || 'No se pudo generar la vista previa del documento';
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
