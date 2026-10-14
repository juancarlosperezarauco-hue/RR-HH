import { inject, Injectable, signal, computed } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import {
  EmployeeFileService,
  EmployeeDocumentResponse,
  DocumentTypeResponse,
  DocumentCategory
} from '../../../entities/employee-file';
import { ToastService } from '../../../shared/ui/toast/toast.service';

@Injectable({ providedIn: 'root' })
export class EmployeeFileListUseCase {
  private readonly fileService = inject(EmployeeFileService);
  private readonly toast = inject(ToastService);

  readonly documents = signal<EmployeeDocumentResponse[]>([]);
  readonly documentTypes = signal<DocumentTypeResponse[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  // Filtros y paginación
  readonly selectedCategory = signal<string>('ALL');
  readonly searchQuery = signal<string>('');
  readonly selectedEmployeeId = signal<string>('9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d'); // ID default demo
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
        d.fileName.toLowerCase().includes(query) ||
        d.documentTypeName.toLowerCase().includes(query) ||
        (d.documentNumber && d.documentNumber.toLowerCase().includes(query))
    );
  });

  // Métricas reactivas (KPIs)
  readonly kpis = computed(() => {
    const all = this.documents();
    const total = all.length;
    const sensitive = all.filter((d) => d.documentCategory === 'HEALTH').length;
    const expiringSoon = all.filter((d) => {
      if (!d.expirationDate) return false;
      const exp = new Date(d.expirationDate).getTime();
      const now = Date.now();
      const diffDays = (exp - now) / (1000 * 60 * 60 * 24);
      return diffDays >= 0 && diffDays <= 60; // Vence en los próximos 60 días
    }).length;
    return { total, sensitive, expiringSoon };
  });

  async loadDocumentTypes(): Promise<void> {
    try {
      const res = await firstValueFrom(this.fileService.getDocumentTypes());
      this.documentTypes.set(res.data);
    } catch (err: any) {
      console.error('Error cargando tipos de documento', err);
    }
  }

  async loadDocuments(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const res = await firstValueFrom(
        this.fileService.getEmployeeDocuments(
          this.selectedEmployeeId(),
          this.selectedCategory(),
          this.currentPage(),
          this.pageSize()
        )
      );
      this.documents.set(res.data.content);
      this.totalElements.set(res.data.totalElements);
      this.totalPages.set(res.data.totalPages);
    } catch (err: any) {
      const msg = err?.error?.message || 'Error al cargar los documentos del expediente';
      this.error.set(msg);
      this.toast.error(msg);
    } finally {
      this.loading.set(false);
    }
  }

  setEmployeeId(employeeId: string): void {
    this.selectedEmployeeId.set(employeeId);
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

  async deleteDocument(documentId: string): Promise<boolean> {
    try {
      await firstValueFrom(this.fileService.deleteDocument(documentId));
      this.toast.success('Documento archivado exitosamente (soft delete aplicado)');
      // Actualizar estado local reactivo
      this.documents.update((docs) => docs.filter((d) => d.id !== documentId));
      return true;
    } catch (err: unknown) {
      const errorResponse = err as { error?: { message?: string } };
      const msg = errorResponse?.error?.message || 'No se pudo eliminar el documento';
      this.toast.error(msg);
      return false;
    }
  }

  /**
   * Verifica o rechaza un documento digital
   * PATCH /api/employee-files/{id}/documents/{documentId}/verify
   */
  async verifyDocument(documentId: string, status: 'VERIFIED' | 'REJECTED'): Promise<boolean> {
    try {
      const doc = this.documents().find((d) => d.id === documentId);
      const fileId = doc?.employeeFileId || `ef-${this.selectedEmployeeId()}`;
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
}

