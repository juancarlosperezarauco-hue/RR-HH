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
    'image/jpg'
  ];
  readonly MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB

  validateFile(file: File): { isValid: boolean; message?: string } {
    const fileName = file.name || '';
    const ext = fileName.includes('.') ? fileName.substring(fileName.lastIndexOf('.')).toLowerCase() : '';
    const allowedExtensions = ['.pdf', '.jpg', '.jpeg', '.png'];
    const isMimeValid = this.ALLOWED_MIME_TYPES.includes(file.type);
    const isExtValid = allowedExtensions.includes(ext);

    if (!isMimeValid && !isExtValid) {
      return {
        isValid: false,
        message: 'Formato no soportado. Se aceptan únicamente documentos PDF o imágenes JPG y PNG.'
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
   * Resuelve el expediente (EmployeeFile) del colaborador
   */
  private async ensureEmployeeFileId(employeeId?: string, fileId?: string): Promise<string> {
    if (fileId) {
      return fileId;
    }
    if (!employeeId) {
      throw new Error('Seleccione un empleado antes de adjuntar documentos.');
    }

    try {
      const res = await firstValueFrom(this.fileService.getFileByEmployee(employeeId));
      if (res?.data?.id) return res.data.id;
    } catch {}

    throw new Error('El colaborador seleccionado no cuenta con un expediente digital aperturado. Apertúrelo antes de adjuntar documentos.');
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
   * Flujo canónico Spring Boot en 2 pasos:
   * Paso 1: POST /api/employee-files/{id}/documents (metadatos)
   * Paso 2: POST /api/employee-files/{id}/documents/{docId}/versions (archivo binario multipart)
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

    let fileId: string | null = null;
    let createdDocId: string | null = null;
    let metadataPayload: CreateDocumentRequest | null = null;

    try {
      // 1. Obtener expediente padre canónico
      fileId = await this.ensureEmployeeFileId(request.employeeId, request.fileId);

      // 2. Paso 1: Registrar metadatos
      const docType = request.documentType || this.mapToDocumentType(request.documentTypeId);
      metadataPayload = {
        contractId: null,
        documentType: docType,
        title: request.title || request.file.name.replace(/\.[^/.]+$/, ''),
        description: 'Cargado desde la plataforma digital',
        documentNumber: request.documentNumber || null,
        issuedAt: request.issueDate || null,
        expiresAt: request.expirationDate || null,
        confidential: !!request.confidential
      };

      const docRes = await firstValueFrom(
        this.fileService.createDocumentMetadata(fileId, metadataPayload)
      );

      createdDocId = docRes?.data?.id || null;
      // 3. Paso 2: Subir archivo físico como versión (MultipartFile)
      if (!createdDocId) {
        throw new Error('No se pudo registrar los metadatos del documento en el backend.');
      }

      await firstValueFrom(
        this.fileService.uploadVersion(fileId, createdDocId, request.file)
      );

      this.toast.success('Documento legal y versión física registrados con éxito en el backend.');
      return true;
    } catch (err: unknown) {
      // Compensación / Rollback: Si se registraron metadatos pero falló la carga binaria (ej: formato no válido en backend),
      // se archiva inmediatamente el registro para no dejar documentos huérfanos o vacíos en el expediente.
      if (fileId && createdDocId && metadataPayload) {
        try {
          await firstValueFrom(
            this.fileService.updateDocumentMetadata(fileId, createdDocId, {
              contractId: metadataPayload.contractId,
              documentType: metadataPayload.documentType,
              title: metadataPayload.title,
              description: 'Carga fallida - archivado automáticamente',
              documentNumber: metadataPayload.documentNumber,
              issuedAt: metadataPayload.issuedAt,
              expiresAt: metadataPayload.expiresAt,
              confidential: metadataPayload.confidential,
              status: 'ARCHIVED'
            })
          );
        } catch (rollbackErr) {
          console.warn('No se pudo compensar el documento huérfano:', rollbackErr);
        }
      }

      const errorResponse = err as { error?: { message?: string } };
      const msg = errorResponse?.error?.message || (err as any)?.message || 'Error al subir el documento';
      this.error.set(msg);
      this.toast.error(msg);
      return false;
    } finally {
      this.uploading.set(false);
    }
  }
}
