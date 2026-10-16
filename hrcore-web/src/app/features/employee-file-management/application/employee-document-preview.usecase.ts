import { inject, Injectable, signal } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { firstValueFrom } from 'rxjs';
import { EmployeeFileService, EmployeeDocumentResponse, DocumentVersionResponse } from '../../../entities/employee-file';
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

  private currentBlob: Blob | null = null;
  private createdBlobUrl: string | null = null;

  async openPreview(doc: EmployeeDocumentResponse): Promise<void> {
    this.cleanupBlobUrl();
    this.selectedDoc.set(doc);
    this.loading.set(true);
    this.error.set(null);

    try {
      const fileId = doc.employeeFileId;
      const documentId = doc.id;

      if (!fileId || !documentId) {
        throw new Error('Identificador de expediente o documento no disponible.');
      }

      // 1. Obtener la versión más reciente del documento en el backend
      let versionId = doc.activeVersionId;
      let latestVersion: DocumentVersionResponse | null = null;

      if (!versionId) {
        const versionsRes = await firstValueFrom(
          this.fileService.listVersions(fileId, documentId, 0, 1)
        );
        latestVersion = versionsRes?.data?.content?.[0] || null;
        if (latestVersion) {
          versionId = latestVersion.id;
          // Actualizar datos del documento seleccionado con los metadatos reales de la versión
          this.selectedDoc.update((prev) =>
            prev
              ? {
                  ...prev,
                  fileName: latestVersion!.originalFilename || prev.fileName,
                  fileSizeBytes: latestVersion!.fileSize || prev.fileSizeBytes,
                  mimeType: latestVersion!.mimeType || prev.mimeType,
                  versionNumber: latestVersion!.versionNumber || prev.versionNumber,
                  activeVersionId: latestVersion!.id
                }
              : null
          );
        }
      }

      if (!versionId) {
        throw new Error('El documento no cuenta con ningún archivo digital o versión adjunta.');
      }

      // 2. Descargar el archivo binario mediante StreamingResponseBody con token JWT del interceptor
      const rawBlob = await firstValueFrom(
        this.fileService.downloadVersion(fileId, documentId, versionId)
      );

      if (!rawBlob || rawBlob.size === 0) {
        throw new Error('El archivo del documento está vacío o no se pudo recuperar del almacenamiento.');
      }

      // 3. Determinar MIME Type exacto para el visor embebido (iframe / img)
      let resolvedMime =
        latestVersion?.mimeType ||
        doc.mimeType ||
        'application/pdf';

      const resolvedName = (
        latestVersion?.originalFilename ||
        doc.fileName ||
        doc.title ||
        ''
      ).toLowerCase();

      if (resolvedName.endsWith('.pdf')) {
        resolvedMime = 'application/pdf';
      } else if (resolvedName.endsWith('.png')) {
        resolvedMime = 'image/png';
      } else if (resolvedName.endsWith('.jpg') || resolvedName.endsWith('.jpeg')) {
        resolvedMime = 'image/jpeg';
      } else if (resolvedName.endsWith('.webp')) {
        resolvedMime = 'image/webp';
      }

      const typedBlob = new Blob([rawBlob], { type: resolvedMime });
      this.currentBlob = typedBlob;
      this.createdBlobUrl = URL.createObjectURL(typedBlob);
      this.rawUrl.set(this.createdBlobUrl);
      this.previewUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(this.createdBlobUrl));
    } catch (err: unknown) {
      const errorResponse = err as { error?: { message?: string } };
      const msg =
        errorResponse?.error?.message ||
        (err as any)?.message ||
        'No se pudo generar la vista previa del documento digital';
      this.error.set(msg);
      this.toast.error(msg);
    } finally {
      this.loading.set(false);
    }
  }

  /**
   * Descarga el archivo actual que ya está cargado en la vista previa
   */
  downloadCurrent(): void {
    const doc = this.selectedDoc();
    const fileName = doc?.fileName || doc?.title || 'documento.pdf';

    if (this.currentBlob) {
      this.triggerBrowserDownload(this.currentBlob, fileName);
      return;
    }

    if (doc) {
      this.downloadDocument(doc);
    }
  }

  /**
   * Descarga un documento directamente desde la tarjeta o tabla
   */
  async downloadDocument(doc: EmployeeDocumentResponse): Promise<void> {
    try {
      const fileId = doc.employeeFileId;
      const documentId = doc.id;

      if (!fileId || !documentId) {
        throw new Error('Identificadores no válidos para la descarga.');
      }

      let versionId = doc.activeVersionId;
      let fileName = doc.fileName || doc.title || 'documento.pdf';
      let mimeType = doc.mimeType || 'application/octet-stream';

      if (!versionId) {
        const vRes = await firstValueFrom(
          this.fileService.listVersions(fileId, documentId, 0, 1)
        );
        const latest = vRes?.data?.content?.[0];
        if (latest) {
          versionId = latest.id;
          fileName = latest.originalFilename || fileName;
          mimeType = latest.mimeType || mimeType;
        }
      }

      if (!versionId) {
        throw new Error('El documento no cuenta con ningún archivo disponible para descargar.');
      }

      const rawBlob = await firstValueFrom(
        this.fileService.downloadVersion(fileId, documentId, versionId)
      );

      const typedBlob = new Blob([rawBlob], { type: mimeType });
      this.triggerBrowserDownload(typedBlob, fileName);
      this.toast.success(`Descarga de "${fileName}" iniciada.`);
    } catch (err: unknown) {
      const errorResponse = err as { error?: { message?: string } };
      const msg =
        errorResponse?.error?.message ||
        (err as any)?.message ||
        'No se pudo descargar el archivo solicitado';
      this.toast.error(msg);
    }
  }

  private triggerBrowserDownload(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    document.body.appendChild(anchor);
    anchor.click();
    document.body.removeChild(anchor);
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }

  private cleanupBlobUrl(): void {
    if (this.createdBlobUrl) {
      URL.revokeObjectURL(this.createdBlobUrl);
      this.createdBlobUrl = null;
    }
    this.currentBlob = null;
  }

  closePreview(): void {
    this.cleanupBlobUrl();
    this.selectedDoc.set(null);
    this.previewUrl.set(null);
    this.rawUrl.set(null);
    this.error.set(null);
  }
}
