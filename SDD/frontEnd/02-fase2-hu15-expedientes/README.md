# SDD - Fase 2: HU15 - Gestión de Expedientes Laborales
## Capas `features/employee-file-management` y `pages/employee-files-page`

---

### 1. Resumen de la Historia de Usuario y Reglas de Negocio
* **Actor:** Responsable de Recursos Humanos.
* **Historia:** Crear, consultar y actualizar expedientes de trabajadores, adjuntando y clasificando documentos personales, contractuales y laborales, para centralizar su documentación y mantener un historial organizado.
* **Alineación con Base de Datos (`dateBaseDiagram.md` - Dominio 6):**
  * `document_type`: Catálogo por tenant (`IDENTIFICATION`, `CONTRACT`, `EDUCATION`, `HEALTH`, `DISCIPLINARY`, `ASSET`).
  * `employee_document`: Archivos con `file_size_bytes` (máx 10MB), `mime_type`, `version_number` y soft delete (`status: ACTIVE, EXPIRED, ARCHIVED`).
  * `employee_document_audit`: Auditoría inmutable de acciones (`UPLOAD`, `VIEW`, `DOWNLOAD`, `UPDATE`, `ARCHIVE`).
* **Reglas Clave:**
  1. **Validación de Archivos:** Acepta solo `.pdf`, `.png`, `.jpg`, `.jpeg`, `.docx` con un tamaño máximo de 10 MB (10,485,760 bytes).
  2. **Previsualización Embebida y Descarga Segura:** Previsualización en modal para PDFs e imágenes mediante URLs firmadas temporales emitidas por el backend (`getPreviewUrl`).
  3. **Control de Acceso RBAC:** Documentos de categoría `HEALTH` (`MEDICO`) exigen permiso especial `dossiers.sensitive.read`.
  4. **Línea de Tiempo Histórica:** Línea de tiempo visual cronológica de eventos, contratos, amonestaciones y movimientos.
  5. **Compatibilidad con Mocks:** Compatible de forma nativa con [`hu15-dossiers.mock.json`](file:///c:/Users/hp/web/proyectos/proyecto%20U/si2/proyecto_grupalS2_2-2026/hrcore-system/SDD/BackEnd/mocks/hu15-dossiers.mock.json) cuando `environment.useMocks === true`.

---

### 2. Estructura Modular de Archivos a Implementar

```
hrcore-web/src/app/
├── features/employee-file-management/
│   ├── index.ts
│   ├── application/
│   │   ├── employee-file-list.usecase.ts
│   │   ├── employee-document-upload.usecase.ts
│   │   └── employee-document-preview.usecase.ts
│   └── ui/
│       ├── document-category-filter/
│       │   └── document-category-filter.component.ts
│       ├── document-upload-dialog/
│       │   └── document-upload-dialog.component.ts
│       ├── document-preview-modal/
│       │   └── document-preview-modal.component.ts
│       ├── document-timeline/
│       │   └── document-timeline.component.ts
│       └── document-card-grid/
│           └── document-card-grid.component.ts
│
└── pages/employee-files-page/
    └── employee-files-page.component.ts
```

---

### 3. Casos de Uso (Application Layer)

#### 3.1. `employee-document-upload.usecase.ts`
```typescript
import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { EmployeeFileService, UploadEmployeeDocumentRequest } from '../../../entities/employee-file';
import { ToastService } from '../../../shared/ui/toast/toast.service';

@Injectable({ providedIn: 'root' })
export class EmployeeDocumentUploadUseCase {
  private readonly fileService = inject(EmployeeFileService);
  private readonly toastService = inject(ToastService);

  readonly uploading = signal(false);
  readonly error = signal<string | null>(null);

  readonly ALLOWED_MIME_TYPES = [
    'application/pdf',
    'image/jpeg',
    'image/png',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
  ];
  readonly MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB

  validateFile(file: File): { isValid: boolean; message?: string } {
    if (!this.ALLOWED_MIME_TYPES.includes(file.type)) {
      return { isValid: false, message: 'Formato no soportado. Se aceptan únicamente PDF, JPG, PNG o DOCX.' };
    }
    if (file.size > this.MAX_FILE_SIZE_BYTES) {
      return { isValid: false, message: 'El archivo excede el tamaño máximo permitido de 10 MB.' };
    }
    return { isValid: true };
  }

  async execute(request: UploadEmployeeDocumentRequest): Promise<boolean> {
    const validation = this.validateFile(request.file);
    if (!validation.isValid) {
      this.error.set(validation.message!);
      this.toastService.error(validation.message!);
      return false;
    }

    this.uploading.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(this.fileService.uploadDocument(request));
      this.toastService.success('Documento subido y clasificado exitosamente');
      return true;
    } catch (err: any) {
      const msg = err?.error?.message || 'Error al subir el documento';
      this.error.set(msg);
      this.toastService.error(msg);
      return false;
    } finally {
      this.uploading.set(false);
    }
  }
}
```

#### 3.2. `employee-document-preview.usecase.ts`
```typescript
import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { EmployeeFileService, EmployeeDocumentResponse } from '../../../entities/employee-file';

@Injectable({ providedIn: 'root' })
export class EmployeeDocumentPreviewUseCase {
  private readonly fileService = inject(EmployeeFileService);

  readonly previewUrl = signal<string | null>(null);
  readonly selectedDoc = signal<EmployeeDocumentResponse | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  async openPreview(doc: EmployeeDocumentResponse): Promise<void> {
    this.selectedDoc.set(doc);
    this.loading.set(true);
    this.error.set(null);
    try {
      const res = await firstValueFrom(this.fileService.getPreviewUrl(doc.id));
      this.previewUrl.set(res.data.downloadUrl);
    } catch (err: any) {
      this.error.set(err?.error?.message || 'No se pudo generar la URL de previsualización');
    } finally {
      this.loading.set(false);
    }
  }

  closePreview(): void {
    this.selectedDoc.set(null);
    this.previewUrl.set(null);
  }
}
```

---

### 4. Componentes Visuales Clave

1. **`document-preview-modal.component.ts`:**
   * Modal flotante con fondo opaco `bg-background/80 backdrop-blur-sm`.
   * Para PDF: Embed interactivo `<iframe [src]="safeUrl" class="w-full h-[75vh]"></iframe>`.
   * Para Imágenes: Visor centrado con zoom y fondo oscuro contrastante.
   * Barra de herramientas superior: Nombre del archivo, badge de categoría, botón de descarga directa y botón de cierre.

2. **`document-timeline.component.ts`:**
   * Lista cronológica vertical con íconos temáticos según el evento (`file-plus` para subida, `award` para contrato, `shield-alert` para amonestación).
   * Muestra fecha, autor de la acción y estado de verificación.

---

### 5. Criterios de Aceptación y Definición de Terminado (DoD) - Fase 2

#### 5.1. Checklist de Calidad Técnica y Código Limpio (Quality Gate)
- [ ] **Componentización y SRP:** La vista está modularizada en componentes de responsabilidad única:
  * `document-upload-dialog.component.ts` (solo formulario y validación de archivo).
  * `document-preview-modal.component.ts` (solo renderizado seguro de iframe/imagen).
  * `document-timeline.component.ts` (solo renderizado de eventos cronológicos).
  * `document-card-grid.component.ts` (solo grilla de tarjetas y filtros).
- [ ] **Clean Code & Tipado Estricto:** Prohibido el uso de `any`. Las interfaces provienen directamente de `src/app/entities/employee-file`.
- [ ] **Encapsulamiento en Casos de Uso:** Toda la lógica de validación de archivos, tamaño, estado de carga y comunicación HTTP reside en `EmployeeDocumentUploadUseCase` y `EmployeeDocumentPreviewUseCase`.
- [ ] **Reactividad con Signals:** Los estados de carga (`uploading`, `loading`) y errores (`error`) se manejan mediante `signal<boolean>` y `signal<string | null>`.
- [ ] **Verificación Pre-Entrega (Zero-Bug Gate):**
  * `npm run build` ejecutado con éxito sin errores de compilación.
  * Consola de navegador sin advertencias ni fugas de memoria al abrir y cerrar modales de previsualización.
  * Verificado en modo mock (`useMocks: true`) con datos de `hu15-dossiers.mock.json`.

#### 5.2. Escenarios BDD de Aceptación Funcional (Gherkin)

##### Escenario 1: Subida Exitosa de Documento Válido
* **DADO QUE** un usuario con permiso `dossiers.write` está en el expediente de un colaborador.
* **CUANDO** selecciona un archivo en formato PDF de 3.5 MB, clasificado como `CONTRACT`, e ingresa los metadatos requeridos.
* **ENTONCES** el caso de uso valida exitosamente el tipo MIME y tamaño, envía el `FormData`, muestra un mensaje toast de éxito y añade el documento a la lista y línea de tiempo.

##### Escenario 2: Bloqueo Inmediato por Exceso de Tamaño (Validación en Cliente)
* **DADO QUE** el usuario intenta adjuntar un archivo que pesa 12 MB (superando el límite de 10 MB).
* **CUANDO** suelta o selecciona el archivo en el modal de subida.
* **ENTONCES** el caso de uso rechaza el archivo de inmediato, no realiza ninguna petición de red, resalta el campo en color destructivo y muestra: *"El archivo excede el tamaño máximo permitido de 10 MB"*.

##### Escenario 3: Bloqueo de Formato no Permitido
* **DADO QUE** el usuario selecciona un archivo con extensión `.exe` o `.zip`.
* **CUANDO** se dispara el evento de selección de archivo.
* **ENTONCES** el sistema impide el envío y notifica: *"Formato no soportado. Se aceptan únicamente PDF, JPG, PNG o DOCX"*.

##### Escenario 4: Previsualización Embebida con URL Firmada
* **DADO QUE** un documento PDF o imagen ya se encuentra registrado en el expediente.
* **CUANDO** el usuario hace clic en el botón "Previsualizar".
* **ENTONCES** el modal `document-preview-modal` solicita la URL firmada al caso de uso, activa un spinner mientras resuelve y embebe el visor seguro (`<iframe>` para PDF o visor con zoom para imágenes) sin descargar el archivo localmente.

##### Escenario 5: Control de Acceso RBAC para Documentos Sensibles
* **DADO QUE** un usuario autenticado solo cuenta con el permiso `dossiers.read` (sin `dossiers.sensitive.read`).
* **CUANDO** visualiza el expediente del empleado.
* **ENTONCES** los documentos de categoría `HEALTH` (`MEDICO`) no muestran enlaces de previsualización ni descarga, mostrando un badge de *"Documento Confidencial - Requiere Autorización Especial"*.

