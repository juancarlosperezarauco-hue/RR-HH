import {
  DocumentTypeResponse,
  EmployeeDocumentResponse,
  EmployeeDocumentAuditResponse
} from '../../../../entities/employee-file';

export const MOCK_DOSSIERS: {
  employeeFiles: any[];
  documentTypes: DocumentTypeResponse[];
  employeeDocuments: EmployeeDocumentResponse[];
  documentAudits: EmployeeDocumentAuditResponse[];
} = {
  employeeFiles: [
    {
      id: 'ef-9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
      employeeId: '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
      fileNumber: 'EXP-EMP-00104',
      status: 'ACTIVE',
      notes: 'Expediente digital principal de Carlos Mamani Quispe',
      createdAt: '2026-01-15T08:00:00Z',
      updatedAt: '2026-01-15T08:00:00Z'
    },
    {
      id: 'ef-8a2ceb3c-4a6c-4cbe-8acc-1a0c6a2cba5c',
      employeeId: '8a2ceb3c-4a6c-4cbe-8acc-1a0c6a2cba5c',
      fileNumber: 'EXP-EMP-00105',
      status: 'ACTIVE',
      notes: 'Expediente digital de Valeria Rios Salazar',
      createdAt: '2026-02-01T08:00:00Z',
      updatedAt: '2026-02-01T08:00:00Z'
    },
    {
      id: 'ef-7f3bda2b-5b5b-4dbd-9ebb-3b1d8a4dcb7e',
      employeeId: '7f3bda2b-5b5b-4dbd-9ebb-3b1d8a4dcb7e',
      fileNumber: 'EXP-EMP-00106',
      status: 'ACTIVE',
      notes: 'Expediente digital de Rodrigo Flores Condori',
      createdAt: '2026-02-15T08:00:00Z',
      updatedAt: '2026-02-15T08:00:00Z'
    }
  ],
  documentTypes: [
    {
      id: '11111111-1111-1111-1111-111111111101',
      tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
      name: 'Cédula de Identidad',
      code: 'DOC_CI',
      category: 'IDENTIFICATION' as const,
      isMandatory: true,
      hasExpiration: true,
      status: 'ACTIVE' as const
    },
    {
      id: '11111111-1111-1111-1111-111111111102',
      tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
      name: 'Contrato Indefinido de Trabajo',
      code: 'DOC_CONTRACT_INDEF',
      category: 'CONTRACT' as const,
      isMandatory: true,
      hasExpiration: false,
      status: 'ACTIVE' as const
    },
    {
      id: '11111111-1111-1111-1111-111111111103',
      tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
      name: 'Título Académico / Certificado de Estudios',
      code: 'DOC_DEGREE',
      category: 'EDUCATION' as const,
      isMandatory: false,
      hasExpiration: false,
      status: 'ACTIVE' as const
    },
    {
      id: '11111111-1111-1111-1111-111111111104',
      tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
      name: 'Certificado Médico Prenupcial / Ocupacional',
      code: 'DOC_MED_CERT',
      category: 'HEALTH' as const,
      isMandatory: true,
      hasExpiration: true,
      status: 'ACTIVE' as const
    },
    {
      id: '11111111-1111-1111-1111-111111111105',
      tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
      name: 'Memorando de Felicitación / Desempeño',
      code: 'DOC_MEMO_RECOG',
      category: 'DISCIPLINARY' as const,
      isMandatory: false,
      hasExpiration: false,
      status: 'ACTIVE' as const
    },
    {
      id: '11111111-1111-1111-1111-111111111106',
      tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
      name: 'Acta de Entrega de Equipos (Laptop/Token)',
      code: 'DOC_ASSET_HANDOVER',
      category: 'ASSET' as const,
      isMandatory: true,
      hasExpiration: false,
      status: 'ACTIVE' as const
    }
  ],
  employeeDocuments: [
    {
      id: 'aa11bb22-33cc-44dd-55ee-66ff77889901',
      employeeFileId: 'ef-9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
      tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
      employeeId: '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
      documentTypeId: '11111111-1111-1111-1111-111111111101',
      documentType: 'IDENTITY' as const,
      documentTypeName: 'Cédula de Identidad',
      documentCategory: 'IDENTIFICATION' as const,
      title: 'Cédula de Identidad',
      documentNumber: '7483920-LP',
      fileName: 'CI_Carlos_Mamani_2026.pdf',
      filePath: 'tenants/e2a1b3c4/employees/9b1deb4d/documents/CI_Carlos_Mamani_2026.pdf',
      fileSizeBytes: 1048576,
      mimeType: 'application/pdf',
      versionNumber: 1,
      issueDate: '2020-05-14',
      expirationDate: '2030-05-14',
      confidential: false,
      verificationStatus: 'VERIFIED' as const,
      status: 'ACTIVE' as const,
      verifiedBy: '550e8400-e29b-41d4-a716-446655440000',
      verifiedAt: '2026-02-01T10:30:00Z',
      createdAt: '2026-02-01T09:15:00Z',
      updatedAt: '2026-02-01T10:30:00Z',
      downloadUrl: 'https://raw.githubusercontent.com/mozilla/pdf.js/ba2edeae/web/compressed.tracemonkey-pldi-09.pdf'
    },
    {
      id: 'aa11bb22-33cc-44dd-55ee-66ff77889902',
      employeeFileId: 'ef-9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
      tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
      employeeId: '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
      documentTypeId: '11111111-1111-1111-1111-111111111102',
      documentType: 'CONTRACT' as const,
      documentTypeName: 'Contrato Indefinido de Trabajo',
      documentCategory: 'CONTRACT' as const,
      title: 'Contrato Indefinido de Trabajo',
      documentNumber: 'CTR-2026-0042',
      fileName: 'Contrato_Laboral_Carlos_Mamani.pdf',
      filePath: 'tenants/e2a1b3c4/employees/9b1deb4d/documents/Contrato_Laboral_Carlos_Mamani.pdf',
      fileSizeBytes: 2516582,
      mimeType: 'application/pdf',
      versionNumber: 1,
      issueDate: '2026-02-01',
      expirationDate: null,
      confidential: false,
      verificationStatus: 'VERIFIED' as const,
      status: 'ACTIVE' as const,
      verifiedBy: '550e8400-e29b-41d4-a716-446655440000',
      verifiedAt: '2026-02-01T14:20:00Z',
      createdAt: '2026-02-01T11:00:00Z',
      updatedAt: '2026-02-01T14:20:00Z',
      downloadUrl: 'https://raw.githubusercontent.com/mozilla/pdf.js/ba2edeae/web/compressed.tracemonkey-pldi-09.pdf'
    },
    {
      id: 'aa11bb22-33cc-44dd-55ee-66ff77889903',
      employeeFileId: 'ef-9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
      tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
      employeeId: '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
      documentTypeId: '11111111-1111-1111-1111-111111111104',
      documentType: 'MEDICAL' as const,
      documentTypeName: 'Certificado Médico Prenupcial / Ocupacional',
      documentCategory: 'HEALTH' as const,
      title: 'Certificado Médico Prenupcial / Ocupacional',
      documentNumber: 'MED-CNS-2026',
      fileName: 'Examen_Preocupacional_CNS.pdf',
      filePath: 'tenants/e2a1b3c4/employees/9b1deb4d/documents/Examen_Preocupacional_CNS.pdf',
      fileSizeBytes: 1845120,
      mimeType: 'application/pdf',
      versionNumber: 1,
      issueDate: '2026-01-20',
      expirationDate: '2027-01-20',
      confidential: true,
      verificationStatus: 'PENDING' as const,
      status: 'ACTIVE' as const,
      verifiedBy: '550e8400-e29b-41d4-a716-446655440000',
      verifiedAt: '2026-02-02T08:45:00Z',
      createdAt: '2026-02-02T08:30:00Z',
      updatedAt: '2026-02-02T08:45:00Z',
      downloadUrl: 'https://raw.githubusercontent.com/mozilla/pdf.js/ba2edeae/web/compressed.tracemonkey-pldi-09.pdf'
    }
  ],
  documentAudits: [
    {
      id: 'aud-001-aa11bb22',
      documentId: 'aa11bb22-33cc-44dd-55ee-66ff77889901',
      userId: '550e8400-e29b-41d4-a716-446655440000',
      userEmail: 'rrhh@hrcore.com',
      action: 'UPLOAD' as const,
      performedAt: '2026-02-01T09:15:00Z',
      ipAddress: '190.181.12.45'
    },
    {
      id: 'aud-002-aa11bb22',
      documentId: 'aa11bb22-33cc-44dd-55ee-66ff77889901',
      userId: '550e8400-e29b-41d4-a716-446655440000',
      userEmail: 'rrhh@hrcore.com',
      action: 'VIEW' as const,
      performedAt: '2026-02-01T10:28:00Z',
      ipAddress: '190.181.12.45'
    },
    {
      id: 'aud-003-aa11bb22',
      documentId: 'aa11bb22-33cc-44dd-55ee-66ff77889903',
      userId: '550e8400-e29b-41d4-a716-446655440000',
      userEmail: 'rrhh@hrcore.com',
      action: 'DOWNLOAD' as const,
      performedAt: '2026-02-05T16:00:00Z',
      ipAddress: '190.181.12.45'
    }
  ]
};
