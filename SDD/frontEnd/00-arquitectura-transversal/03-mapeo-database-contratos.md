# Mapeo Exhaustivo: Base de Datos Relacional a Modelos TypeScript
## Alineación con `SDD/DataBase/dateBaseDiagram.md` (Dominios 6, 7, 8 y 9)

---

### 1. Principio de Consistencia de Datos

Para evitar discrepancias entre el backend (Spring Boot / JPA / PostgreSQL) y el frontend (Angular 20), los modelos y DTOs del frontend respetan con total fidelidad los nombres, tipos de dato y restricciones de integridad definidas en el script DDL de la base de datos [`dateBaseDiagram.md`](file:///c:/Users/hp/web/proyectos/proyecto%20U/si2/proyecto_grupalS2_2-2026/hrcore-system/SDD/DataBase/dateBaseDiagram.md).

* **Convención de serialización JSON:** Las propiedades en los DTOs de TypeScript se manejan en `camelCase` (estándar de Spring Boot con Jackson `PropertyNamingStrategies.LOWER_CAMEL_CASE` o configurado con `@JsonProperty`).
* **Tipos de datos:**
  * `UUID` en Postgres -> `string` en TypeScript.
  * `DATE` en Postgres -> `string` en formato ISO `YYYY-MM-DD`.
  * `TIMESTAMP WITH TIME ZONE` -> `string` en formato ISO `YYYY-MM-DDTHH:mm:ssZ`.
  * `TIME` en Postgres -> `string` en formato `HH:mm:ss` o `HH:mm`.
  * `NUMERIC(p,s)` / `BIGINT` / `INTEGER` -> `number` en TypeScript.
  * `BOOLEAN` -> `boolean` en TypeScript.

---

### 2. HU15: Dominio 6 - Expediente Laboral Digital

#### 2.1. Tabla: `document_type`
| Columna SQL | Tipo Postgres | DTO TypeScript (`camelCase`) | Tipo TS | Restricción / Valores Válidos |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | `id` | `string` | Clave primaria |
| `tenant_id` | `UUID` | `tenantId` | `string` | Multi-tenant estricto |
| `name` | `VARCHAR(100)` | `name` | `string` | Nombre descriptivo del documento |
| `code` | `VARCHAR(50)` | `code` | `string` | Código único por tenant (ej. `DOC_CI`) |
| `category` | `VARCHAR(50)` | `category` | `DocumentCategory` | `'IDENTIFICATION' \| 'CONTRACT' \| 'EDUCATION' \| 'HEALTH' \| 'DISCIPLINARY' \| 'ASSET'` |
| `is_mandatory` | `BOOLEAN` | `isMandatory` | `boolean` | Requerido en contratación |
| `has_expiration` | `BOOLEAN` | `hasExpiration` | `boolean` | Dispara alerta de renovación |
| `status` | `VARCHAR(20)` | `status` | `'ACTIVE' \| 'INACTIVE'` | Estado del tipo |

#### 2.2. Tabla: `employee_document`
| Columna SQL | Tipo Postgres | DTO TypeScript (`camelCase`) | Tipo TS | Observaciones |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | `id` | `string` | Clave primaria |
| `tenant_id` | `UUID` | `tenantId` | `string` | Multi-tenant |
| `employee_id` | `UUID` | `employeeId` | `string` | Llave foránea `employee(id)` |
| `document_type_id` | `UUID` | `documentTypeId` | `string` | Llave foránea `document_type(id)` |
| `document_number` | `VARCHAR(100)` | `documentNumber` | `string \| null` | Nro de cédula, contrato, etc. |
| `file_name` | `VARCHAR(255)` | `fileName` | `string` | Nombre original del archivo |
| `file_path` | `TEXT` | `filePath` | `string` | Ruta en almacenamiento seguro (S3/MinIO) |
| `file_size_bytes` | `BIGINT` | `fileSizeBytes` | `number` | Máx 10 MB (10,485,760 bytes) |
| `mime_type` | `VARCHAR(100)` | `mimeType` | `string` | `application/pdf`, `image/jpeg`, etc. |
| `version_number` | `INTEGER` | `versionNumber` | `number` | Control de versiones (default 1) |
| `issue_date` | `DATE` | `issueDate` | `string \| null` | Fecha de emisión |
| `expiration_date` | `DATE` | `expirationDate` | `string \| null` | Fecha de vencimiento |
| `status` | `VARCHAR(20)` | `status` | `'ACTIVE' \| 'EXPIRED' \| 'ARCHIVED'` | Soft delete / vigencia |
| `verified_by` | `UUID` | `verifiedBy` | `string \| null` | Usuario que validó el documento |
| `verified_at` | `TIMESTAMPTZ` | `verifiedAt` | `string \| null` | Fecha/hora de validación |
| `created_at` | `TIMESTAMPTZ` | `createdAt` | `string` | Fecha de carga |
| `updated_at` | `TIMESTAMPTZ` | `updatedAt` | `string` | Fecha de última modificación |

#### 2.3. Tabla: `employee_document_audit`
| Columna SQL | Tipo Postgres | DTO TypeScript | Tipo TS | Valores Válidos |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | `id` | `string` | Clave primaria |
| `document_id` | `UUID` | `documentId` | `string` | Llave foránea a documento |
| `user_id` | `UUID` | `userId` | `string` | Usuario que realizó la acción |
| `action` | `VARCHAR(50)` | `action` | `DocumentAuditAction` | `'UPLOAD' \| 'VIEW' \| 'DOWNLOAD' \| 'UPDATE' \| 'ARCHIVE'` |
| `performed_at` | `TIMESTAMPTZ` | `performedAt` | `string` | Timestamp inmutable |
| `ip_address` | `VARCHAR(45)` | `ipAddress` | `string \| null` | IPv4 / IPv6 del cliente |

---

### 3. HU16: Dominio 7 - Parametrización y Asignación de Turnos

#### 3.1. Tabla: `shift`
| Columna SQL | Tipo Postgres | DTO TypeScript | Tipo TS | Restricción / Valores Válidos |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | `id` | `string` | Clave primaria |
| `tenant_id` | `UUID` | `tenantId` | `string` | Multi-tenant |
| `code` | `VARCHAR(50)` | `code` | `string` | Código de turno |
| `name` | `VARCHAR(100)` | `name` | `string` | Nombre legible |
| `shift_type` | `VARCHAR(30)` | `shiftType` | `ShiftType` | `'CONTINUOUS' \| 'SPLIT' \| 'ROTATING' \| 'NIGHT'` |
| `start_time` | `TIME` | `startTime` | `string` | Hora de inicio (`HH:mm:ss`) |
| `end_time` | `TIME` | `endTime` | `string` | Hora de finalización (`HH:mm:ss`) |
| `break_minutes` | `INTEGER` | `breakMinutes` | `number` | Tiempo de descanso (default 0) |
| `grace_period_minutes`| `INTEGER`| `gracePeriodMinutes` | `number` | Tolerancia puntualidad (5 a 15 min) |
| `is_overnight` | `BOOLEAN` | `isOvernight` | `boolean` | Cruza medianoche (+1 día) |
| `status` | `VARCHAR(20)` | `status` | `'ACTIVE' \| 'INACTIVE'` | Baja lógica si tiene historial |

#### 3.2. Tabla: `shift_day`
| Columna SQL | Tipo Postgres | DTO TypeScript | Tipo TS | Valores Válidos |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | `id` | `string` | Clave primaria |
| `shift_id` | `UUID` | `shiftId` | `string` | Turno padre |
| `day_of_week` | `INTEGER` | `dayOfWeek` | `number` | 1: Lunes, 2: Martes, ..., 7: Domingo |
| `is_work_day` | `BOOLEAN` | `isWorkDay` | `boolean` | Si es día laborable |
| `start_time` | `TIME` | `startTime` | `string \| null` | Horario específico de ese día |
| `end_time` | `TIME` | `endTime` | `string \| null` | Horario fin de ese día |
| `break_minutes` | `INTEGER` | `breakMinutes` | `number` | Minutos de receso |

#### 3.3. Tabla: `employee_shift_assignment`
| Columna SQL | Tipo Postgres | DTO TypeScript | Tipo TS | Valores Válidos |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | `id` | `string` | Clave primaria |
| `tenant_id` | `UUID` | `tenantId` | `string` | Multi-tenant |
| `employee_id` | `UUID` | `employeeId` | `string` | Llave foránea `employee(id)` |
| `shift_id` | `UUID` | `shiftId` | `string` | Llave foránea `shift(id)` |
| `start_date` | `DATE` | `startDate` | `string` | Fecha inicio de vigencia (`YYYY-MM-DD`) |
| `end_date` | `DATE` | `endDate` | `string \| null` | Fin de vigencia (null = indefinido) |
| `is_recurring` | `BOOLEAN` | `isRecurring` | `boolean` | Si se repite semanalmente |
| `status` | `VARCHAR(20)` | `status` | `'ACTIVE' \| 'INACTIVE'` | Estado de la asignación |

---

### 4. HU17: Dominio 8 - Control de Asistencia y Marcaciones

#### 4.1. Tabla: `attendance_record`
| Columna SQL | Tipo Postgres | DTO TypeScript | Tipo TS | Valores Válidos |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | `id` | `string` | Clave primaria |
| `tenant_id` | `UUID` | `tenantId` | `string` | Multi-tenant |
| `employee_id` | `UUID` | `employeeId` | `string` | Empleado |
| `shift_id` | `UUID` | `shiftId` | `string \| null` | Turno de la jornada |
| `work_date` | `DATE` | `workDate` | `string` | Fecha de jornada (`YYYY-MM-DD`) |
| `check_in_time` | `TIMESTAMPTZ` | `checkInTime` | `string \| null` | Marcación de entrada real (servidor) |
| `check_out_time` | `TIMESTAMPTZ` | `checkOutTime` | `string \| null` | Marcación de salida real (servidor) |
| `worked_hours` | `NUMERIC(5,2)` | `workedHours` | `number` | Horas ordinarias computadas |
| `overtime_hours` | `NUMERIC(5,2)` | `overtimeHours` | `number` | Horas extraordinarias computadas |
| `late_minutes` | `INTEGER` | `lateMinutes` | `number` | Minutos de retraso acumulados |
| `early_departure_minutes`| `INTEGER` | `earlyDepartureMinutes`| `number` | Salida anticipada en minutos |
| `status` | `VARCHAR(30)` | `status` | `AttendanceStatus` | `'PRESENT' \| 'LATE' \| 'EARLY_DEPARTURE' \| 'ABSENT' \| 'EXCUSED' \| 'HOLIDAY' \| 'REST_DAY'` |
| `source` | `VARCHAR(20)` | `source` | `PunchSource` | `'WEB' \| 'MOBILE' \| 'BIOMETRIC' \| 'MANUAL'` |
| `latitude` | `NUMERIC(10,7)`| `latitude` | `number \| null` | Coordenada GPS capturada |
| `longitude` | `NUMERIC(10,7)`| `longitude` | `number \| null` | Coordenada GPS capturada |

#### 4.2. Tabla: `attendance_punch`
| Columna SQL | Tipo Postgres | DTO TypeScript | Tipo TS | Valores Válidos |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | `id` | `string` | Clave primaria |
| `attendance_record_id` | `UUID` | `attendanceRecordId` | `string` | Jornada asociada |
| `punch_time` | `TIMESTAMPTZ` | `punchTime` | `string` | Sello de tiempo oficial |
| `punch_type` | `VARCHAR(20)` | `punchType` | `'IN' \| 'OUT' \| 'BREAK_OUT' \| 'BREAK_IN'` | Tipo de marcación |
| `source` | `VARCHAR(20)` | `source` | `'WEB' \| 'MOBILE' \| 'BIOMETRIC'` | Canal |
| `device_id` | `VARCHAR(100)` | `deviceId` | `string \| null` | Identificador de dispositivo/navegador |
| `latitude` / `longitude` | `NUMERIC(10,7)`| `latitude` / `longitude` | `number \| null` | Coordenadas |

#### 4.3. Tabla: `attendance_correction`
| Columna SQL | Tipo Postgres | DTO TypeScript | Tipo TS | Valores Válidos |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | `id` | `string` | Clave primaria |
| `attendance_record_id` | `UUID` | `attendanceRecordId` | `string` | Registro a regularizar |
| `requested_by` | `UUID` | `requestedBy` | `string` | Solicitante |
| `approved_by` | `UUID` | `approvedBy` | `string \| null` | Aprobador (RRHH) |
| `original_check_in` / `corrected_check_in` | `TIMESTAMPTZ` | `originalCheckIn` / `correctedCheckIn` | `string \| null` | Corrección entrada |
| `original_check_out` / `corrected_check_out`| `TIMESTAMPTZ` | `originalCheckOut` / `correctedCheckOut` | `string \| null` | Corrección salida |
| `reason` | `TEXT` | `reason` | `string` | Justificación obligatoria |
| `status` | `VARCHAR(20)` | `status` | `'PENDING' \| 'APPROVED' \| 'REJECTED'` | Estado |

---

### 5. HU18: Dominio 9 - Feriados, Ausencias y Vacaciones

#### 5.1. Tabla: `leave_type`
| Columna SQL | Tipo Postgres | DTO TypeScript | Tipo TS | Valores Válidos |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | `id` | `string` | Clave primaria |
| `code` | `VARCHAR(50)` | `code` | `string` | Código (`LEAVE_VACATION`, etc.) |
| `name` | `VARCHAR(100)` | `name` | `string` | Nombre legible |
| `is_paid` | `BOOLEAN` | `isPaid` | `boolean` | Con / sin goce de haberes |
| `requires_approval` | `BOOLEAN` | `requiresApproval` | `boolean` | Flujo de aprobación obligatorio |
| `requires_attachment`| `BOOLEAN` | `requiresAttachment`| `boolean` | Exige archivo probatorio (HU15) |
| `max_days_per_year` | `NUMERIC(4,1)`| `maxDaysPerYear` | `number \| null` | Límite anual si aplica |
| `status` | `VARCHAR(20)` | `status` | `'ACTIVE' \| 'INACTIVE'` | Estado |

#### 5.2. Tabla: `leave_balance`
| Columna SQL | Tipo Postgres | DTO TypeScript | Tipo TS | Valores Válidos |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | `id` | `string` | Clave primaria |
| `employee_id` | `UUID` | `employeeId` | `string` | Colaborador |
| `leave_type_id` | `UUID` | `leaveTypeId` | `string` | Tipo de licencia |
| `year` | `INTEGER` | `year` | `number` | Gestión (ej. 2026) |
| `entitled_days` | `NUMERIC(4,1)`| `entitledDays` | `number` | Días por derecho legal (15, 20, 30) |
| `used_days` | `NUMERIC(4,1)`| `usedDays` | `number` | Días ya gozados |
| `pending_days` | `NUMERIC(4,1)`| `pendingDays` | `number` | Días en solicitudes pendientes |
| `remaining_days` | `NUMERIC(4,1)`| `remainingDays` | `number` | Saldo disponible en tiempo real |

#### 5.3. Tabla: `leave_request`
| Columna SQL | Tipo Postgres | DTO TypeScript | Tipo TS | Valores Válidos |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | `id` | `string` | Clave primaria |
| `employee_id` | `UUID` | `employeeId` | `string` | Colaborador |
| `leave_type_id` | `UUID` | `leaveTypeId` | `string` | Tipo de ausencia |
| `start_date` / `end_date` | `DATE` | `startDate` / `endDate` | `string` | Rango de fechas (`YYYY-MM-DD`) |
| `total_days` | `NUMERIC(4,1)`| `totalDays` | `number` | Solo días hábiles computados |
| `is_hourly` | `BOOLEAN` | `isHourly` | `boolean` | Si es permiso por horas |
| `reason` | `TEXT` | `reason` | `string` | Motivo de la solicitud |
| `attachment_url` | `TEXT` | `attachmentUrl` | `string \| null` | Enlace al archivo HU15 |
| `status` | `VARCHAR(20)` | `status` | `LeaveStatus` | `'PENDING' \| 'APPROVED' \| 'REJECTED' \| 'CANCELLED'` |
| `reviewed_by` | `UUID` | `reviewedBy` | `string \| null` | Revisor |
| `rejection_reason` | `TEXT` | `rejectionReason` | `string \| null` | Motivo obligatorio en rechazo |

#### 5.4. Tabla: `holiday`
| Columna SQL | Tipo Postgres | DTO TypeScript | Tipo TS | Valores Válidos |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | `id` | `string` | Clave primaria |
| `name` | `VARCHAR(100)` | `name` | `string` | Nombre del feriado |
| `holiday_date` | `DATE` | `holidayDate` | `string` | Fecha oficial (`YYYY-MM-DD`) |
| `is_recurring` | `BOOLEAN` | `isRecurring` | `boolean` | Se repite anualmente |
| `scope` | `VARCHAR(20)` | `scope` | `'NATIONAL' \| 'REGIONAL'` | Alcance |
