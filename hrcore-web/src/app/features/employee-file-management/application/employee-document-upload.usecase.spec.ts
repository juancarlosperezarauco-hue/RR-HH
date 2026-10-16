import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { EmployeeDocumentUploadUseCase } from './employee-document-upload.usecase';
import { EmployeeFileService, DocumentResponse, DocumentVersionResponse } from '../../../entities/employee-file';
import { ToastService } from '../../../shared/ui/toast/toast.service';

describe('EmployeeDocumentUploadUseCase (HU15 - Ingesta en 2 Pasos)', () => {
  let useCase: EmployeeDocumentUploadUseCase;
  let fileServiceSpy: jasmine.SpyObj<EmployeeFileService>;
  let toastSpy: jasmine.SpyObj<ToastService>;

  const mockFile = new File(['dummy-content-pdf'], 'contrato_laboral.pdf', { type: 'application/pdf' });

  const mockDocumentResponse: DocumentResponse = {
    id: 'doc-uuid-555',
    employeeFileId: 'file-uuid-111',
    contractId: null,
    documentType: 'CONTRACT',
    title: 'Contrato Indefinido',
    description: 'Cargado desde la plataforma digital',
    documentNumber: 'CTR-2026-001',
    issuedAt: '2026-01-01',
    expiresAt: null,
    confidential: false,
    verificationStatus: 'PENDING',
    status: 'ACTIVE',
    createdAt: '2026-01-15T08:00:00Z',
    updatedAt: '2026-01-15T08:00:00Z'
  };

  const mockVersionResponse: DocumentVersionResponse = {
    id: 'ver-uuid-999',
    documentId: 'doc-uuid-555',
    versionNumber: 1,
    originalFilename: 'contrato_laboral.pdf',
    mimeType: 'application/pdf',
    fileSize: 1024,
    checksumSha256: 'abc123sha256',
    createdAt: '2026-01-15T08:01:00Z'
  };

  beforeEach(() => {
    fileServiceSpy = jasmine.createSpyObj('EmployeeFileService', [
      'getFileByEmployee',
      'createDocumentMetadata',
      'uploadVersion'
    ]);
    toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error', 'warning', 'info']);

    TestBed.configureTestingModule({
      providers: [
        EmployeeDocumentUploadUseCase,
        { provide: EmployeeFileService, useValue: fileServiceSpy },
        { provide: ToastService, useValue: toastSpy }
      ]
    });

    useCase = TestBed.inject(EmployeeDocumentUploadUseCase);
  });

  it('debe validar tipos de archivo permitidos (PDF, JPG, PNG)', () => {
    const validPdf = new File(['test'], 'doc.pdf', { type: 'application/pdf' });
    expect(useCase.validateFile(validPdf).isValid).toBeTrue();

    const validJpg = new File(['test'], 'foto.jpg', { type: 'image/jpeg' });
    expect(useCase.validateFile(validJpg).isValid).toBeTrue();

    const invalidDocx = new File(['test'], 'doc.docx', { type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' });
    expect(useCase.validateFile(invalidDocx).isValid).toBeFalse();

    const invalidExe = new File(['malware'], 'setup.exe', { type: 'application/x-msdownload' });
    const result = useCase.validateFile(invalidExe);
    expect(result.isValid).toBeFalse();
    expect(result.message).toContain('Formato no soportado');
  });

  it('debe rechazar archivos que excedan el límite de 10 MB', () => {
    const hugeFile = {
      name: 'large.pdf',
      type: 'application/pdf',
      size: 11 * 1024 * 1024
    } as File;

    const result = useCase.validateFile(hugeFile);
    expect(result.isValid).toBeFalse();
    expect(result.message).toContain('10 MB');
  });

  it('debe ejecutar el flujo canónico de 2 pasos de Spring Boot con éxito', async () => {
    fileServiceSpy.getFileByEmployee.and.returnValue(of({
      success: true,
      message: 'OK',
      data: {
        id: 'file-uuid-111',
        employeeId: 'emp-uuid-001',
        fileNumber: 'EXP-001',
        status: 'ACTIVE',
        notes: '',
        createdAt: '',
        updatedAt: ''
      },
      timestamp: ''
    }));

    fileServiceSpy.createDocumentMetadata.and.returnValue(of({
      success: true,
      message: 'Document metadata created',
      data: mockDocumentResponse,
      timestamp: ''
    }));

    fileServiceSpy.uploadVersion.and.returnValue(of({
      success: true,
      message: 'Version uploaded',
      data: mockVersionResponse,
      timestamp: ''
    }));

    const result = await useCase.upload({
      employeeId: 'emp-uuid-001',
      documentTypeId: 'CONTRACT',
      documentType: 'CONTRACT',
      title: 'Contrato Indefinido',
      documentNumber: 'CTR-2026-001',
      file: mockFile
    });

    expect(result).toBeTrue();
    // Paso 1: Registro de metadatos JSON
    expect(fileServiceSpy.createDocumentMetadata).toHaveBeenCalledWith('file-uuid-111', jasmine.objectContaining({
      documentType: 'CONTRACT',
      title: 'Contrato Indefinido',
      documentNumber: 'CTR-2026-001'
    }));
    // Paso 2: Subida de archivo físico MultipartFile
    expect(fileServiceSpy.uploadVersion).toHaveBeenCalledWith('file-uuid-111', 'doc-uuid-555', mockFile);
    expect(toastSpy.success).toHaveBeenCalled();
  });

  it('debe rechazar la subida si el empleado no tiene un expediente activo', async () => {
    fileServiceSpy.getFileByEmployee.and.returnValue(throwError(() => ({ status: 404 })));

    const result = await useCase.upload({
      employeeId: 'emp-sin-expediente',
      documentTypeId: 'CONTRACT',
      file: mockFile
    });

    expect(result).toBeFalse();
    expect(fileServiceSpy.createDocumentMetadata).not.toHaveBeenCalled();
    expect(toastSpy.error).toHaveBeenCalledWith(jasmine.stringMatching(/no cuenta con un expediente digital aperturado/));
  });
});
