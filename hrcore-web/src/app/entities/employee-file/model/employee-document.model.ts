import { DocumentCategory, DocumentType } from './document-type.model';
import { FileStatus } from './employee-file.model';

export type VerificationStatus = 'PENDING' | 'VERIFIED' | 'REJECTED';
export type DocumentStatus = 'ACTIVE' | 'EXPIRED' | 'ARCHIVED';

/**
 * DTO Canónico estricto de Spring Boot: DocumentResponse
 * com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.application.dto.DocumentResponse
 */
export interface DocumentResponse {
  id: string;                         // UUID del documento
  employeeFileId: string;             // UUID del expediente
  contractId?: string | null;         // UUID del contrato opcional
  documentType: DocumentType;         // Enum oficial (IDENTITY, CONTRACT, etc.)
  title: string;                      // Título (VARCHAR 200)
  description?: string | null;        // Descripción libre
  documentNumber?: string | null;     // Número de documento (VARCHAR 100)
  issuedAt?: string | null;           // Fecha de emisión (YYYY-MM-DD)
  expiresAt?: string | null;          // Fecha de vencimiento (YYYY-MM-DD)
  confidential: boolean;              // Requiere permiso employee_files.documents.confidential.read
  verificationStatus: VerificationStatus; // PENDING | VERIFIED | REJECTED
  verifiedBy?: string | null;         // UUID del usuario verificador
  verifiedAt?: string | null;         // Timestamp ISO
  status: FileStatus;                 // ACTIVE | ARCHIVED
  createdAt: string;                  // Timestamp ISO
  updatedAt: string;                  // Timestamp ISO
}

/**
 * Modelo de Documento de Empleado con compatibilidad dual (Spring Boot + UI/Mocks)
 */
export interface EmployeeDocumentResponse {
  id: string;                         // UUID del documento
  employeeFileId?: string;            // UUID del expediente
  contractId?: string | null;         // UUID del contrato opcional
  documentType?: DocumentType;        // Enum oficial (IDENTITY, CONTRACT, etc.)
  title?: string;                     // Título (VARCHAR 200)
  description?: string | null;        // Descripción libre
  documentNumber?: string | null;     // Número de documento (VARCHAR 100)
  issuedAt?: string | null;           // Fecha de emisión (YYYY-MM-DD)
  expiresAt?: string | null;          // Fecha de vencimiento (YYYY-MM-DD)
  confidential?: boolean;             // Requiere permiso employee_files.documents.confidential.read
  verificationStatus?: VerificationStatus; // PENDING | VERIFIED | REJECTED
  verifiedBy?: string | null;         // UUID del usuario verificador
  verifiedAt?: string | null;         // Timestamp ISO
  status: FileStatus | DocumentStatus;// ACTIVE | ARCHIVED
  createdAt: string;                  // Timestamp ISO
  updatedAt: string;                  // Timestamp ISO

  // ----------------------------------------------------
  // Propiedades requeridas para la UI actual y Mocks
  // ----------------------------------------------------
  tenantId?: string;
  employeeId?: string;
  documentTypeId: string;
  documentTypeName: string;
  documentCategory: DocumentCategory;
  fileName: string;
  filePath: string;
  fileSizeBytes: number;
  mimeType: string;
  versionNumber: number;
  issueDate?: string | null;
  expirationDate?: string | null;
  downloadUrl?: string;
}

/**
 * Paso 1 del flujo oficial: Registro de Metadatos
 * POST /api/employee-files/{id}/documents
 */
export interface CreateDocumentRequest {
  contractId?: string | null;
  documentType: DocumentType;
  title: string;
  description?: string | null;
  documentNumber?: string | null;
  issuedAt?: string | null;
  expiresAt?: string | null;
  confidential: boolean;
}

/**
 * Actualización de Metadatos
 * PUT /api/employee-files/{id}/documents/{documentId}
 */
export interface UpdateDocumentRequest {
  contractId?: string | null;
  documentType: DocumentType;
  title: string;
  description?: string | null;
  documentNumber?: string | null;
  issuedAt?: string | null;
  expiresAt?: string | null;
  confidential: boolean;
  status: FileStatus;
}

/**
 * Verificación / Rechazo de Documento
 * PATCH /api/employee-files/{id}/documents/{documentId}/verify
 */
export interface VerificationRequest {
  status: VerificationStatus;
}

/**
 * Paso 2 del flujo oficial: Versión física del archivo
 * POST /api/employee-files/{id}/documents/{documentId}/versions
 */
export interface DocumentVersionResponse {
  id: string;               // UUID
  documentId: string;       // UUID
  versionNumber: number;    // 1, 2, 3...
  originalFilename: string;
  mimeType: string;
  fileSize: number;         // bytes
  checksumSha256: string;
  createdAt: string;
}

// Compatibilidad transicional
export interface UploadEmployeeDocumentRequest {
  employeeId: string;
  documentTypeId: string;
  documentType?: DocumentType;
  title?: string;
  documentNumber?: string;
  issueDate?: string;
  expirationDate?: string;
  confidential?: boolean;
  file: File;
}

export interface SignedUrlResponse {
  downloadUrl: string;
  expiresInMinutes: number;
}
