import { inject, Injectable, signal, computed } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import {
  EmployeeFileService,
  EmployeeDocumentResponse,
  DocumentResponse,
  EmployeeFileResponse,
  DocumentTypeResponse,
  DocumentCategory,
  DocumentType,
  DOCUMENT_TYPE_LABELS
} from '../../../entities/employee-file';
import { ToastService } from '../../../shared/ui/toast/toast.service';
import { environment } from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class EmployeeFileListUseCase {
  private readonly fileService = inject(EmployeeFileService);
  private readonly toast = inject(ToastService);

  readonly files = signal<EmployeeFileResponse[]>([]);
  readonly activeFile = signal<EmployeeFileResponse | null>(null);
  readonly documents = signal<EmployeeDocumentResponse[]>([]);
  readonly documentTypes = signal<DocumentTypeResponse[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  // Filtros y paginación
  readonly selectedCategory = signal<string>('ALL');
  readonly searchQuery = signal<string>('');
  readonly selectedEmployeeId = signal<string>('');
  readonly currentPage = signal(0);
  readonly pageSize = signal(8);
  readonly totalElements = signal(0);
  readonly totalPages = signal(1);

  // Documentos filtrados reactivamente por búsqueda local adicional
  readonly filteredDocuments = computed(() => {
    const query = this.searchQuery().toLowerCase().trim();
    const docs = this.documents();
    if (!query) return docs;
    return docs.filter(
      (d) =>
        (d.fileName && d.fileName.toLowerCase().includes(query)) ||
        (d.title && d.title.toLowerCase().includes(query)) ||
        (d.documentTypeName && d.documentTypeName.toLowerCase().includes(query)) ||
        (d.documentNumber && d.documentNumber.toLowerCase().includes(query))
    );
  });

  // Métricas reactivas (KPIs)
  readonly kpis = computed(() => {
    const all = this.documents();
    const total = all.length;
    const sensitive = all.filter((d) => d.confidential || d.documentCategory === 'HEALTH').length;
    const expiringSoon = all.filter((d) => {
      const expDate = d.expirationDate || d.expiresAt;
      if (!expDate) return false;
      const exp = new Date(expDate).getTime();
      const now = Date.now();
      const diffDays = (exp - now) / (1000 * 60 * 60 * 24);
      return diffDays >= 0 && diffDays <= 60; // Vence en los próximos 60 días
    }).length;
    return { total, sensitive, expiringSoon };
  });

  async loadDocumentTypes(): Promise<void> {
    try {
      const res = await firstValueFrom(this.fileService.getDocumentTypes());
      this.documentTypes.set(res?.data || []);
    } catch {
      // Se usarán los tipos canónicos de DOCUMENT_TYPE_LABELS
    }
  }

  /**
   * Consulta expedientes y documentos reales desde Spring Boot
   * GET /api/employee-files
   * GET /api/employee-files/{id}/documents
   */
  async loadDocuments(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      // 1. Obtener lista de expedientes laborales del tenant
      const employeeId = this.selectedEmployeeId();
      const filesRes = await firstValueFrom(this.fileService.listFiles(
        employeeId ? { employeeId } : undefined,
        0,
        50
      ));
      const fileList = filesRes?.data?.content || [];
      this.files.set(fileList);

      if (fileList.length === 0) {
        // No hay expedientes registrados aún
        this.activeFile.set(null);
        this.documents.set([]);
        this.totalElements.set(0);
        this.totalPages.set(1);
        return;
      }

      // Determinar expediente activo
      let current = this.activeFile();
      if (!current || !fileList.some((f) => f.id === current?.id)) {
        current = fileList[0];
        this.activeFile.set(current);
        if (current.employeeId) {
          this.selectedEmployeeId.set(current.employeeId);
        }
      }

      // 2. Mapear categoría a enum DocumentType para filtro de backend
      const docTypeFilter = this.mapCategoryToDocumentType(this.selectedCategory());

      // 3. Consultar documentos activos del expediente (status=ACTIVE para excluir archivados)
      const docsRes = await firstValueFrom(
        this.fileService.listDocuments(
          current.id,
          docTypeFilter,
          'ACTIVE',
          undefined,
          this.currentPage(),
          this.pageSize()
        )
      );

      const rawDocs = docsRes?.data?.content || [];
      const mapped = rawDocs.map((d) => this.mapDocumentResponse(d));

      this.documents.set(mapped);
      this.totalElements.set(docsRes?.data?.totalElements ?? mapped.length);
      this.totalPages.set(docsRes?.data?.totalPages ?? 1);
    } catch (err: any) {
      console.warn('No se encontraron documentos o error de carga:', err);
      this.documents.set([]);
      this.totalElements.set(0);
      this.totalPages.set(1);
    } finally {
      this.loading.set(false);
    }
  }

  setEmployeeId(employeeId: string): void {
    this.selectedEmployeeId.set(employeeId);
    this.activeFile.set(null);
    this.currentPage.set(0);
    this.loadDocuments();
  }

  async createFileForEmployee(employeeId: string, employeeNumber: string): Promise<boolean> {
    if (!employeeId) {
      this.toast.warning('Seleccione un empleado para crear su expediente.');
      return false;
    }
    try {
      const response = await firstValueFrom(this.fileService.createFile({
        employeeId,
        fileNumber: `EXP-${employeeNumber || employeeId.substring(0, 8)}`.toUpperCase(),
        notes: 'Expediente laboral digital del colaborador'
      }));
      this.activeFile.set(response.data);
      this.files.set([response.data]);
      this.toast.success('Expediente laboral creado correctamente.');
      await this.loadDocuments();
      return true;
    } catch (err: any) {
      this.toast.error(err?.error?.message || 'No se pudo crear el expediente laboral.');
      return false;
    }
  }

  setActiveFile(file: EmployeeFileResponse): void {
    this.activeFile.set(file);
    if (file.employeeId) {
      this.selectedEmployeeId.set(file.employeeId);
    }
    this.currentPage.set(0);
    this.loadDocuments();
  }

  setCategory(category: string): void {
    this.selectedCategory.set(category);
    this.currentPage.set(0);
    this.loadDocuments();
  }

  setSearchQuery(query: string): void {
    this.searchQuery.set(query);
  }

  setPage(page: number): void {
    this.currentPage.set(page);
    this.loadDocuments();
  }

  /**
   * Archiva un documento laboral (cumplimiento legal: no se elimina físicamente)
   * PUT /api/employee-files/{id}/documents/{documentId} con status: ARCHIVED
   */
  async archiveDocument(documentId: string): Promise<boolean> {
    try {
      const doc = this.documents().find((d) => d.id === documentId);
      const fileId = doc?.employeeFileId || this.activeFile()?.id;
      if (!fileId) {
        throw new Error('No se encontró el identificador del expediente.');
      }
      await firstValueFrom(this.fileService.archiveDocument(fileId, documentId, doc!));
      this.toast.success('Documento archivado exitosamente (cumplimiento legal aplicado)');
      
      // Actualizar reactivamente el listado retirando el documento de la vista activa
      this.documents.update((docs) => docs.filter((d) => d.id !== documentId));
      this.totalElements.update((prev) => Math.max(0, prev - 1));
      return true;
    } catch (err: unknown) {
      const errorResponse = err as { error?: { message?: string } };
      const msg = errorResponse?.error?.message || 'No se pudo archivar el documento';
      this.toast.error(msg);
      return false;
    }
  }

  /**
   * Alias de compatibilidad
   */
  deleteDocument(documentId: string): Promise<boolean> {
    return this.archiveDocument(documentId);
  }

  /**
   * Verifica o rechaza un documento digital
   * PATCH /api/employee-files/{id}/documents/{documentId}/verify
   */
  async verifyDocument(documentId: string, status: 'VERIFIED' | 'REJECTED'): Promise<boolean> {
    try {
      const doc = this.documents().find((d) => d.id === documentId);
      const fileId = doc?.employeeFileId || this.activeFile()?.id;
      if (!fileId) {
        throw new Error('No se encontró el identificador del expediente');
      }
      await firstValueFrom(this.fileService.verifyDocument(fileId, documentId, status));
      this.toast.success(
        status === 'VERIFIED' ? 'Documento verificado y validado con éxito' : 'Documento marcado como rechazado'
      );
      this.documents.update((docs) =>
        docs.map((d) => (d.id === documentId ? { ...d, verificationStatus: status } : d))
      );
      return true;
    } catch (err: unknown) {
      const errorResponse = err as { error?: { message?: string } };
      const msg = errorResponse?.error?.message || 'Error al actualizar el estado de verificación';
      this.toast.error(msg);
      return false;
    }
  }

  private mapCategoryToDocumentType(category: string): DocumentType | undefined {
    switch (category) {
      case 'IDENTIFICATION': return 'IDENTITY';
      case 'CONTRACT': return 'CONTRACT';
      case 'EDUCATION': return 'EDUCATION';
      case 'HEALTH': return 'MEDICAL';
      case 'DISCIPLINARY': return 'DISCIPLINARY';
      case 'ASSET': return 'OTHER';
      default: return undefined;
    }
  }

  private mapDocumentResponse(doc: DocumentResponse): EmployeeDocumentResponse {
    const categoryMap: Record<DocumentType, DocumentCategory> = {
      IDENTITY: 'IDENTIFICATION',
      CONTRACT: 'CONTRACT',
      CERTIFICATE: 'EDUCATION',
      EDUCATION: 'EDUCATION',
      MEDICAL: 'HEALTH',
      DISCIPLINARY: 'DISCIPLINARY',
      PAYROLL: 'CONTRACT',
      OTHER: 'ASSET'
    };
    return {
      id: doc.id,
      employeeFileId: doc.employeeFileId,
      contractId: doc.contractId,
      documentType: doc.documentType,
      title: doc.title,
      description: doc.description,
      documentNumber: doc.documentNumber,
      issuedAt: doc.issuedAt,
      expiresAt: doc.expiresAt,
      confidential: doc.confidential,
      verificationStatus: doc.verificationStatus,
      verifiedBy: doc.verifiedBy,
      verifiedAt: doc.verifiedAt,
      status: doc.status,
      createdAt: doc.createdAt,
      updatedAt: doc.updatedAt,
      documentTypeId: doc.documentType,
      documentTypeName: DOCUMENT_TYPE_LABELS[doc.documentType] || doc.documentType,
      documentCategory: categoryMap[doc.documentType] || 'CONTRACT',
      fileName: doc.title,
      filePath: '',
      fileSizeBytes: 0,
      mimeType: 'application/pdf',
      versionNumber: 1,
      issueDate: doc.issuedAt,
      expirationDate: doc.expiresAt,
      downloadUrl: `${environment.apiUrl}/api/employee-files/${doc.employeeFileId}/documents/${doc.id}`
    };
  }
}
