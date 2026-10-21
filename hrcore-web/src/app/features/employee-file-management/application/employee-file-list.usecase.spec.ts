import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { EmployeeFileListUseCase } from './employee-file-list.usecase';
import { EmployeeFileService, EmployeeFileResponse, DocumentResponse } from '../../../entities/employee-file';
import { ToastService } from '../../../shared/ui/toast/toast.service';

describe('EmployeeFileListUseCase (HU15 - Expedientes Digitales)', () => {
  let useCase: EmployeeFileListUseCase;
  let fileServiceSpy: jasmine.SpyObj<EmployeeFileService>;
  let toastSpy: jasmine.SpyObj<ToastService>;

  const mockFile: EmployeeFileResponse = {
    id: 'file-uuid-001',
    employeeId: 'emp-uuid-001',
    fileNumber: 'EXP-EMP-001',
    status: 'ACTIVE',
    notes: 'Expediente digital principal',
    createdAt: '2026-01-15T08:00:00Z',
    updatedAt: '2026-01-15T08:00:00Z'
  };

  const mockDocument: DocumentResponse = {
    id: 'doc-uuid-101',
    employeeFileId: 'file-uuid-001',
    contractId: null,
    documentType: 'IDENTITY',
    title: 'Cédula de Identidad',
    description: 'Documento legal vigente',
    documentNumber: 'CI-1234567',
    issuedAt: '2024-01-01',
    expiresAt: '2029-01-01',
    confidential: false,
    verificationStatus: 'PENDING',
    status: 'ACTIVE',
    createdAt: '2026-01-15T09:00:00Z',
    updatedAt: '2026-01-15T09:00:00Z'
  };

  beforeEach(() => {
    fileServiceSpy = jasmine.createSpyObj('EmployeeFileService', [
      'getFileByEmployee',
      'listFiles',
      'listDocuments',
      'createFile',
      'verifyDocument',
      'archiveDocument'
    ]);
    toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error', 'warning', 'info']);

    TestBed.configureTestingModule({
      providers: [
        EmployeeFileListUseCase,
        { provide: EmployeeFileService, useValue: fileServiceSpy },
        { provide: ToastService, useValue: toastSpy }
      ]
    });

    fileServiceSpy.listFiles.and.returnValue(of({
      success: true,
      message: 'OK',
      data: { content: [], page: 0, size: 50, totalElements: 0, totalPages: 1 },
      timestamp: ''
    }));

    fileServiceSpy.listDocuments.and.returnValue(of({
      success: true,
      message: 'OK',
      data: { content: [], page: 0, size: 25, totalElements: 0, totalPages: 1 },
      timestamp: ''
    }));

    useCase = TestBed.inject(EmployeeFileListUseCase);
  });

  it('debe inicializarse con estado limpio y sin mocks', () => {
    expect(useCase.activeFile()).toBeNull();
    expect(useCase.documents().length).toBe(0);
    expect(useCase.loading()).toBeFalse();
  });

  it('debe cargar el expediente y documentos reales cuando el empleado tiene expediente', async () => {
    fileServiceSpy.getFileByEmployee.and.returnValue(of({
      success: true,
      message: 'OK',
      data: mockFile,
      timestamp: new Date().toISOString()
    }));

    fileServiceSpy.listDocuments.and.returnValue(of({
      success: true,
      message: 'OK',
      data: {
        content: [mockDocument],
        page: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1
      },
      timestamp: new Date().toISOString()
    }));

    useCase.setEmployeeId('emp-uuid-001');
    await useCase.loadDocuments();

    expect(fileServiceSpy.getFileByEmployee).toHaveBeenCalledWith('emp-uuid-001');
    expect(useCase.activeFile()?.id).toBe('file-uuid-001');
    expect(useCase.documents().length).toBe(1);
    expect(useCase.documents()[0].title).toBe('Cédula de Identidad');
    expect(useCase.kpis().total).toBe(1);
  });

  it('debe establecer activeFile en null cuando el empleado NO tiene expediente', async () => {
    fileServiceSpy.getFileByEmployee.and.returnValue(throwError(() => ({ status: 404, error: { message: 'Employee file not found' } })));

    useCase.setEmployeeId('emp-sin-expediente');
    await useCase.loadDocuments();

    expect(fileServiceSpy.getFileByEmployee).toHaveBeenCalledWith('emp-sin-expediente');
    expect(useCase.activeFile()).toBeNull();
    expect(useCase.documents().length).toBe(0);
    expect(useCase.totalElements()).toBe(0);
  });

  it('debe aperturar expediente en Spring Boot llamando a createFile con CreateFileRequest', async () => {
    fileServiceSpy.createFile.and.returnValue(of({
      success: true,
      message: 'Created',
      data: mockFile,
      timestamp: new Date().toISOString()
    }));
    fileServiceSpy.getFileByEmployee.and.returnValue(of({
      success: true,
      message: 'OK',
      data: mockFile,
      timestamp: new Date().toISOString()
    }));
    fileServiceSpy.listDocuments.and.returnValue(of({
      success: true,
      message: 'OK',
      data: { content: [], page: 0, size: 10, totalElements: 0, totalPages: 1 },
      timestamp: new Date().toISOString()
    }));

    const result = await useCase.createFileForEmployee('emp-uuid-001', 'EXP-EMP-001', 'Expediente nuevo');

    expect(result).toBeTrue();
    expect(fileServiceSpy.createFile).toHaveBeenCalledWith({
      employeeId: 'emp-uuid-001',
      fileNumber: 'EXP-EMP-001',
      notes: 'Expediente nuevo'
    });
    expect(toastSpy.success).toHaveBeenCalledWith('Expediente laboral aperturado correctamente.');
    expect(useCase.activeFile()?.fileNumber).toBe('EXP-EMP-001');
  });

  it('debe verificar un documento actualizando su estado reactivamente a VERIFIED', async () => {
    useCase.activeFile.set(mockFile);
    useCase.documents.set([{
      ...mockDocument,
      documentTypeId: 'IDENTITY',
      documentTypeName: 'Documento de Identidad',
      documentCategory: 'IDENTIFICATION',
      fileName: 'ci.pdf',
      filePath: '',
      fileSizeBytes: 1024,
      mimeType: 'application/pdf',
      versionNumber: 1
    }]);

    fileServiceSpy.verifyDocument.and.returnValue(of({
      success: true,
      message: 'Verified',
      data: { ...mockDocument, verificationStatus: 'VERIFIED' },
      timestamp: new Date().toISOString()
    }));

    const ok = await useCase.verifyDocument('doc-uuid-101', 'VERIFIED');

    expect(ok).toBeTrue();
    expect(fileServiceSpy.verifyDocument).toHaveBeenCalledWith('file-uuid-001', 'doc-uuid-101', 'VERIFIED');
    expect(useCase.documents()[0].verificationStatus).toBe('VERIFIED');
    expect(toastSpy.success).toHaveBeenCalled();
  });
});
