import { Component, EventEmitter, inject, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { DocumentTypeResponse } from '../../../../entities/employee-file';
import { EmployeeDocumentUploadUseCase } from '../../application/employee-document-upload.usecase';
import { ToastService } from '../../../../shared/ui/toast/toast.service';

@Component({
  selector: 'app-document-upload-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  template: `
    <div
      *ngIf="isOpen"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm">
      <div
        class="relative w-full max-w-lg rounded-[24px] border border-border bg-card p-6 shadow-2xl overflow-hidden space-y-5">
        
        <!-- Header -->
        <div class="flex items-center justify-between border-b border-border pb-4">
          <div>
            <h3 class="text-xl font-bold text-accent-foreground">Adjuntar Documento</h3>
            <p class="text-xs text-muted-foreground mt-0.5">Clasifica y archiva un documento en el expediente del trabajador.</p>
          </div>
          <button
            type="button"
            (click)="close()"
            class="inline-flex h-8 w-8 cursor-pointer items-center justify-center rounded-full border border-border bg-card text-muted-foreground hover:bg-destructive/10 hover:text-destructive">
            <lucide-icon name="x" [size]="16"></lucide-icon>
          </button>
        </div>

        <!-- Formulario -->
        <form (ngSubmit)="submit()" class="space-y-4">
          <!-- Tipo de Documento -->
          <div>
            <label class="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
              Tipo de Documento *
            </label>
            <select
              [(ngModel)]="selectedTypeId"
              name="documentTypeId"
              required
              class="w-full rounded-xl border border-border bg-muted/40 px-3.5 py-2.5 text-sm font-medium text-foreground outline-none focus:border-primary focus:ring-1 focus:ring-primary">
              <option value="" disabled selected>Selecciona una categoría de documento...</option>
              <option *ngFor="let t of documentTypes" [value]="t.id">
                {{ t.name }} ({{ t.category }})
              </option>
            </select>
          </div>

          <!-- Nro de Documento / Folio -->
          <div>
            <label class="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
              Nro. de Documento o Folio (Opcional)
            </label>
            <input
              type="text"
              [(ngModel)]="documentNumber"
              name="documentNumber"
              placeholder="Ej: CI-7483920 o CTR-2026-01"
              class="w-full rounded-xl border border-border bg-muted/40 px-3.5 py-2.5 text-sm text-foreground outline-none focus:border-primary focus:ring-1 focus:ring-primary" />
          </div>

          <!-- Fechas: Emisión y Vencimiento -->
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                Fecha Emisión
              </label>
              <input
                type="date"
                [(ngModel)]="issueDate"
                name="issueDate"
                class="w-full rounded-xl border border-border bg-muted/40 px-3.5 py-2 text-sm text-foreground outline-none focus:border-primary focus:ring-1 focus:ring-primary" />
            </div>
            <div>
              <label class="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                Fecha Vencimiento
              </label>
              <input
                type="date"
                [(ngModel)]="expirationDate"
                name="expirationDate"
                class="w-full rounded-xl border border-border bg-muted/40 px-3.5 py-2 text-sm text-foreground outline-none focus:border-primary focus:ring-1 focus:ring-primary" />
            </div>
          </div>

          <!-- Selector de Archivo / Drop Area con Validación Inmediata -->
          <div>
            <label class="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
              Archivo Digital (PDF, JPG, PNG, DOCX - Máx 10 MB) *
            </label>
            <div
              (dragover)="onDragOver($event)"
              (dragleave)="isDragging = false"
              (drop)="onFileDrop($event)"
              [class.border-primary]="isDragging"
              [class.bg-primary/5]="isDragging"
              [class.border-destructive]="!!fileError"
              [class.bg-destructive/5]="!!fileError"
              class="relative flex flex-col items-center justify-center rounded-2xl border-2 border-dashed border-border bg-muted/20 p-5 text-center transition-colors hover:border-primary/50">
              
              <input
                type="file"
                (change)="onFileSelected($event)"
                accept=".pdf,.png,.jpg,.jpeg,.docx"
                class="absolute inset-0 cursor-pointer opacity-0" />
              
              <div *ngIf="!selectedFile" class="space-y-1.5 pointer-events-none">
                <lucide-icon
                  [name]="fileError ? 'circle-alert' : 'upload'"
                  [size]="28"
                  [class.text-destructive]="!!fileError"
                  [class.text-muted-foreground]="!fileError"
                  class="mx-auto"></lucide-icon>
                <p class="text-xs font-semibold text-accent-foreground">
                  {{ isDragging ? 'Suelta el archivo aquí...' : 'Arrastra un archivo aquí o haz clic para explorar' }}
                </p>
                <p class="text-[11px] text-muted-foreground">PDF, JPEG, PNG, DOCX hasta 10MB</p>
              </div>

              <div *ngIf="selectedFile" class="flex items-center gap-3 text-left w-full bg-card p-3 rounded-xl border border-border">
                <lucide-icon name="file-text" [size]="24" class="text-primary shrink-0"></lucide-icon>
                <div class="min-w-0 flex-1">
                  <p class="truncate text-xs font-bold text-accent-foreground">{{ selectedFile.name }}</p>
                  <p class="text-[11px] text-muted-foreground">{{ (selectedFile.size / 1024 / 1024).toFixed(2) }} MB</p>
                </div>
                <button
                  type="button"
                  (click)="clearFile($event)"
                  class="p-1 text-muted-foreground hover:text-destructive cursor-pointer">
                  <lucide-icon name="x" [size]="16"></lucide-icon>
                </button>
              </div>
            </div>

            <!-- Mensaje de error inmediato -->
            <p *ngIf="fileError" class="mt-1.5 flex items-center gap-1 text-xs font-semibold text-destructive">
              <lucide-icon name="circle-alert" [size]="13"></lucide-icon>
              {{ fileError }}
            </p>
          </div>

          <!-- Botones de Acción -->
          <div class="flex items-center justify-end gap-3 pt-3 border-t border-border">
            <button
              type="button"
              (click)="close()"
              class="rounded-full border border-border bg-card px-4 py-2 text-xs font-semibold text-muted-foreground hover:bg-accent hover:text-foreground cursor-pointer">
              Cancelar
            </button>
            <button
              type="submit"
              [disabled]="!isValid() || uploadUseCase.uploading()"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full bg-primary px-5 py-2 text-xs font-semibold text-primary-foreground shadow-md transition-all hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed">
              <lucide-icon
                *ngIf="uploadUseCase.uploading()"
                name="loader-circle"
                [size]="14"
                class="animate-spin"></lucide-icon>
              {{ uploadUseCase.uploading() ? 'Subiendo...' : 'Subir Documento' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  `
})
export class DocumentUploadDialogComponent {
  @Input() isOpen = false;
  @Input() employeeId = '';
  @Input() documentTypes: DocumentTypeResponse[] = [];
  @Output() closeDialog = new EventEmitter<void>();
  @Output() uploaded = new EventEmitter<void>();

  readonly uploadUseCase = inject(EmployeeDocumentUploadUseCase);
  private readonly toastService = inject(ToastService);

  selectedTypeId = '';
  documentNumber = '';
  issueDate = '';
  expirationDate = '';
  selectedFile: File | null = null;
  fileError: string | null = null;
  isDragging = false;

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = true;
  }

  onFileDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = false;
    const file = event.dataTransfer?.files?.[0];
    if (file) {
      this.handleFile(file);
    }
  }

  onFileSelected(event: any): void {
    const file = event.target.files?.[0];
    if (file) {
      this.handleFile(file);
    }
  }

  private handleFile(file: File): void {
    const val = this.uploadUseCase.validateFile(file);
    if (!val.isValid) {
      this.selectedFile = null;
      this.fileError = val.message || 'Archivo no permitido';
      this.toastService.error(this.fileError);
      return;
    }
    this.fileError = null;
    this.selectedFile = file;
  }

  clearFile(event: Event): void {
    event.stopPropagation();
    this.selectedFile = null;
    this.fileError = null;
  }

  isValid(): boolean {
    return !!this.selectedTypeId && !!this.selectedFile && !this.fileError;
  }

  async submit(): Promise<void> {
    if (!this.isValid()) return;

    const ok = await this.uploadUseCase.upload({
      employeeId: this.employeeId,
      documentTypeId: this.selectedTypeId,
      documentNumber: this.documentNumber || undefined,
      issueDate: this.issueDate || undefined,
      expirationDate: this.expirationDate || undefined,
      file: this.selectedFile!
    });

    if (ok) {
      this.resetForm();
      this.uploaded.emit();
      this.close();
    }
  }

  close(): void {
    this.resetForm();
    this.closeDialog.emit();
  }

  private resetForm(): void {
    this.selectedTypeId = '';
    this.documentNumber = '';
    this.issueDate = '';
    this.expirationDate = '';
    this.selectedFile = null;
    this.fileError = null;
    this.isDragging = false;
  }
}
