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

  async openPreview(doc: EmployeeDocumentResponse): Promise<void> {
    this.selectedDoc.set(doc);
    this.loading.set(true);
    this.error.set(null);
    try {
      const res = await firstValueFrom(this.fileService.getPreviewUrl(doc.id));
      const url = res.data.downloadUrl;
      this.rawUrl.set(url);
      this.previewUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(url));
    } catch (err: any) {
      const msg = err?.error?.message || 'No se pudo generar la URL de previsualización';
      this.error.set(msg);
      this.toast.error(msg);
    } finally {
      this.loading.set(false);
    }
  }

  closePreview(): void {
    this.selectedDoc.set(null);
    this.previewUrl.set(null);
    this.rawUrl.set(null);
    this.error.set(null);
  }
}
