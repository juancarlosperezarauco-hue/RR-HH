# SDD - Fase 4: HU17 - Gestión y Control de Asistencia
## Capas `features/attendance-management` y `pages/attendance-page`

---

### 1. Resumen de la Historia de Usuario y Reglas de Negocio
* **Actor:** Responsable de Recursos Humanos y Colaborador.
* **Historia:** Registrar, consultar y gestionar entradas y salidas, identificando retrasos, faltas y horas trabajadas, permitiendo correcciones justificadas, para controlar el cumplimiento de jornadas.
* **Alineación con Base de Datos (`dateBaseDiagram.md` - Dominios 3 y 8):**
  * `work_location`: Coordenadas `latitude`, `longitude` y `radius_meters` (ej. 100m) para validación de presencia física.
  * `attendance_record`: Estado (`PRESENT`, `LATE`, `EARLY_DEPARTURE`, `ABSENT`, `EXCUSED`, `HOLIDAY`, `REST_DAY`), horas ordinarias y extras calculadas.
  * `attendance_punch`: Marcaciones individuales (`IN`, `OUT`, `BREAK_OUT`, `BREAK_IN`) selladas con timestamp del servidor.
  * `attendance_correction`: Regularización manual auditada con justificación obligatoria.
* **Reglas Clave:**
  1. **Timestamp Server-Side:** La hora oficial de marcación siempre es generada y sellada por el backend.
  2. **Geolocalización contra Radio de Sucursal:** Captura de coordenadas GPS (`navigator.geolocation`) y validación de proximidad a la sucursal asignada.
  3. **Debounce Anti-Doble Marcación:** Bloqueo preventivo de marcaciones duplicadas realizadas en **menos de 2 minutos** (120 segundos) mediante almacenamiento local y temporizador visual.
  4. **Regularización Manual Auditada:** Cualquier ajuste manual sobre una marcación requiere **motivo obligatorio** (mínimo 10 caracteres) y conserva el registro previo de forma inmutable.
  5. **Mocks Listos:** Consumo directo de [`hu17-attendance.mock.json`](file:///c:/Users/hp/web/proyectos/proyecto%20U/si2/proyecto_grupalS2_2-2026/hrcore-system/SDD/BackEnd/mocks/hu17-attendance.mock.json).

---

### 2. Estructura Modular de Archivos a Implementar

```
hrcore-web/src/app/
├── features/attendance-management/
│   ├── index.ts
│   ├── application/
│   │   ├── attendance-list.usecase.ts
│   │   ├── attendance-clock.usecase.ts
│   │   └── attendance-regularize.usecase.ts
│   └── ui/
│       ├── clock-in-widget/
│       │   └── clock-in-widget.component.ts
│       ├── attendance-status-badge/
│       │   └── attendance-status-badge.component.ts
│       ├── attendance-regularize-dialog/
│       │   └── attendance-regularize-dialog.component.ts
│       └── attendance-table/
│           └── attendance-table.component.ts
│
└── pages/attendance-page/
    └── attendance-page.component.ts
```

---

### 3. Casos de Uso y Lógica de Negocio

#### 3.1. `attendance-clock.usecase.ts`
* **Geolocalización, Debounce de 2 Minutos y Marcación:**
```typescript
import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AttendanceService, AttendanceRecordResponse } from '../../../entities/attendance';
import { ToastService } from '../../../shared/ui/toast/toast.service';

@Injectable({ providedIn: 'root' })
export class AttendanceClockUseCase {
  private readonly attendanceService = inject(AttendanceService);
  private readonly toastService = inject(ToastService);

  private readonly DEBOUNCE_KEY = 'hrcore_last_clock_timestamp';
  private readonly DEBOUNCE_MS = 2 * 60 * 1000; // 2 minutos

  readonly clocking = signal(false);
  readonly error = signal<string | null>(null);
  readonly remainingDebounceSeconds = signal(0);

  constructor() {
    this.checkDebounceTimer();
  }

  private checkDebounceTimer(): void {
    const lastTimestamp = Number(localStorage.getItem(this.DEBOUNCE_KEY) || 0);
    const elapsed = Date.now() - lastTimestamp;
    if (elapsed < this.DEBOUNCE_MS) {
      this.remainingDebounceSeconds.set(Math.ceil((this.DEBOUNCE_MS - elapsed) / 1000));
      const interval = setInterval(() => {
        const remaining = this.remainingDebounceSeconds() - 1;
        if (remaining <= 0) {
          this.remainingDebounceSeconds.set(0);
          clearInterval(interval);
        } else {
          this.remainingDebounceSeconds.set(remaining);
        }
      }, 1000);
    }
  }

  async captureCoordinates(): Promise<{ latitude?: number; longitude?: number }> {
    if (!('geolocation' in navigator)) return {};
    return new Promise((resolve) => {
      navigator.geolocation.getCurrentPosition(
        (pos) => resolve({ latitude: pos.coords.latitude, longitude: pos.coords.longitude }),
        () => resolve({}), // Fallback amigable si deniega permisos
        { timeout: 5000, enableHighAccuracy: true }
      );
    });
  }

  async registerClock(employeeId: string, punchType: 'IN' | 'OUT'): Promise<AttendanceRecordResponse | null> {
    if (this.remainingDebounceSeconds() > 0) {
      this.toastService.warning(`Por favor espere ${this.remainingDebounceSeconds()}s antes de volver a marcar.`);
      return null;
    }

    this.clocking.set(true);
    this.error.set(null);
    try {
      const coords = await this.captureCoordinates();
      const res = await firstValueFrom(this.attendanceService.registerClock({
        employeeId,
        type: punchType,
        method: 'WEB',
        latitude: coords.latitude,
        longitude: coords.longitude
      }));

      // Iniciar debounce
      localStorage.setItem(this.DEBOUNCE_KEY, Date.now().toString());
      this.checkDebounceTimer();

      const label = punchType === 'IN' ? 'Entrada' : 'Salida';
      this.toastService.success(`Marcación de ${label} registrada correctamente.`);
      return res.data;
    } catch (err: any) {
      const msg = err?.error?.message || 'Error al registrar marcación';
      this.error.set(msg);
      this.toastService.error(msg);
      return null;
    } finally {
      this.clocking.set(false);
    }
  }
}
```

#### 3.2. `attendance-regularize.usecase.ts`
```typescript
import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AttendanceService, RegularizeAttendanceRequest } from '../../../entities/attendance';
import { ToastService } from '../../../shared/ui/toast/toast.service';

@Injectable({ providedIn: 'root' })
export class AttendanceRegularizeUseCase {
  private readonly attendanceService = inject(AttendanceService);
  private readonly toastService = inject(ToastService);

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  async regularize(request: RegularizeAttendanceRequest): Promise<boolean> {
    if (!request.reason || request.reason.trim().length < 10) {
      this.error.set('La justificación es obligatoria y debe tener al menos 10 caracteres.');
      return false;
    }

    this.submitting.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(this.attendanceService.regularizeAttendance(request));
      this.toastService.success('Regularización de asistencia enviada exitosamente.');
      return true;
    } catch (err: any) {
      this.error.set(err?.error?.message || 'Error al procesar la regularización.');
      return false;
    } finally {
      this.submitting.set(false);
    }
  }
}
```

---

### 4. Componentes UI Clave

1. **`clock-in-widget.component.ts`:**
   * Reloj digital reactivo en tiempo real (`HH:mm:ss`) con fecha actual en español.
   * Botón dinámico "Marcar Entrada" / "Marcar Salida".
   * Indicador visual de bloqueo por debounce con cuenta regresiva en segundos.
   * Indicador de geolocalización GPS (icono de pin verde si fue capturado).

2. **`attendance-regularize-dialog.component.ts`:**
   * Formulario modal para justificar corrección de hora de entrada o salida.
   * Campo de motivo obligatorio con contador de caracteres (`(10 caracteres mín.)`).
   * Alerta de auditoría: "Esta modificación registrará un evento inmutable en el historial".

---

### 5. Criterios de Aceptación y Definición de Terminado (DoD) - Fase 4

#### 5.1. Checklist de Calidad Técnica y Código Limpio (Quality Gate)
- [ ] **Componentización y SRP:** La vista de control de asistencia se divide en componentes modulares:
  * `clock-in-widget.component.ts` (solo reloj en vivo, botón de acción y debounce local).
  * `attendance-table.component.ts` (solo visualización de registros diarios, filtros y paginación).
  * `attendance-status-badge.component.ts` (renderizado semántico de estados: PRESENT, LATE, ABSENT, etc.).
  * `attendance-regularize-dialog.component.ts` (formulario modal para justificación de corrección).
- [ ] **Clean Code & Tipado Estricto:** Utiliza las interfaces `AttendanceRecordResponse`, `ClockPunchRequest`, `AttendanceCorrectionResponse`. Cero tipos `any`.
- [ ] **Encapsulamiento en Casos de Uso:** Geolocation API, temporizador de debounce en `localStorage`, cálculo de horas y regularización residen en `AttendanceClockUseCase` y `AttendanceRegularizeUseCase`.
- [ ] **Reactividad con Signals:** El conteo regresivo de debounce (`remainingDebounceSeconds`) y el estado de la marcación se administran mediante `signal<number>` y `signal<boolean>`.
- [ ] **Verificación Pre-Entrega (Zero-Bug Gate):**
  * `npm run build` ejecutado exitosamente con 0 errores TypeScript.
  * Consola del navegador libre de errores si el usuario rechaza los permisos de geolocalización (fallback graceful a coordenadas vacías).
  * Pruebas completas en modo mock (`useMocks: true`) con datos de `hu17-attendance.mock.json`.

#### 5.2. Escenarios BDD de Aceptación Funcional (Gherkin)

##### Escenario 1: Marcación Exitosa de Entrada con Geolocalización
* **DADO QUE** un colaborador con permiso `attendance.clock` accede al widget de asistencia.
* **CUANDO** presiona el botón "Marcar Entrada" y el navegador solicita geolocalización.
* **ENTONCES** el caso de uso captura latitud y longitud, envía la petición al backend/mock, recibe la confirmación con sello de tiempo del servidor (`checkInTime`), cambia el botón a "Marcar Salida" e inicia el temporizador de debounce de 120 segundos.

##### Escenario 2: Bloqueo Preventivo por Debounce (Menos de 2 minutos)
* **DADO QUE** un colaborador acaba de registrar una marcación hace 45 segundos.
* **CUANDO** observa el widget de marcación.
* **ENTONCES** el botón de marcar permanece inhabilitado, mostrando una cuenta regresiva visual: *"Espere 75s para volver a marcar"*, y no se emite ninguna llamada HTTP si intenta hacer clic.

##### Escenario 3: Clasificación Automática de Retraso contra HU16
* **DADO QUE** el empleado tiene asignado un turno con entrada a las 08:30 y tolerancia de 10 minutos (hasta las 08:40).
* **CUANDO** el registro de entrada del servidor marca las 08:48:00.
* **ENTONCES** el sistema clasifica la jornada con estado `LATE`, calcula 18 minutos de retraso (`lateMinutes = 18`) y renderiza el badge ámbar correspondiente en la tabla.

##### Escenario 4: Regularización Manual Exitosa con Justificación
* **DADO QUE** un registro de asistencia presenta una omisión de marcación o retraso justificado.
* **CUANDO** el responsable de RRHH abre el diálogo de regularización, ingresa la hora corregida y escribe una justificación detallada de 25 caracteres.
* **ENTONCES** el caso de uso valida que la justificación tiene $\ge 10$ caracteres, envía la corrección auditada y notifica: *"Regularización enviada exitosamente"*.

##### Escenario 5: Rechazo de Regularización con Justificación Insuficiente
* **DADO QUE** el usuario ingresa una justificación de 4 caracteres (ej: "olvido").
* **CUANDO** intenta presionar el botón "Guardar Regularización".
* **ENTONCES** el validador reactivo bloquea el envío y muestra en rojo: *"La justificación es obligatoria y debe tener al menos 10 caracteres"*.

