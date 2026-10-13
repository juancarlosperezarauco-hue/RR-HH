import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse, PageResponse } from '../../../shared/api';
import { DocumentTypeResponse } from '../model/document-type.model';
import {
  EmployeeDocumentResponse,
  UploadEmployeeDocumentRequest,
  SignedUrlResponse
} from '../model/employee-document.model';
import { EmployeeDocumentAuditResponse } from '../model/document-audit.model';

@Injectable({ providedIn: 'root' })
export class EmployeeFileService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1`;

  getDocumentTypes(): Observable<ApiResponse<DocumentTypeResponse[]>> {
    return this.http.get<ApiResponse<DocumentTypeResponse[]>>(`${this.baseUrl}/document-types`);
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
      `${this.baseUrl}/employees/${employeeId}/documents`,
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
      `${this.baseUrl}/employees/${req.employeeId}/documents`,
      formData
    );
  }

  getPreviewUrl(documentId: string): Observable<ApiResponse<SignedUrlResponse>> {
    return this.http.get<ApiResponse<SignedUrlResponse>>(`${this.baseUrl}/documents/${documentId}/preview`);
  }

  deleteDocument(documentId: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/documents/${documentId}`);
  }

  getDocumentAudits(documentId: string): Observable<ApiResponse<EmployeeDocumentAuditResponse[]>> {
    return this.http.get<ApiResponse<EmployeeDocumentAuditResponse[]>>(`${this.baseUrl}/documents/${documentId}/audits`);
  }
}
