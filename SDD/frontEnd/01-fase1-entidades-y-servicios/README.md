# SDD - Fase 1: Arquitectura de Entidades, Modelos Tipados y Servicios API
## Capa `entities/` (Alineada al 100% con `SDD/DataBase/dateBaseDiagram.md`)

---

### 1. Propósito de la Fase 1

Construir la capa de cimientos y contratos de datos para **HU15, HU16, HU17 y HU18** en el directorio `src/app/entities/`.
Esta capa es completamente independiente de componentes visuales y contiene:
1. Interfaces de modelos y DTOs cuyos atributos coinciden estrictamente con las tablas SQL de `dateBaseDiagram.md`.
2. Servicios Angular (`HttpClient`) preparados para conectarse tanto a la API real de Spring Boot como al `MockHttpInterceptor` mediante `environment.useMocks`.
3. Manejo de envoltura estándar `ApiResponse<T>` y paginación `PageResponse<T>`.

---

### 2. Estructura Modular de Archivos a Crear

```
hrcore-web/src/app/entities/
├── employee-file/                    # HU15: Dominio 6 DB (document_type, employee_document)
│   ├── index.ts
│   ├── model/
│   │   ├── document-type.model.ts
│   │   ├── employee-document.model.ts
│   │   └── document-audit.model.ts
│   └── api/
│       └── employee-file.service.ts
│
├── shift/                            # HU16: Dominio 7 DB (shift, shift_day, assignment)
│   ├── index.ts
│   ├── model/
│   │   ├── shift.model.ts
│   │   ├── shift-day.model.ts
│   │   └── shift-assignment.model.ts
│   └── api/
│       └── shift.service.ts
│
├── attendance/                       # HU17: Dominio 8 DB (record, punch, correction, overtime)
│   ├── index.ts
│   ├── model/
│   │   ├── attendance-record.model.ts
│   │   ├── attendance-punch.model.ts
│   │   └── attendance-correction.model.ts
│   └── api/
│       └── attendance.service.ts
│
└── absence/                          # HU18: Dominio 9 DB (leave_type, leave_balance, leave_request, holiday)
    ├── index.ts
    ├── model/
    │   ├── leave-type.model.ts
    │   ├── leave-balance.model.ts
    │   ├── leave-request.model.ts
    │   └── holiday.model.ts
    └── api/
        └── absence.service.ts
```

---

### 3. Especificación Detallada de Modelos y Contratos

#### 3.1. HU15: `entities/employee-file`

##### Archivo: `model/document-type.model.ts`
```typescript
export type DocumentCategory = 
  | 'IDENTIFICATION' // CI, Pasaporte, Libreta militar
  | 'CONTRACT'       // Contratos, adendas, confidencialidad
  | 'EDUCATION'      // Títulos académicos, certificaciones
  | 'HEALTH'         // Exámenes pre-ocupacionales, bajas (SENSIBLE)
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
```

##### Archivo: `model/employee-document.model.ts`
```typescript
import { DocumentCategory } from './document-type.model';

export type DocumentStatus = 'ACTIVE' | 'EXPIRED' | 'ARCHIVED';

export interface EmployeeDocumentResponse {
  id: string;              // UUID
  tenantId: string;        // UUID
  employeeId: string;      // UUID
  documentTypeId: string;  // UUID
  documentTypeName: string;
  documentCategory: DocumentCategory;
  documentNumber?: string | null;
  fileName: string;
  filePath: string;
  fileSizeBytes: number;   // BIGINT (máx 10 MB)
  mimeType: string;        // 'application/pdf' | 'image/jpeg' | 'image/png' | ...
  versionNumber: number;
  issueDate?: string | null;      // YYYY-MM-DD
  expirationDate?: string | null; // YYYY-MM-DD
  status: DocumentStatus;
  verifiedBy?: string | null;     // UUID
  verifiedAt?: string | null;     // ISO Timestamp
  createdAt: string;              // ISO Timestamp
  updatedAt: string;              // ISO Timestamp
  downloadUrl?: string;           // URL firmada temporal para preview o descarga
}

export interface UploadEmployeeDocumentRequest {
  employeeId: string;
  documentTypeId: string;
  documentNumber?: string;
  issueDate?: string;
  expirationDate?: string;
  file: File;
}

export interface SignedUrlResponse {
  downloadUrl: string;
  expiresInMinutes: number;
}
```

##### Archivo: `model/document-audit.model.ts`
```typescript
export type DocumentAuditAction = 'UPLOAD' | 'VIEW' | 'DOWNLOAD' | 'UPDATE' | 'ARCHIVE';

export interface EmployeeDocumentAuditResponse {
  id: string;          // UUID
  documentId: string;  // UUID
  userId: string;      // UUID
  userEmail?: string;
  action: DocumentAuditAction;
  performedAt: string; // ISO Timestamp
  ipAddress?: string;
}
```

##### Archivo: `api/employee-file.service.ts`
```typescript
import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../shared/api';
import { PageResponse } from '../../user/model/page-response.model';
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
    let params = new HttpParams().set('page', page).set('size', size);
    if (category && category !== 'ALL') params = params.set('category', category);

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
```

---

#### 3.2. HU16: `entities/shift`

##### Archivo: `model/shift.model.ts`
```typescript
export type ShiftType = 'CONTINUOUS' | 'SPLIT' | 'ROTATING' | 'NIGHT';

export interface ShiftDayResponse {
  id?: string;
  shiftId?: string;
  dayOfWeek: number; // 1: Lunes, 7: Domingo
  isWorkDay: boolean;
  startTime?: string | null; // HH:mm:ss
  endTime?: string | null;   // HH:mm:ss
  breakMinutes: number;
}

export interface ShiftResponse {
  id: string;               // UUID
  tenantId: string;         // UUID
  code: string;             // VARCHAR(50)
  name: string;             // VARCHAR(100)
  shiftType: ShiftType;
  startTime: string;        // TIME (HH:mm:ss)
  endTime: string;          // TIME (HH:mm:ss)
  breakMinutes: number;     // INT
  gracePeriodMinutes: number; // Tolerancia (5 a 15 min, default 10)
  isOvernight: boolean;     // Cruce de medianoche (+1 día)
  status: 'ACTIVE' | 'INACTIVE';
  createdAt: string;
  updatedAt: string;
  days?: ShiftDayResponse[];
}

export interface CreateShiftRequest {
  code: string;
  name: string;
  shiftType: ShiftType;
  startTime: string;
  endTime: string;
  breakMinutes: number;
  gracePeriodMinutes: number;
  isOvernight: boolean;
  workDays: number[]; // Array de días [1, 2, 3, 4, 5]
}
```

##### Archivo: `model/shift-assignment.model.ts`
```typescript
export interface EmployeeShiftAssignmentResponse {
  id: string;
  tenantId: string;
  employeeId: string;
  employeeFullName?: string;
  employeeCode?: string;
  shiftId: string;
  shiftName?: string;
  startDate: string;        // YYYY-MM-DD
  endDate?: string | null;  // YYYY-MM-DD (null = indefinido)
  isRecurring: boolean;
  status: 'ACTIVE' | 'INACTIVE';
  createdAt: string;
}

export interface AssignShiftRequest {
  employeeIds: string[]; // Asignación masiva o individual
  shiftId: string;
  startDate: string;
  endDate?: string | null;
  isRecurring: boolean;
}
```

##### Archivo: `api/shift.service.ts`
```typescript
import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../shared/api';
import { PageResponse } from '../../user/model/page-response.model';
import { ShiftResponse, CreateShiftRequest } from '../model/shift.model';
import {
  EmployeeShiftAssignmentResponse,
  AssignShiftRequest
} from '../model/shift-assignment.model';

@Injectable({ providedIn: 'root' })
export class ShiftService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/shifts`;

  listShifts(status?: 'ACTIVE' | 'INACTIVE' | 'ALL', page = 0, size = 20): Observable<ApiResponse<PageResponse<ShiftResponse>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status && status !== 'ALL') params = params.set('status', status);

    return this.http.get<ApiResponse<PageResponse<ShiftResponse>>>(this.baseUrl, { params });
  }

  createShift(request: CreateShiftRequest): Observable<ApiResponse<ShiftResponse>> {
    return this.http.post<ApiResponse<ShiftResponse>>(this.baseUrl, request);
  }

  updateShift(id: string, request: CreateShiftRequest): Observable<ApiResponse<ShiftResponse>> {
    return this.http.put<ApiResponse<ShiftResponse>>(`${this.baseUrl}/${id}`, request);
  }

  toggleShiftStatus(id: string, newStatus: 'ACTIVE' | 'INACTIVE'): Observable<ApiResponse<ShiftResponse>> {
    return this.http.patch<ApiResponse<ShiftResponse>>(`${this.baseUrl}/${id}/status`, { status: newStatus });
  }

  assignShifts(request: AssignShiftRequest): Observable<ApiResponse<EmployeeShiftAssignmentResponse[]>> {
    return this.http.post<ApiResponse<EmployeeShiftAssignmentResponse[]>>(`${this.baseUrl}/assignments`, request);
  }

  getEmployeeAssignments(employeeId: string): Observable<ApiResponse<EmployeeShiftAssignmentResponse[]>> {
    return this.http.get<ApiResponse<EmployeeShiftAssignmentResponse[]>>(`${this.baseUrl}/assignments/employee/${employeeId}`);
  }
}
```

---

#### 3.3. HU17: `entities/attendance`

##### Archivo: `model/attendance-record.model.ts`
```typescript
export type AttendanceStatus = 
  | 'PRESENT'
  | 'LATE'
  | 'EARLY_DEPARTURE'
  | 'ABSENT'
  | 'EXCUSED'
  | 'HOLIDAY'
  | 'REST_DAY';

export type PunchSource = 'WEB' | 'MOBILE' | 'BIOMETRIC' | 'MANUAL';

export interface AttendanceRecordResponse {
  id: string;               // UUID
  tenantId: string;         // UUID
  employeeId: string;       // UUID
  employeeFullName?: string;
  employeeCode?: string;
  shiftId?: string | null;  // UUID
  workDate: string;         // YYYY-MM-DD
  checkInTime?: string | null;  // ISO Timestamp server-side
  checkOutTime?: string | null; // ISO Timestamp server-side
  workedHours: number;      // Horas ordinarias computadas
  overtimeHours: number;    // Horas extraordinarias computadas
  lateMinutes: number;      // Retraso en minutos
  earlyDepartureMinutes: number;
  status: AttendanceStatus;
  source: PunchSource;
  latitude?: number | null;
  longitude?: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface ClockPunchRequest {
  employeeId: string;
  type: 'IN' | 'OUT' | 'BREAK_OUT' | 'BREAK_IN';
  method: PunchSource;
  latitude?: number;
  longitude?: number;
}
```

##### Archivo: `model/attendance-correction.model.ts`
```typescript
export interface AttendanceCorrectionResponse {
  id: string;
  attendanceRecordId: string;
  requestedBy: string;
  approvedBy?: string | null;
  originalCheckIn?: string | null;
  correctedCheckIn?: string | null;
  originalCheckOut?: string | null;
  correctedCheckOut?: string | null;
  reason: string;
  decisionNotes?: string | null;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  createdAt: string;
  decidedAt?: string | null;
}

export interface RegularizeAttendanceRequest {
  attendanceRecordId: string;
  correctedCheckIn?: string;
  correctedCheckOut?: string;
  reason: string; // Mínimo 10 caracteres obligatorios
}
```

##### Archivo: `api/attendance.service.ts`
```typescript
import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../shared/api';
import { PageResponse } from '../../user/model/page-response.model';
import {
  AttendanceRecordResponse,
  ClockPunchRequest,
  AttendanceStatus
} from '../model/attendance-record.model';
import {
  AttendanceCorrectionResponse,
  RegularizeAttendanceRequest
} from '../model/attendance-correction.model';

@Injectable({ providedIn: 'root' })
export class AttendanceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/attendance`;

  listRecords(
    startDate: string,
    endDate: string,
    employeeId?: string,
    status?: AttendanceStatus | 'ALL',
    page = 0,
    size = 20
  ): Observable<ApiResponse<PageResponse<AttendanceRecordResponse>>> {
    let params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate)
      .set('page', page)
      .set('size', size);

    if (employeeId) params = params.set('employeeId', employeeId);
    if (status && status !== 'ALL') params = params.set('status', status);

    return this.http.get<ApiResponse<PageResponse<AttendanceRecordResponse>>>(`${this.baseUrl}/records`, { params });
  }

  registerClock(request: ClockPunchRequest): Observable<ApiResponse<AttendanceRecordResponse>> {
    return this.http.post<ApiResponse<AttendanceRecordResponse>>(`${this.baseUrl}/clock`, request);
  }

  regularizeAttendance(request: RegularizeAttendanceRequest): Observable<ApiResponse<AttendanceCorrectionResponse>> {
    return this.http.post<ApiResponse<AttendanceCorrectionResponse>>(`${this.baseUrl}/corrections`, request);
  }

  getTodayStatus(employeeId: string): Observable<ApiResponse<AttendanceRecordResponse | null>> {
    return this.http.get<ApiResponse<AttendanceRecordResponse | null>>(`${this.baseUrl}/today/${employeeId}`);
  }
}
```

---

#### 3.4. HU18: `entities/absence`

##### Archivo: `model/leave-type.model.ts`
```typescript
export interface LeaveTypeResponse {
  id: string;               // UUID
  tenantId: string;         // UUID
  code: string;             // VARCHAR(50) (LEAVE_VACATION, LEAVE_MEDICAL, etc.)
  name: string;             // VARCHAR(100)
  isPaid: boolean;
  requiresApproval: boolean;
  requiresAttachment: boolean;
  maxDaysPerYear?: number | null;
  description?: string | null;
  status: 'ACTIVE' | 'INACTIVE';
}
```

##### Archivo: `model/leave-balance.model.ts`
```typescript
export interface LeaveBalanceResponse {
  id: string;
  tenantId: string;
  employeeId: string;
  leaveTypeId: string;
  leaveTypeName?: string;
  year: number;
  hireDate?: string;
  yearsOfService?: number;
  entitledDays: number; // 15, 20 o 30 según LGT
  usedDays: number;
  pendingDays: number;
  remainingDays: number; // Saldo en tiempo real
  updatedAt: string;
}
```

##### Archivo: `model/leave-request.model.ts`
```typescript
export type LeaveStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

export interface LeaveRequestResponse {
  id: string;               // UUID
  tenantId: string;         // UUID
  employeeId: string;       // UUID
  employeeFullName?: string;
  leaveTypeId: string;      // UUID
  leaveTypeName?: string;
  startDate: string;        // YYYY-MM-DD
  endDate: string;          // YYYY-MM-DD
  totalDays: number;        // Días hábiles computados
  isHourly: boolean;
  startTime?: string | null;
  endTime?: string | null;
  totalHours?: number | null;
  reason: string;
  attachmentUrl?: string | null; // Vinculado con archivo en HU15
  status: LeaveStatus;
  reviewedBy?: string | null;
  reviewedAt?: string | null;
  rejectionReason?: string | null;
  createdAt: string;
}

export interface CreateLeaveRequestPayload {
  employeeId: string;
  leaveTypeId: string;
  startDate: string;
  endDate: string;
  reason: string;
  proofDocument?: File | null;
}

export interface ReviewLeaveRequestPayload {
  requestId: string;
  approved: boolean;
  rejectionReason?: string; // Obligatorio si approved = false (mín 10 caracteres)
}
```

##### Archivo: `api/absence.service.ts`
```typescript
import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../shared/api';
import { PageResponse } from '../../user/model/page-response.model';
import { LeaveTypeResponse } from '../model/leave-type.model';
import { LeaveBalanceResponse } from '../model/leave-balance.model';
import {
  LeaveRequestResponse,
  CreateLeaveRequestPayload,
  ReviewLeaveRequestPayload,
  LeaveStatus
} from '../model/leave-request.model';

@Injectable({ providedIn: 'root' })
export class AbsenceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/leaves`;

  getLeaveTypes(): Observable<ApiResponse<LeaveTypeResponse[]>> {
    return this.http.get<ApiResponse<LeaveTypeResponse[]>>(`${this.baseUrl}/types`);
  }

  getEmployeeBalance(employeeId: string): Observable<ApiResponse<LeaveBalanceResponse>> {
    return this.http.get<ApiResponse<LeaveBalanceResponse>>(`${this.baseUrl}/balances/${employeeId}`);
  }

  listRequests(
    employeeId?: string,
    status?: LeaveStatus | 'ALL',
    page = 0,
    size = 20
  ): Observable<ApiResponse<PageResponse<LeaveRequestResponse>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (employeeId) params = params.set('employeeId', employeeId);
    if (status && status !== 'ALL') params = params.set('status', status);

    return this.http.get<ApiResponse<PageResponse<LeaveRequestResponse>>>(`${this.baseUrl}/requests`, { params });
  }

  submitRequest(payload: CreateLeaveRequestPayload): Observable<ApiResponse<LeaveRequestResponse>> {
    const formData = new FormData();
    formData.append('employeeId', payload.employeeId);
    formData.append('leaveTypeId', payload.leaveTypeId);
    formData.append('startDate', payload.startDate);
    formData.append('endDate', payload.endDate);
    formData.append('reason', payload.reason);
    if (payload.proofDocument) {
      formData.append('proofDocument', payload.proofDocument);
    }

    return this.http.post<ApiResponse<LeaveRequestResponse>>(`${this.baseUrl}/requests`, formData);
  }

  reviewRequest(payload: ReviewLeaveRequestPayload): Observable<ApiResponse<LeaveRequestResponse>> {
    return this.http.patch<ApiResponse<LeaveRequestResponse>>(`${this.baseUrl}/requests/${payload.requestId}/review`, {
      approved: payload.approved,
      rejectionReason: payload.rejectionReason || null
    });
  }
}
```

---

### 4. Criterios de Aceptación y Definición de Terminado (DoD) - Fase 1

#### 4.1. Checklist de Calidad Técnica y Código Limpio (Quality Gate)
- [ ] **Estructura Modular FSD:** Los 4 slices (`employee-file`, `shift`, `attendance`, `absence`) están ubicados exclusivamente dentro de `src/app/entities/`.
- [ ] **Desacoplamiento Estricto:** Ningún archivo de `entities/` importa de `features/`, `widgets/` o `pages/`. Solo de `shared/` y `environments/`.
- [ ] **Exportación Limpia (Barrels):** Cada slice posee un archivo `index.ts` público que exporta sus modelos y su servicio principal, evitando rutas de importación profundas.
- [ ] **Tipado 100% Estricto (Zero Any):** Todas las propiedades coinciden con los tipos SQL de `dateBaseDiagram.md` (`string` para UUID y fechas ISO, `number` para montos, horas y bytes). Prohibido el uso de `any`.
- [ ] **Soporte Dual (Mock / Real):** Los 4 servicios inyectan `HttpClient` y son transparentemente interceptables por `MockHttpInterceptor` cuando `environment.useMocks: true`.
- [ ] **Compilación Verificada Pre-Entrega:** `npm run build` debe finalizar con código de salida `0` sin ningún error de TypeScript.

#### 4.2. Escenarios BDD de Aceptación Funcional

##### Escenario 1: Consumo de Modelos Tipados en Modo Mock
* **DADO QUE** la aplicación está configurada con `environment.useMocks = true`.
* **CUANDO** se invoca el método `ShiftService.listShifts()`.
* **ENTONCES** el servicio retorna un `Observable<ApiResponse<PageResponse<ShiftResponse>>>` con los datos tipados de `hu16-shifts.mock.json`, sin requerir backend activo.

##### Escenario 2: Fidelidad de Atributos con Base de Datos
* **DADO QUE** se recibe una respuesta de la API para `EmployeeDocumentResponse`.
* **CUANDO** el cliente procesa la entidad.
* **ENTONCES** los atributos `fileSizeBytes`, `mimeType`, `documentCategory` y `versionNumber` son mapeados exactamente según el esquema relacional de la tabla `employee_document`.

##### Escenario 3: Manejo Seguro de Paginación y Envolturas
* **DADO QUE** se solicita una lista paginada de registros de asistencia con `AttendanceService.listRecords()`.
* **CUANDO** la respuesta es devuelta por el servidor o mock.
* **ENTONCES** la estructura contiene `success: true`, `data.content` como array tipado y los metadatos `page`, `size`, `totalElements` y `totalPages`.

