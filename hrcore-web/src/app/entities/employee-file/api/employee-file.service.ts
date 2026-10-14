import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse, PageResponse } from '../../../shared/api';
import { DocumentType, DocumentTypeResponse } from '../model/document-type.model';
import {
  EmployeeFileResponse,
  CreateFileRequest,
  UpdateFileRequest,
  EmployeeFileFilter,
  FileStatus,
  EmployeeSummaryResponse
} from '../model/employee-file.model';
import {
  EmployeeDocumentResponse,
  DocumentResponse,
  CreateDocumentRequest,
  UpdateDocumentRequest,
  VerificationRequest,
  VerificationStatus,
  DocumentVersionResponse,
  UploadEmployeeDocumentRequest,
  SignedUrlResponse
} from '../model/employee-document.model';
import { EmployeeDocumentAuditResponse } from '../model/document-audit.model';

@Injectable({ providedIn: 'root' })
export class EmployeeFileService {
  private readonly http = inject(HttpClient);
  // URL canónica del backend Spring Boot: /api/employee-files
  private readonly filesUrl = `${environment.apiUrl}/api/employee-files`;
  // URL legada para compatibilidad con endpoints /api/v1
  private readonly legacyUrl = `${environment.apiUrl}/api/v1`;

  // =========================================================================
  // 1. ENDPOINTS CANÓNICOS DE EXPEDIENTE (EmployeeFile - /api/employee-files)
  // =========================================================================

  /**
   * Crear expediente para un empleado
   * POST /api/employee-files
   */
  createFile(req: CreateFileRequest): Observable<ApiResponse<EmployeeFileResponse>> {
    return this.http.post<ApiResponse<EmployeeFileResponse>>(this.filesUrl, req);
  }

  /**
   * Listar y filtrar expedientes
   * GET /api/employee-files
   */
  listFiles(
    filter?: EmployeeFileFilter,
    page = 0,
    size = 25,
    sort?: string
  ): Observable<ApiResponse<PageResponse<EmployeeFileResponse>>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', Math.min(Math.max(1, size), 100).toString());

    if (sort) params = params.set('sort', sort);
    if (filter?.employeeId) params = params.set('employeeId', filter.employeeId);
    if (filter?.fileNumber) params = params.set('fileNumber', filter.fileNumber);
    if (filter?.status) params = params.set('status', filter.status);

    return this.http.get<ApiResponse<PageResponse<EmployeeFileResponse>>>(this.filesUrl, { params });
  }

  /**
   * Consultar expediente por ID de empleado
   * GET /api/employee-files/by-employee/{employeeId}
   */
  getFileByEmployee(employeeId: string): Observable<ApiResponse<EmployeeFileResponse>> {
    return this.http.get<ApiResponse<EmployeeFileResponse>>(
      `${this.filesUrl}/by-employee/${employeeId}`
    );
  }

  /**
   * Consultar expediente por su ID
   * GET /api/employee-files/{id}
   */
  getFileById(id: string): Observable<ApiResponse<EmployeeFileResponse>> {
    return this.http.get<ApiResponse<EmployeeFileResponse>>(`${this.filesUrl}/${id}`);
  }

  /**
   * Editar expediente o su estado
   * PUT /api/employee-files/{id}
   */
  updateFile(id: string, req: UpdateFileRequest): Observable<ApiResponse<EmployeeFileResponse>> {
    return this.http.put<ApiResponse<EmployeeFileResponse>>(`${this.filesUrl}/${id}`, req);
  }

  /**
   * Consultar lista de empleados reales registrados en el sistema
   * GET /api/employees
   */
  getEmployees(page = 0, size = 50): Observable<ApiResponse<PageResponse<EmployeeSummaryResponse>>> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<ApiResponse<PageResponse<EmployeeSummaryResponse>>>(`${environment.apiUrl}/api/employees`, { params });
  }

  /**
   * Crear colaborador en el sistema si no existe ninguno
   * POST /api/employees
   */
  createEmployee(req: any): Observable<ApiResponse<EmployeeSummaryResponse>> {
    return this.http.post<ApiResponse<EmployeeSummaryResponse>>(`${environment.apiUrl}/api/employees`, req);
  }

  // =========================================================================
  // 2. ENDPOINTS CANÓNICOS DE DOCUMENTOS (/api/employee-files/{id}/documents)
  // =========================================================================

  /**
   * Registrar metadatos de un nuevo documento (Paso 1 del flujo de subida)
   * POST /api/employee-files/{id}/documents
   */
  createDocumentMetadata(
    fileId: string,
    req: CreateDocumentRequest
  ): Observable<ApiResponse<DocumentResponse>> {
    return this.http.post<ApiResponse<DocumentResponse>>(
      `${this.filesUrl}/${fileId}/documents`,
      req
    );
  }

  /**
   * Listar documentos de un expediente
   * GET /api/employee-files/{id}/documents
   */
  listDocuments(
    fileId: string,
    type?: DocumentType,
    status?: FileStatus,
    expiresBefore?: string,
    page = 0,
    size = 25
  ): Observable<ApiResponse<PageResponse<DocumentResponse>>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', Math.min(Math.max(1, size), 100).toString());

    if (type) params = params.set('type', type);
    if (status) params = params.set('status', status);
    if (expiresBefore) params = params.set('expiresBefore', expiresBefore);

    return this.http.get<ApiResponse<PageResponse<DocumentResponse>>>(
      `${this.filesUrl}/${fileId}/documents`,
      { params }
    );
  }

  /**
   * Consultar detalle de un documento
   * GET /api/employee-files/{id}/documents/{documentId}
   */
  getDocumentById(fileId: string, documentId: string): Observable<ApiResponse<DocumentResponse>> {
    return this.http.get<ApiResponse<DocumentResponse>>(
      `${this.filesUrl}/${fileId}/documents/${documentId}`
    );
  }

  /**
   * Actualizar metadatos de un documento
   * PUT /api/employee-files/{id}/documents/{documentId}
   */
  updateDocumentMetadata(
    fileId: string,
    documentId: string,
    req: UpdateDocumentRequest
  ): Observable<ApiResponse<DocumentResponse>> {
    return this.http.put<ApiResponse<DocumentResponse>>(
      `${this.filesUrl}/${fileId}/documents/${documentId}`,
      req
    );
  }

  /**
   * Verificar o rechazar un documento
   * PATCH /api/employee-files/{id}/documents/{documentId}/verify
   */
  verifyDocument(
    fileId: string,
    documentId: string,
    status: VerificationStatus
  ): Observable<ApiResponse<DocumentResponse>> {
    const payload: VerificationRequest = { status };
    return this.http.patch<ApiResponse<DocumentResponse>>(
      `${this.filesUrl}/${fileId}/documents/${documentId}/verify`,
      payload
    );
  }

  // =========================================================================
  // 3. ENDPOINTS CANÓNICOS DE VERSIONES (/api/employee-files/{id}/documents/{docId}/versions)
  // =========================================================================

  /**
   * Subir archivo físico como nueva versión (Paso 2 del flujo de subida)
   * POST /api/employee-files/{id}/documents/{documentId}/versions
   */
  uploadVersion(
    fileId: string,
    documentId: string,
    file: File
  ): Observable<ApiResponse<DocumentVersionResponse>> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<ApiResponse<DocumentVersionResponse>>(
      `${this.filesUrl}/${fileId}/documents/${documentId}/versions`,
      formData
    );
  }

  /**
   * Listar historial de versiones de un documento
   * GET /api/employee-files/{id}/documents/{documentId}/versions
   */
  listVersions(
    fileId: string,
    documentId: string,
    page = 0,
    size = 25
  ): Observable<ApiResponse<PageResponse<DocumentVersionResponse>>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', Math.min(Math.max(1, size), 100).toString());

    return this.http.get<ApiResponse<PageResponse<DocumentVersionResponse>>>(
      `${this.filesUrl}/${fileId}/documents/${documentId}/versions`,
      { params }
    );
  }

  /**
   * Descargar archivo binario directo (StreamingResponseBody Blob)
   * GET /api/employee-files/{id}/documents/{documentId}/versions/{versionId}/download
   */
  downloadVersion(fileId: string, documentId: string, versionId: string): Observable<Blob> {
    return this.http.get(
      `${this.filesUrl}/${fileId}/documents/${documentId}/versions/${versionId}/download`,
      { responseType: 'blob' }
    );
  }

  /**
   * Archivar documento (cumplimiento legal: no se elimina físicamente)
   * PUT /api/employee-files/{id}/documents/{documentId} con status: ARCHIVED
   */
  archiveDocument(
    fileId: string,
    documentId: string,
    doc: EmployeeDocumentResponse | DocumentResponse
  ): Observable<ApiResponse<DocumentResponse>> {
    const payload: UpdateDocumentRequest = {
      contractId: doc.contractId || null,
      documentType: (doc.documentType as DocumentType) || 'OTHER',
      title: doc.title || (doc as any).fileName || 'Documento',
      description: doc.description || null,
      documentNumber: doc.documentNumber || null,
      issuedAt: doc.issuedAt || (doc as any).issueDate || null,
      expiresAt: doc.expiresAt || (doc as any).expirationDate || null,
      confidential: !!doc.confidential,
      status: 'ARCHIVED'
    };
    return this.updateDocumentMetadata(fileId, documentId, payload);
  }

  // =========================================================================
  // 4. MÉTODOS DE COMPATIBILIDAD TRANSICIONAL (UI ACTUAL / MOCKS)
  // =========================================================================

  getDocumentTypes(): Observable<ApiResponse<DocumentTypeResponse[]>> {
    return this.http.get<ApiResponse<DocumentTypeResponse[]>>(`${this.legacyUrl}/document-types`);
  }

  getEmployeeDocuments(
    employeeId: string,
    category?: string,
    page = 0,
    size = 20
  ): Observable<ApiResponse<PageResponse<EmployeeDocumentResponse>>> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    if (category && category !== 'ALL') {
      params = params.set('category', category);
    }

    return this.http.get<ApiResponse<PageResponse<EmployeeDocumentResponse>>>(
      `${this.legacyUrl}/employees/${employeeId}/documents`,
      { params }
    );
  }

  uploadDocument(req: UploadEmployeeDocumentRequest): Observable<ApiResponse<EmployeeDocumentResponse>> {
    const formData = new FormData();
    formData.append('documentTypeId', req.documentTypeId);
    formData.append('file', req.file);
    if (req.documentNumber) formData.append('documentNumber', req.documentNumber);
    if (req.issueDate) formData.append('issueDate', req.issueDate);
    if (req.expirationDate) formData.append('expirationDate', req.expirationDate);

    return this.http.post<ApiResponse<EmployeeDocumentResponse>>(
      `${this.legacyUrl}/employees/${req.employeeId}/documents`,
      formData
    );
  }

  getPreviewUrl(documentId: string): Observable<ApiResponse<SignedUrlResponse>> {
    return this.http.get<ApiResponse<SignedUrlResponse>>(`${this.legacyUrl}/documents/${documentId}/preview`);
  }

  deleteDocument(documentId: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.legacyUrl}/documents/${documentId}`);
  }

  getDocumentAudits(documentId: string): Observable<ApiResponse<EmployeeDocumentAuditResponse[]>> {
    return this.http.get<ApiResponse<EmployeeDocumentAuditResponse[]>>(`${this.legacyUrl}/documents/${documentId}/audits`);
  }
}
