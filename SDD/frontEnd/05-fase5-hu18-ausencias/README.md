# SDD - Fase 5: HU18 - Gestión de Ausencias y Vacaciones
## Capas `features/absence-management` y `pages/absences-page`

---

### 1. Resumen de la Historia de Usuario y Reglas de Negocio
* **Actor:** Responsable de Recursos Humanos y Colaborador.
* **Historia:** Registrar, consultar, aprobar, rechazar y justificar permisos y ausencias, indicando motivos, fechas, duración y respaldos, para mantener control de ausencias laborales.
* **Alineación con Base de Datos (`dateBaseDiagram.md` - Dominio 9):**
  * `holiday`: Feriados oficiales (`holiday_date`, `scope: NATIONAL, REGIONAL`) excluidos del cómputo.
  * `leave_type`: Tipos de licencia (`code`, `is_paid`, `requires_attachment`, etc.).
  * `leave_balance`: Saldo por año (`entitled_days`, `used_days`, `pending_days`, `remaining_days`).
  * `leave_request`: Solicitud con `start_date`, `end_date`, `total_days`, `attachment_url` (vinculado a HU15), `rejection_reason`.
* **Reglas Laborales de Bolivia (Ley General del Trabajo - LGT):**
  1. **Escala de Vacaciones por Antigüedad:**
     * 1 a 5 años: **15 días hábiles**.
     * 5 a 10 años: **20 días hábiles**.
     * Más de 10 años: **30 días hábiles**.
  2. **Cómputo Exclusivo de Días Hábiles:** El cálculo de días hábiles descuenta fines de semana y feriados de la tabla `holiday`.
  3. **Validación y Descuento en Tiempo Real:** Las solicitudes de vacación impiden pedir más días de los que indica `remaining_days`.
  4. **Adjunto Probatorio Obligatorio (Vínculo HU15):** Bajas médicas (`LEAVE_MEDICAL`) exigen comprobante que se almacena y clasifica en el expediente del trabajador (`HEALTH`).
  5. **Flujo de Revisión:** Si RRHH o el supervisor rechazan la solicitud, el motivo (`rejection_reason`) es **estrictamente obligatorio** (mínimo 10 caracteres).
  6. **Sincronización con HU17:** Al aprobarse, los días involucrados se marcan como `EXCUSED` (Ausencia justificada) en asistencia.
  7. **Mocks Listos:** Consumo directo de [`hu18-leaves.mock.json`](file:///c:/Users/hp/web/proyectos/proyecto%20U/si2/proyecto_grupalS2_2-2026/hrcore-system/SDD/BackEnd/mocks/hu18-leaves.mock.json).

---

### 2. Estructura Modular de Archivos a Implementar

```
hrcore-web/src/app/
├── features/absence-management/
│   ├── index.ts
│   ├── application/
│   │   ├── absence-list.usecase.ts
│   │   ├── vacation-balance.usecase.ts
│   │   ├── absence-request.usecase.ts
│   │   └── absence-review.usecase.ts
│   └── ui/
│       ├── vacation-balance-card/
│       │   └── vacation-balance-card.component.ts
│       ├── absence-status-badge/
│       │   └── absence-status-badge.component.ts
│       ├── absence-request-modal/
│       │   └── absence-request-modal.component.ts
│       ├── absence-review-modal/
│       │   └── absence-review-modal.component.ts
│       └── absence-table/
│           └── absence-table.component.ts
│
└── pages/absences-page/
    └── absences-page.component.ts
```

---

### 3. Casos de Uso (Application Layer)

#### 3.1. `vacation-balance.usecase.ts`
```typescript
import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AbsenceService, LeaveBalanceResponse } from '../../../entities/absence';

@Injectable({ providedIn: 'root' })
export class VacationBalanceUseCase {
  private readonly absenceService = inject(AbsenceService);

  readonly balance = signal<LeaveBalanceResponse | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  /**
   * Escala de Vacaciones LGT Bolivia según años de antigüedad
   */
  static getBolivianEntitledDays(yearsOfService: number): number {
    if (yearsOfService < 1) return 0;
    if (yearsOfService < 5) return 15;
    if (yearsOfService < 10) return 20;
    return 30;
  }

  async loadBalance(employeeId: string): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const res = await firstValueFrom(this.absenceService.getEmployeeBalance(employeeId));
      this.balance.set(res.data);
    } catch (err: any) {
      this.error.set(err?.error?.message || 'Error al obtener el saldo de vacaciones');
    } finally {
      this.loading.set(false);
    }
  }
}
```

#### 3.2. `absence-request.usecase.ts`
```typescript
import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AbsenceService, CreateLeaveRequestPayload } from '../../../entities/absence';
import { ToastService } from '../../../shared/ui/toast/toast.service';

@Injectable({ providedIn: 'root' })
export class AbsenceRequestUseCase {
  private readonly absenceService = inject(AbsenceService);
  private readonly toastService = inject(ToastService);

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  calculateBusinessDays(startStr: string, endStr: string): number {
    if (!startStr || !endStr) return 0;
    const start = new Date(startStr + 'T00:00:00');
    const end = new Date(endStr + 'T00:00:00');
    if (end < start) return 0;

    let count = 0;
    const cur = new Date(start);
    while (cur <= end) {
      const day = cur.getDay();
      if (day !== 0 && day !== 6) { // Lunes a Viernes
        count++;
      }
      cur.setDate(cur.getDate() + 1);
    }
    return count;
  }

  async submitRequest(
    payload: CreateLeaveRequestPayload,
    isVacation: boolean,
    remainingDays?: number,
    requiresAttachment?: boolean
  ): Promise<boolean> {
    this.error.set(null);

    if (payload.endDate < payload.startDate) {
      this.error.set('La fecha final no puede ser anterior a la inicial');
      return false;
    }

    if (isVacation && remainingDays !== undefined) {
      const neededDays = this.calculateBusinessDays(payload.startDate, payload.endDate);
      if (neededDays > remainingDays) {
        this.error.set(`Los días solicitados (${neededDays}) superan el saldo disponible (${remainingDays} días).`);
        return false;
      }
    }

    if (requiresAttachment && !payload.proofDocument) {
      this.error.set('Este tipo de ausencia requiere adjuntar un documento probatorio obligatorio.');
      return false;
    }

    this.submitting.set(true);
    try {
      await firstValueFrom(this.absenceService.submitRequest(payload));
      this.toastService.success('Solicitud de ausencia registrada correctamente');
      return true;
    } catch (err: any) {
      this.error.set(err?.error?.message || 'Error al enviar la solicitud');
      return false;
    } finally {
      this.submitting.set(false);
    }
  }
}
```

#### 3.3. `absence-review.usecase.ts`
```typescript
import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AbsenceService, ReviewLeaveRequestPayload } from '../../../entities/absence';
import { ToastService } from '../../../shared/ui/toast/toast.service';

@Injectable({ providedIn: 'root' })
export class AbsenceReviewUseCase {
  private readonly absenceService = inject(AbsenceService);
  private readonly toastService = inject(ToastService);

  readonly reviewing = signal(false);
  readonly error = signal<string | null>(null);

  async review(payload: ReviewLeaveRequestPayload): Promise<boolean> {
    if (!payload.approved) {
      if (!payload.rejectionReason || payload.rejectionReason.trim().length < 10) {
        this.error.set('El motivo de rechazo es obligatorio (mínimo 10 caracteres).');
        return false;
      }
    }

    this.reviewing.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(this.absenceService.reviewRequest(payload));
      const msg = payload.approved ? 'Solicitud aprobada exitosamente' : 'Solicitud rechazada';
      this.toastService.success(msg);
      return true;
    } catch (err: any) {
      this.error.set(err?.error?.message || 'Error al procesar la revisión');
      return false;
    } finally {
      this.reviewing.set(false);
    }
  }
}
```

---

### 4. Componentes UI Clave

1. **`vacation-balance-card.component.ts`:**
   * Widget con visualización del saldo en tiempo real (`remainingDays`), días gozados (`usedDays`) y días de derecho legal (`entitledDays`).
   * Barra de progreso porcentual y badge con años de servicio del trabajador.

2. **`absence-review-modal.component.ts`:**
   * Modal para que RRHH o supervisor revise la solicitud, vea el respaldo adjunto (con preview HU15) y apruebe o rechace con justificación obligatoria.

---

### 5. Criterios de Aceptación y Definición de Terminado (DoD) - Fase 5

#### 5.1. Checklist de Calidad Técnica y Código Limpio (Quality Gate)
- [ ] **Componentización y SRP:** La vista de ausencias se modulariza en componentes especializados:
  * `vacation-balance-card.component.ts` (solo widget de saldo de vacaciones, barra de progreso y badges).
  * `absence-request-modal.component.ts` (solo formulario de solicitud, datepicker reactivo y carga de respaldo).
  * `absence-review-modal.component.ts` (modal para decisión de aprobación/rechazo con justificación obligatoria).
  * `absence-status-badge.component.ts` (renderizado semántico: PENDING, APPROVED, REJECTED, CANCELLED).
  * `absence-table.component.ts` (solo listado tabular con acciones según RBAC).
- [ ] **Clean Code & Tipado Estricto:** Prohibido el uso de `any`. Uso riguroso de `LeaveTypeResponse`, `LeaveBalanceResponse`, `LeaveRequestResponse`.
- [ ] **Cálculos Legales Encapsulados:** Escala de vacaciones bolivianas (LGT) y cómputo de días hábiles residen en `VacationBalanceUseCase` y `AbsenceRequestUseCase`.
- [ ] **Reactividad con Signals:** Días hábiles requeridos y validación de saldo en vivo se recalculan mediante `computed()` mientras el usuario escoge fechas.
- [ ] **Verificación Pre-Entrega (Zero-Bug Gate):**
  * `npm run build` ejecutado exitosamente con 0 errores TypeScript.
  * Consola del navegador libre de errores en la subida de archivos de respaldo (`FormData`).
  * Pruebas completas en modo mock (`useMocks: true`) con datos de `hu18-leaves.mock.json`.

#### 5.2. Escenarios BDD de Aceptación Funcional (Gherkin)

##### Escenario 1: Cálculo Exacto de Escala de Vacaciones Bolivianas
* **DADO QUE** un colaborador tiene 3 años de antigüedad en la empresa.
* **CUANDO** el componente `vacation-balance-card` carga su saldo.
* **ENTONCES** el caso de uso computa 15 días anuales por derecho (`entitledDays = 15.0`), muestra los días tomados y refleja en tiempo real el saldo remanente (`remainingDays`). Para un empleado con 7 años, refleja 20 días; y para más de 10 años, 30 días.

##### Escenario 2: Cómputo Exclusivo de Días Hábiles
* **DADO QUE** un colaborador solicita vacaciones desde el viernes 06/11/2026 hasta el martes 10/11/2026.
* **CUANDO** el formulario calcula la duración solicitada.
* **ENTONCES** el sistema descuenta sábado y domingo, computando exactamente 3 días hábiles (viernes, lunes y martes).

##### Escenario 3: Bloqueo de Solicitud de Vacación por Saldo Insuficiente
* **DADO QUE** el colaborador tiene un saldo remanente de 4 días hábiles de vacación.
* **CUANDO** intenta solicitar un periodo que abarca 7 días hábiles.
* **ENTONCES** el validador reactivo muestra: *"Los días solicitados (7) superan el saldo disponible (4 días)"*, e inhabilita el botón de enviar.

##### Escenario 4: Respaldo Probatorio Obligatorio para Baja Médica (Vínculo HU15)
* **DADO QUE** el usuario selecciona el tipo de ausencia `Baja Médica` (`LEAVE_MEDICAL`).
* **CUANDO** deja el campo de comprobante vacío e intenta enviar la solicitud.
* **ENTONCES** el sistema bloquea el envío indicando: *"Este tipo de ausencia requiere adjuntar un documento probatorio obligatorio"*, y al adjuntarlo se clasifica en el expediente del trabajador bajo categoría `HEALTH`.

##### Escenario 5: Rechazo de Solicitud con Justificación Obligatoria
* **DADO QUE** el responsable de RRHH evalúa una solicitud en el modal `absence-review-modal`.
* **CUANDO** hace clic en "Rechazar" sin ingresar un motivo o con menos de 10 caracteres.
* **ENTONCES** el botón de confirmación permanece bloqueado y se resalta el campo de texto con la advertencia: *"El motivo de rechazo es obligatorio (mínimo 10 caracteres)"*.

##### Escenario 6: Sincronización Automática con Asistencia (HU17)
* **DADO QUE** una solicitud de ausencia para el rango 12/10/2026 al 14/10/2026 es aprobada por RRHH.
* **CUANDO** se consulta la asistencia de esas fechas en `attendance-page`.
* **ENTONCES** las jornadas correspondientes se marcan automáticamente con estado `EXCUSED` (Ausencia justificada).

