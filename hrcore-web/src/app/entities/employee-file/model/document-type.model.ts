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
