import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { EmployeeFileService, UploadEmployeeDocumentRequest } from '../../../entities/employee-file';
import { ToastService } from '../../../shared/ui/toast/toast.service';

@Injectable({ providedIn: 'root' })
export class EmployeeDocumentUploadUseCase {
  private readonly fileService = inject(EmployeeFileService);
  private readonly toast = inject(ToastService);

  readonly uploading = signal(false);
  readonly error = signal<string | null>(null);

  readonly ALLOWED_MIME_TYPES = [
    'application/pdf',
    'image/jpeg',
    'image/png',
    'image/jpg',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/msword'
  ];
  readonly MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB

  validateFile(file: File): { isValid: boolean; message?: string } {
    const fileName = file.name || '';
    const ext = fileName.includes('.') ? fileName.substring(fileName.lastIndexOf('.')).toLowerCase() : '';
    const allowedExtensions = ['.pdf', '.jpg', '.jpeg', '.png', '.docx', '.doc'];
    const isMimeValid = this.ALLOWED_MIME_TYPES.includes(file.type);
    const isExtValid = allowedExtensions.includes(ext);

    if (!isMimeValid && !isExtValid) {
      return {
        isValid: false,
        message: 'Formato no soportado. Se aceptan únicamente PDF, JPG, PNG o DOCX.'
      };
    }
    if (file.size > this.MAX_FILE_SIZE_BYTES) {
      return {
        isValid: false,
        message: 'El archivo excede el tamaño máximo permitido de 10 MB.'
      };
    }
    return { isValid: true };
  }

  async upload(request: UploadEmployeeDocumentRequest): Promise<boolean> {
    const val = this.validateFile(request.file);
    if (!val.isValid) {
      this.error.set(val.message!);
      this.toast.error(val.message!);
      return false;
    }

    this.uploading.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(this.fileService.uploadDocument(request));
      this.toast.success('Documento subido y clasificado correctamente');
      return true;
    } catch (err: any) {
      const msg = err?.error?.message || 'Error al subir el documento';
      this.error.set(msg);
      this.toast.error(msg);
      return false;
    } finally {
      this.uploading.set(false);
    }
  }
}
