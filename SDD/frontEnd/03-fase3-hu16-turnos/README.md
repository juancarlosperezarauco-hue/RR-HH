# SDD - Fase 3: HU16 - Parametrización y Asignación de Turnos
## Capas `features/shift-management` y `pages/shifts-page`

---

### 1. Resumen de la Historia de Usuario y Reglas de Negocio
* **Actor:** Responsable de Recursos Humanos.
* **Historia:** Crear, consultar, modificar y desactivar turnos de trabajo, definiendo horarios, días laborales y descansos, así como asignarlos a trabajadores, para organizar jornadas y facilitar el control de asistencia.
* **Alineación con Base de Datos (`dateBaseDiagram.md` - Dominio 7):**
  * `shift`: `shift_type` (`CONTINUOUS`, `SPLIT`, `ROTATING`, `NIGHT`), `start_time`, `end_time`, `break_minutes`, `grace_period_minutes` (5 a 15 min), `is_overnight`.
  * `shift_day`: Configuración por día (1=Lunes a 7=Domingo).
  * `employee_shift_assignment`: Asignación con `start_date`, `end_date`, `is_recurring`, `status`.
* **Reglas Laborales de Bolivia (Ley General del Trabajo - LGT):**
  1. **Jornada Diurna Máxima:** 8 horas/día; 48 horas/semana (varones); 40 horas/semana (mujeres).
  2. **Jornada Nocturna:** Máximo 7 horas entre las 20:00 y las 06:00.
  3. **Cruce de Medianoche:** Identificación automática de turnos nocturnos (`is_overnight = true`, ej. 22:00 a 05:00) calculando la jornada sobre el cambio de día (+1).
  4. **Tolerancia:** Parametrizable entre 5 y 15 minutos (por defecto 10 min).
  5. **Prevención de Solapamientos:** Validación reactiva que impide asignar a un trabajador dos turnos activos que coincidan en fechas.
  6. **Mocks Listos:** Consumo directo de [`hu16-shifts.mock.json`](file:///c:/Users/hp/web/proyectos/proyecto%20U/si2/proyecto_grupalS2_2-2026/hrcore-system/SDD/BackEnd/mocks/hu16-shifts.mock.json).

---

### 2. Estructura Modular de Archivos a Implementar

```
hrcore-web/src/app/
├── features/shift-management/
│   ├── index.ts
│   ├── application/
│   │   ├── shift-list.usecase.ts
│   │   ├── shift-form.usecase.ts
│   │   └── shift-assignment.usecase.ts
│   └── ui/
│       ├── shift-card/
│       │   └── shift-card.component.ts
│       ├── shift-form-modal/
│       │   └── shift-form-modal.component.ts
│       ├── shift-assign-modal/
│       │   └── shift-assign-modal.component.ts
│       ├── shift-status-badge/
│       │   └── shift-status-badge.component.ts
│       └── shift-days-picker/
│           └── shift-days-picker.component.ts
│
└── pages/shifts-page/
    └── shifts-page.component.ts
```

---

### 3. Casos de Uso y Validaciones Laborales

#### 3.1. `shift-form.usecase.ts`
* **Cálculo de Jornada y Cumplimiento LGT:**
```typescript
import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ShiftService, CreateShiftRequest, ShiftResponse } from '../../../entities/shift';
import { ToastService } from '../../../shared/ui/toast/toast.service';

@Injectable({ providedIn: 'root' })
export class ShiftFormUseCase {
  private readonly shiftService = inject(ShiftService);
  private readonly toastService = inject(ToastService);

  readonly saving = signal(false);
  readonly validationError = signal<string | null>(null);

  /**
   * Calcula horas efectivas considerando cruce de medianoche y descanso
   */
  calculateEffectiveHours(startTime: string, endTime: string, breakMinutes: number): {
    hours: number;
    isOvernight: boolean;
  } {
    if (!startTime || !endTime) return { hours: 0, isOvernight: false };

    const [sh, sm] = startTime.split(':').map(Number);
    const [eh, em] = endTime.split(':').map(Number);

    let startMinutes = sh * 60 + sm;
    let endMinutes = eh * 60 + em;
    let isOvernight = false;

    if (endMinutes <= startMinutes) {
      endMinutes += 24 * 60; // Cruce de medianoche (+1 día)
      isOvernight = true;
    }

    const totalMinutes = endMinutes - startMinutes - (breakMinutes || 0);
    const hours = Math.max(0, Number((totalMinutes / 60).toFixed(2)));
    return { hours, isOvernight };
  }

  validateLaborLaw(request: CreateShiftRequest): { isValid: boolean; message?: string } {
    const { hours, isOvernight } = this.calculateEffectiveHours(
      request.startTime,
      request.endTime,
      request.breakMinutes
    );

    // Validación nocturna (20:00 - 06:00, max 7 horas)
    if (request.shiftType === 'NIGHT' || isOvernight) {
      if (hours > 7) {
        return {
          isValid: false,
          message: `La jornada nocturna no puede exceder las 7 horas continuas (LGT Bolivia). Calculado: ${hours}h.`
        };
      }
    } else {
      // Jornada diurna estándar (max 8 horas)
      if (hours > 8) {
        return {
          isValid: false,
          message: `La jornada ordinaria diaria no puede exceder las 8 horas (LGT Bolivia). Calculado: ${hours}h.`
        };
      }
    }

    // Tolerancia (5 a 15 min)
    if (request.gracePeriodMinutes < 5 || request.gracePeriodMinutes > 15) {
      return {
        isValid: false,
        message: 'La tolerancia de puntualidad debe estar configurada entre 5 y 15 minutos.'
      };
    }

    return { isValid: true };
  }

  async saveShift(request: CreateShiftRequest, shiftId?: string): Promise<boolean> {
    const val = this.validateLaborLaw(request);
    if (!val.isValid) {
      this.validationError.set(val.message!);
      this.toastService.error(val.message!);
      return false;
    }

    this.saving.set(true);
    this.validationError.set(null);
    try {
      if (shiftId) {
        await firstValueFrom(this.shiftService.updateShift(shiftId, request));
        this.toastService.success('Turno actualizado correctamente');
      } else {
        await firstValueFrom(this.shiftService.createShift(request));
        this.toastService.success('Turno creado exitosamente');
      }
      return true;
    } catch (err: any) {
      this.validationError.set(err?.error?.message || 'Error al guardar el turno');
      return false;
    } finally {
      this.saving.set(false);
    }
  }
}
```

---

### 4. Componentes UI Clave

1. **`shift-card.component.ts`:**
   * Visualizador del turno con badge de tipo (`CONTINUOUS`, `NIGHT`, `ROTATING`), horas efectivas computadas, horario inicio/fin, tolerancia e indicador de cruce de medianoche (`+1d`).
   * Acciones: Editar, Asignar trabajadores y Cambiar estado (Activo / Inactivo).

2. **`shift-assign-modal.component.ts`:**
   * Selector individual o masivo de colaboradores.
   * Fechas de vigencia (`startDate`, `endDate`).
   * Switch para recurrencia semanal.
   * Bloqueo reactivo de guardado si se detecta superposición de asignaciones.

---

### 5. Criterios de Aceptación y Definición de Terminado (DoD) - Fase 3

#### 5.1. Checklist de Calidad Técnica y Código Limpio (Quality Gate)
- [ ] **Componentización y SRP:** La interfaz de turnos se divide en componentes modulares:
  * `shift-card.component.ts` (solo renderizado visual de tarjeta, badges de horario y estado).
  * `shift-form-modal.component.ts` (solo formulario de creación/edición de turno y días).
  * `shift-days-picker.component.ts` (selector interactivo de días de la semana L-D).
  * `shift-assign-modal.component.ts` (solo asignación individual/masiva de trabajadores).
  * `shift-status-badge.component.ts` (indicador de estado Activo/Inactivo con Tailwind semántico).
- [ ] **Tipado Riguroso y Clean Code:** Uso estricto de `ShiftResponse`, `CreateShiftRequest`, `EmployeeShiftAssignmentResponse`. Cero tipos `any`.
- [ ] **Validación Legal Encapsulada:** Todas las reglas de la Ley General del Trabajo (LGT) de Bolivia residen en `ShiftFormUseCase`, sin lógica matemática en los componentes UI.
- [ ] **Reactividad con Signals:** Horas calculadas y banderas de cruce de medianoche se recalculan reactivamente mediante `computed()`.
- [ ] **Verificación Pre-Entrega (Zero-Bug Gate):**
  * `npm run build` ejecutado exitosamente con 0 errores TypeScript.
  * Consola del navegador sin excepciones al calcular horas en vivo mientras el usuario teclea los horarios.
  * Pruebas completas en modo mock (`useMocks: true`) con datos de `hu16-shifts.mock.json`.

#### 5.2. Escenarios BDD de Aceptación Funcional (Gherkin)

##### Escenario 1: Creación Exitosa de Turno Diurno Legal
* **DADO QUE** el usuario de RRHH ingresa al modal de creación de turno.
* **CUANDO** configura un horario de 08:30 a 16:30 con 45 minutos de descanso (7.25 horas efectivas) y tolerancia de 10 minutos.
* **ENTONCES** el caso de uso valida que las horas efectivas son $\le 8.0\text{ h}$, habilita el botón de guardar y registra el turno con estado `ACTIVE`.

##### Escenario 2: Bloqueo de Turno Diurno Excedente de 8 Horas (LGT Bolivia)
* **DADO QUE** el usuario ingresa un horario de 08:00 a 18:00 sin descanso (10 horas efectivas).
* **CUANDO** el formulario evalúa los horarios ingresados.
* **ENTONCES** el caso de uso marca error: *"La jornada ordinaria diaria no puede exceder las 8 horas (LGT Bolivia). Calculado: 10h"*, y deshabilita el envío.

##### Escenario 3: Detección Automática de Cruce de Medianoche y Límite Nocturno (7h)
* **DADO QUE** el usuario define un turno nocturno de 22:00 a 05:00 con 30 minutos de descanso (6.5 horas efectivas).
* **CUANDO** los campos de horario pierden el foco.
* **ENTONCES** el sistema detecta automáticamente `isOvernight = true`, renderiza el badge visual `+1 día`, valida que las horas son $\le 7.0\text{ h}$ nocturnas y permite la creación.

##### Escenario 4: Validación de Rango de Tolerancia de Puntualidad (5 a 15 min)
* **DADO QUE** el usuario intenta configurar una tolerancia de 2 minutos o de 25 minutos.
* **CUANDO** interactúa con el campo de tolerancia.
* **ENTONCES** el validador reactivo muestra: *"La tolerancia de puntualidad debe estar configurada entre 5 y 15 minutos"*, impidiendo valores fuera de la política.

##### Escenario 5: Bloqueo de Solapamiento en Asignaciones de Turno
* **DADO QUE** un empleado ya posee asignado el Turno A del 01/10/2026 al 31/10/2026.
* **CUANDO** se intenta asignarle el Turno B con vigencia del 15/10/2026 al 15/11/2026.
* **ENTONCES** el caso de uso detecta la colisión temporal y notifica al usuario impidiendo la doble asignación simultánea.

---

### 6. Sincronización con el Backend Real Spring Boot (`hrcore-api`)
Para la conexión definitiva con los controladores Java reales (`/api/shifts`), consultar la especificación detallada en:
👉 [07-analisis-integracion-backend-hu15-hu16.md](../07-analisis-integracion-backend-hu15-hu16.md)

**Aspectos clave de alineación:**
* Endpoints bajo `/api/shifts` (sin prefijo `/v1`).
* Mapeo de campos del turno: `crossesMidnight: boolean`, `lateToleranceMinutes: int`, `earlyDepartureToleranceMinutes: int`, `description`.
* Endpoints dedicados para activación: `PATCH /api/shifts/{id}/activate` y `PATCH /api/shifts/{id}/deactivate`.
* Sub-recurso de descansos: `/api/shifts/{id}/breaks` (CRUD para descansos como almuerzo, refrigerio, remunerados o no).
* Asignación individual: `POST /api/shifts/assignments` con array de días de la semana `weekdays: [1, 2, 3, 4, 5]` y ciclo `/cancel`, `/close`.
* Soporte a Excepciones de Calendario (`/api/shifts/calendar-exceptions`) y Horario Resuelto Efectivo (`/api/shifts/schedule`).


