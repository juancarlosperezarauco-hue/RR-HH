/**
 * Enum oficial de backend Spring Boot:
 * com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.model.DocumentType
 */
export type DocumentType =
  | 'IDENTITY'
  | 'CONTRACT'
  | 'CERTIFICATE'
  | 'EDUCATION'
  | 'MEDICAL'
  | 'DISCIPLINARY'
  | 'PAYROLL'
  | 'OTHER';

export const DOCUMENT_TYPE_LABELS: Record<DocumentType, string> = {
  IDENTITY: 'Documento de Identidad',
  CONTRACT: 'Contrato Laboral',
  CERTIFICATE: 'Certificado Laboral / Capacitación',
  EDUCATION: 'Título / Diploma Académico',
  MEDICAL: 'Certificado Médico / Baja de Salud',
  DISCIPLINARY: 'Memorando / Sanción Disciplinaria',
  PAYROLL: 'Boleta de Pago / Liquidación',
  OTHER: 'Otros Documentos'
};

// ========================================================
// Tipos legados para compatibilidad transicional con UI/mocks
// ========================================================
export type DocumentCategory = 
  | 'IDENTIFICATION' // CI, Pasaporte, Libreta militar
  | 'CONTRACT'       // Contratos, adendas, confidencialidad
  | 'EDUCATION'      // Títulos académicos, certificaciones
  | 'HEALTH'         // Exámenes pre-ocupacionales, bajas médicas (SENSIBLE)
  | 'DISCIPLINARY'   // Memorandos, reconocimientos
  | 'ASSET';         // Actas de entrega de equipos / tokens

export interface DocumentTypeResponse {
  id: string;              // UUID
  tenantId: string;        // UUID
  name: string;            // VARCHAR(100)
  code: string;            // VARCHAR(50)
  category: DocumentCategory;
  isMandatory: boolean;
  hasExpiration: boolean;
  status: 'ACTIVE' | 'INACTIVE';
}
