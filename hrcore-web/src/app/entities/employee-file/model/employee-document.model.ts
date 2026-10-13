import { DocumentCategory } from './document-type.model';

export type DocumentStatus = 'ACTIVE' | 'EXPIRED' | 'ARCHIVED';

export interface EmployeeDocumentResponse {
  id: string;              // UUID
  tenantId: string;        // UUID
  employeeId: string;      // UUID
  documentTypeId: string;  // UUID
  documentTypeName: string;
  documentCategory: DocumentCategory;
  documentNumber?: string | null;
  fileName: string;
  filePath: string;
  fileSizeBytes: number;   // BIGINT (máx 10 MB)
  mimeType: string;        // 'application/pdf' | 'image/jpeg' | 'image/png' | ...
  versionNumber: number;
  issueDate?: string | null;      // YYYY-MM-DD
  expirationDate?: string | null; // YYYY-MM-DD
  status: DocumentStatus;
  verifiedBy?: string | null;     // UUID
  verifiedAt?: string | null;     // ISO Timestamp
  createdAt: string;              // ISO Timestamp
  updatedAt: string;              // ISO Timestamp
  downloadUrl?: string;           // URL firmada temporal para preview o descarga
}

export interface UploadEmployeeDocumentRequest {
  employeeId: string;
  documentTypeId: string;
  documentNumber?: string;
  issueDate?: string;
  expirationDate?: string;
  file: File;
}

export interface SignedUrlResponse {
  downloadUrl: string;
  expiresInMinutes: number;
}
