# SDD - BackEnd: Contratos de Conexión, Endpoints y Mocks JSON
## Módulo de Talento y Control Operativo (HU15, HU16, HU17, HU18)

---

### 1. Propósito y Alcance del Documento

Este documento establece el **contrato de interfaz y conexión entre el Frontend (`hrcore-web`) y el Backend (`hrcore-api`)**.
Garantiza que ambos equipos (Frontend y Backend) trabajen sobre la misma definición canónica de datos, con tipados que corresponden exactamente a las tablas y restricciones relacionales definidas en [`SDD/DataBase/dateBaseDiagram.md`](file:///c:/Users/hp/web/proyectos/proyecto%20U/si2/proyecto_grupalS2_2-2026/hrcore-system/SDD/DataBase/dateBaseDiagram.md).

Además, provee **archivos de datos mockeados en formato `.json`** listos para ser consumidos por el Frontend mientras el Backend completa sus casos de uso.

---

### 2. Convenciones Generales de Conexión y Arquitectura

#### 2.1. Base URL y Proxy
* **URL Base de la API:** `http://localhost:8080/api/v1` (configurado en `hrcore-web/proxy.conf.json`).
* **Prefijo Canónico:** Todos los endpoints de negocio operan bajo `/api/v1/...`.

#### 2.2. Aislamiento Multi-Tenant Estricto
* Toda petición autenticada viaja con el encabezado estándar:
  ```http
  Authorization: Bearer <JWT_TOKEN>
  ```
* El `tenant_id` es extraído automáticamente de los claims del JWT en el interceptor del frontend y validado en el backend como discriminador obligatorio en todas las consultas SQL.

#### 2.3. Estructura Estándar de Respuesta (`ApiResponse<T>`)
Toda respuesta del backend sigue la envoltura oficial del sistema:
```json
{
  "success": true,
  "message": "Operación completada exitosamente",
  "data": { ... },
  "errors": null,
  "timestamp": "2026-10-12T20:00:00Z"
}
```

Para listas paginadas (`PageResponse<T>`):
```json
{
  "success": true,
  "message": "Registros obtenidos",
  "data": {
    "content": [ ... ],
    "page": 0,
    "size": 20,
    "totalElements": 45,
    "totalPages": 3,
    "first": true,
    "last": false
  },
  "errors": null,
  "timestamp": "2026-10-12T20:00:00Z"
}
```

---

### 3. Matriz de Endpoints y Mapeo con Base de Datos

| Historia | Dominio DB | Entidad Principal | Endpoint REST Real (Spring Boot) | Método | Mock / Uso |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **HU15** | Dominio 6 | `EmployeeFile` | `/api/employee-files` | `GET`, `POST` | Listar y crear expedientes |
| **HU15** | Dominio 6 | `EmployeeFile` | `/api/employee-files/by-employee/{empId}` | `GET` | Consultar expediente por empleado |
| **HU15** | Dominio 6 | `EmployeeFile` | `/api/employee-files/{id}` | `GET`, `PUT` | Detalle y edición de expediente |
| **HU15** | Dominio 6 | `EmployeeDocument` | `/api/employee-files/{id}/documents` | `GET`, `POST` | Listar docs y registrar metadatos |
| **HU15** | Dominio 6 | `EmployeeDocument` | `/api/employee-files/{id}/documents/{docId}` | `GET`, `PUT` | Detalle y actualización de metadatos |
| **HU15** | Dominio 6 | `EmployeeDocument` | `/api/employee-files/{id}/documents/{docId}/verify` | `PATCH` | Verificar o rechazar documento |
| **HU15** | Dominio 6 | `EmployeeDocumentVersion` | `/api/employee-files/{id}/documents/{docId}/versions` | `GET`, `POST` | Listar versiones / Subir archivo (Multipart) |
| **HU15** | Dominio 6 | `EmployeeDocumentVersion` | `/api/employee-files/{id}/documents/{docId}/versions/{verId}/download` | `GET` | Descargar versión binaria (Blob) |
| **HU16** | Dominio 7 | `WorkShift` | `/api/shifts` | `GET`, `POST` | Listar y crear turnos de trabajo |
| **HU16** | Dominio 7 | `WorkShift` | `/api/shifts/{id}` | `GET`, `PUT` | Detalle y edición de turno |
| **HU16** | Dominio 7 | `WorkShift` | `/api/shifts/{id}/activate` / `/deactivate` | `PATCH` | Activar o desactivar turno |
| **HU16** | Dominio 7 | `ShiftBreak` | `/api/shifts/{id}/breaks` | `GET`, `POST` | Listar y crear descansos del turno |
| **HU16** | Dominio 7 | `ShiftBreak` | `/api/shifts/{id}/breaks/{breakId}` | `GET`, `PUT`, `DELETE` | CRUD de descansos (almuerzo, pausas) |
| **HU16** | Dominio 7 | `EmployeeShiftAssignment` | `/api/shifts/assignments` | `GET`, `POST` | Listar y crear asignaciones individuales |
| **HU16** | Dominio 7 | `EmployeeShiftAssignment` | `/api/shifts/assignments/{id}/cancel` / `/close` | `PATCH` | Cancelar o cerrar asignación |
| **HU16** | Dominio 7 | `WorkCalendarException` | `/api/shifts/calendar-exceptions` | `GET`, `POST`, `PUT`, `DELETE` | Feriados y excepciones de calendario |
| **HU16** | Dominio 7 | `EffectiveSchedule` | `/api/shifts/schedule?employeeId={uuid}&date={date}` | `GET` | Horario resuelto para asistencia |
| **HU17** | Dominio 8 | `attendance_record` | `/api/v1/attendance/records` | `GET` | [`hu17-attendance.mock.json`](./mocks/hu17-attendance.mock.json) |
| **HU17** | Dominio 8 | `attendance_punch` | `/api/v1/attendance/clock` | `POST` | [`hu17-attendance.mock.json`](./mocks/hu17-attendance.mock.json) |
| **HU17** | Dominio 8 | `attendance_record` | `/api/v1/attendance/today/{empId}` | `GET` | [`hu17-attendance.mock.json`](./mocks/hu17-attendance.mock.json) |
| **HU17** | Dominio 8 | `attendance_correction` | `/api/v1/attendance/corrections` | `POST`, `GET` | [`hu17-attendance.mock.json`](./mocks/hu17-attendance.mock.json) |
| **HU18** | Dominio 9 | `leave_type` | `/api/v1/leaves/types` | `GET` | [`hu18-leaves.mock.json`](./mocks/hu18-leaves.mock.json) |
| **HU18** | Dominio 9 | `leave_balance` | `/api/v1/leaves/balances/{empId}` | `GET` | [`hu18-leaves.mock.json`](./mocks/hu18-leaves.mock.json) |
| **HU18** | Dominio 9 | `leave_request` | `/api/v1/leaves/requests` | `GET`, `POST` | [`hu18-leaves.mock.json`](./mocks/hu18-leaves.mock.json) |
| **HU18** | Dominio 9 | `leave_request` | `/api/v1/leaves/requests/{id}/review` | `PATCH` | [`hu18-leaves.mock.json`](./mocks/hu18-leaves.mock.json) |
| **HU18** | Dominio 9 | `holiday` | `/api/v1/holidays` | `GET` | [`hu18-leaves.mock.json`](./mocks/hu18-leaves.mock.json) |

---

### 4. Detalle de Archivos de Mocks JSON Disponibles

Los archivos JSON se encuentran organizados dentro de `SDD/BackEnd/mocks/`:

1. **[`hu15-dossiers.mock.json`](file:///c:/Users/hp/web/proyectos/proyecto%20U/si2/proyecto_grupalS2_2-2026/hrcore-system/SDD/BackEnd/mocks/hu15-dossiers.mock.json):**
   * Catálogo de tipos de documento (`document_type`) con sus categorías exactas (`IDENTIFICATION`, `CONTRACT`, `EDUCATION`, `HEALTH`, `DISCIPLINARY`, `ASSET`).
   * Listado de documentos del empleado (`employee_document`) con tamaños reales, tipos MIME (`application/pdf`, etc.) y URLs de descarga simuladas.
   * Registro histórico de auditoría inmutable (`employee_document_audit`) con acciones `UPLOAD`, `VIEW`, `DOWNLOAD`.

2. **[`hu16-shifts.mock.json`](file:///c:/Users/hp/web/proyectos/proyecto%20U/si2/proyecto_grupalS2_2-2026/hrcore-system/SDD/BackEnd/mocks/hu16-shifts.mock.json):**
   * Turnos de trabajo parametrizados (`CONTINUOUS`, `NIGHT`, `ROTATING`) respetando límites LGT (8 horas diurnas, 7 horas nocturnas).
   * Días laborables por turno (`shift_day`) del 1 al 7.
   * Asignaciones de turno a empleados (`employee_shift_assignment`) individuales y recurrentes.

3. **[`hu17-attendance.mock.json`](file:///c:/Users/hp/web/proyectos/proyecto%20U/si2/proyecto_grupalS2_2-2026/hrcore-system/SDD/BackEnd/mocks/hu17-attendance.mock.json):**
   * Registros consolidados de jornada (`attendance_record`) con estados `PRESENT`, `LATE`, `ABSENT`.
   * Marcaciones biométricas/web (`attendance_punch`) con timestamp ISO, coordenadas GPS y tipo `IN` / `OUT`.
   * Solicitudes de regularización manual (`attendance_correction`) con justificaciones auditadas.

4. **[`hu18-leaves.mock.json`](file:///c:/Users/hp/web/proyectos/proyecto%20U/si2/proyecto_grupalS2_2-2026/hrcore-system/SDD/BackEnd/mocks/hu18-leaves.mock.json):**
   * Tipos de licencia y permiso (`leave_type`) con banderas `requires_attachment` e `is_paid`.
   * Saldo de vacaciones (`leave_balance`) computando años de antigüedad, días de derecho LGT (`entitled_days`), días usados (`used_days`) y saldo libre (`remaining_days`).
   * Solicitudes de ausencia (`leave_request`) en estados `PENDING` y `APPROVED`.
   * Calendario de feriados nacionales (`holiday`) para excluir del cómputo de días hábiles.

---

### 5. Mecanismo de Activación / Desactivación del Mock en Frontend

En el Frontend (`hrcore-web`), el desarrollador controla si consume los mocks o el backend real mediante una sola variable en `src/environments/environment.ts`:

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
  useMocks: true // <-- CAMBIAR A 'false' cuando el backend esté desplegado
};
```

Cuando `useMocks === true`:
* Un `MockHttpInterceptor` intercepta automáticamente las llamadas dirigidas a `/api/v1/document-types`, `/api/v1/shifts`, `/api/v1/attendance`, `/api/v1/leaves`, etc.
* Responde inmediatamente con los datos contenidos en los mocks JSON simulando 250ms de latencia de red.
* Permite crear, modificar y eliminar datos en memoria durante la sesión del navegador.

Cuando `useMocks === false`:
* La petición viaja sin interferencia hacia el backend Spring Boot a través de `proxy.conf.json`.
