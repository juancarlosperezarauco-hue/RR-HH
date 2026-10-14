export type DocumentAuditAction = 'UPLOAD' | 'VIEW' | 'DOWNLOAD' | 'UPDATE' | 'ARCHIVE';

export interface EmployeeDocumentAuditResponse {
  id: string;          // UUID
  documentId: string;  // UUID
  userId: string;      // UUID
  userEmail?: string;
  action: DocumentAuditAction;
  performedAt: string; // ISO Timestamp
  ipAddress?: string;
}
