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

export interface TenantAuditEventResponse {
  id: string;
  actorId?: string | null;
  actorEmail?: string | null;
  eventType: string;
  resourceType: string;
  resourceId: string;
  ipAddress?: string | null;
  createdAt: string;
}
