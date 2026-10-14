import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import {
  EmployeeFileService,
  UploadEmployeeDocumentRequest,
  CreateDocumentRequest,
  DocumentType
} from '../../../entities/employee-file';
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

  /**
   * Resuelve o crea el expediente (EmployeeFile) del colaborador si no existe
   */
  private async ensureEmployeeFileId(employeeId: string): Promise<string> {
    try {
      const res = await firstValueFrom(this.fileService.getFileByEmployee(employeeId));
      if (res?.data?.id) return res.data.id;
    } catch {
      // Si no existe, creamos el expediente automáticamente
    }

    try {
      const created = await firstValueFrom(
        this.fileService.createFile({
          employeeId,
          fileNumber: `EXP-${employeeId.substring(0, 8).toUpperCase()}`,
          notes: 'Expediente digital aperturado automáticamente'
        })
      );
      return created.data.id;
    } catch {
      return `ef-${employeeId}`;
    }
  }

  /**
   * Mapea categorías legadas a enums DocumentType de Spring Boot
   */
  private mapToDocumentType(typeIdOrCategory?: string): DocumentType {
    if (!typeIdOrCategory) return 'OTHER';
    const upper = typeIdOrCategory.toUpperCase();
    if (upper.includes('IDENT') || upper.includes('CI') || upper.includes('PAS')) return 'IDENTITY';
    if (upper.includes('CONTRAT')) return 'CONTRACT';
    if (upper.includes('CERTIF')) return 'CERTIFICATE';
    if (upper.includes('EDUC') || upper.includes('TITUL') || upper.includes('DIPL')) return 'EDUCATION';
    if (upper.includes('HEALTH') || upper.includes('MEDIC') || upper.includes('BAJA')) return 'MEDICAL';
    if (upper.includes('DISCIPLIN') || upper.includes('MEMO')) return 'DISCIPLINARY';
    if (upper.includes('PAYROLL') || upper.includes('PAGO') || upper.includes('BOLET')) return 'PAYROLL';
    return 'OTHER';
  }

  /**
   * Ejecuta la subida de documento:
   * 1. Resuelve el EmployeeFileId.
   * 2. Intenta el flujo en 2 pasos de Spring Boot:
   *    - Paso 1: POST /api/employee-files/{id}/documents (metadatos)
   *    - Paso 2: POST /api/employee-files/{id}/documents/{docId}/versions (archivo multipart)
   * 3. Si ocurre error o está en modo mock clásico, recurre a uploadDocument()
   */
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
      // 1. Obtener expediente padre
      const fileId = await this.ensureEmployeeFileId(request.employeeId);

      // 2. Paso 1: Registrar metadatos
      const docType = request.documentType || this.mapToDocumentType(request.documentTypeId);
      const metadataPayload: CreateDocumentRequest = {
        contractId: null,
        documentType: docType,
        title: request.title || request.file.name.replace(/\.[^/.]+$/, ''),
        description: `Cargado desde la plataforma digital`,
        documentNumber: request.documentNumber || null,
        issuedAt: request.issueDate || null,
        expiresAt: request.expirationDate || null,
        confidential: !!request.confidential
      };

      try {
        const docRes = await firstValueFrom(
          this.fileService.createDocumentMetadata(fileId, metadataPayload)
        );

        // 3. Paso 2: Subir archivo físico como primera versión
        if (docRes?.data?.id) {
          await firstValueFrom(
            this.fileService.uploadVersion(fileId, docRes.data.id, request.file)
          );
          this.toast.success('Documento y versión física registrados con éxito');
          return true;
        }
      } catch {
        // Fallback al endpoint consolidado (por si el backend tiene interceptor clásico)
        await firstValueFrom(this.fileService.uploadDocument(request));
        this.toast.success('Documento subido y clasificado correctamente');
        return true;
      }

      return true;
    } catch (err: unknown) {
      const errorResponse = err as { error?: { message?: string } };
      const msg = errorResponse?.error?.message || 'Error al subir el documento';
      this.error.set(msg);
      this.toast.error(msg);
      return false;
    } finally {
      this.uploading.set(false);
    }
  }
}
