# Software Design Document (SDD) - Frontend HR Core System
## Módulo de Talento y Control Operativo: HU15, HU16, HU17 y HU18
### Arquitectura Escala Empresarial: Feature-Sliced Design (FSD) + Clean Architecture + Mocks Híbridos

---

### 1. Resumen Ejecutivo y Alcance

Este repositorio de diseño arquitectónico especifica técnica y funcionalmente la incorporación de **4 Historias de Usuario (HU)** estratégicas en la aplicación frontend (`hrcore-web`), construida sobre **Angular 20 (Signals, Standalone Components)** y **Tailwind CSS v4**:

1. **HU15: Gestión de Expedientes Laborales** (Digital Dossiers, validación 10MB, PDF/image preview, auditoría RBAC).
2. **HU16: Gestión de Turnos de Trabajo** (Ley General del Trabajo de Bolivia, cruce de medianoche, tolerancia 5-15 min, asignación no solapada).
3. **HU17: Control de Asistencia y Marcaciones** (Reloj con geolocalización contra radio de sucursal, debounce 2 min, timestamps del servidor, regularización auditada).
4. **HU18: Gestión de Ausencias y Vacaciones** (Escala legal boliviana de 15/20/30 días, cómputo estricto de días hábiles, adjuntos probatorios enlazados con HU15, sincronización con asistencia).

#### Principio de No Injerencia y Estabilidad:
> **Regla de Oro:** La implementación es **100% aditiva**. No se modifica ni se altera ningún componente ni servicio preexistente de otros miembros del equipo (`users`, `roles`, `permissions`, `subscription`, `backups`). Únicamente se incorporan rutas hijas independientes en `app.routes.ts` y enlaces en `sidebar.component.ts`.

---

### 2. Estructura Modular y Escalable del SDD Frontend

Para garantizar la mantenibilidad y escalabilidad del proyecto ante la incorporación de futuros casos de uso, el SDD se organiza en módulos de dominio desacoplados:

```
SDD/
├── DataBase/
│   └── dateBaseDiagram.md            # Esquema relacional DDL PostgreSQL (Dominios 6, 7, 8 y 9)
│
├── BackEnd/
│   ├── README.md                     # Contratos de API, endpoints y especificación de conexión
│   └── mocks/                        # Mocks JSON oficiales coincidentes con el esquema de base de datos
│       ├── hu15-dossiers.mock.json   # Mock para expedientes y documentos
│       ├── hu16-shifts.mock.json     # Mock para turnos y días laborables
│       ├── hu17-attendance.mock.json # Mock para registros de asistencia y marcaciones
│       └── hu18-leaves.mock.json     # Mock para feriados, saldos y ausencias
│
└── frontEnd/
    ├── README.md                     # Este documento (Índice Maestro)
    │
    ├── 00-arquitectura-transversal/  # Normas globales, mocking y mapeos
    │   ├── 01-fsd-guidelines.md      # Reglas FSD y barreras de importación
    │   ├── 02-estrategia-mocking.md  # Interceptor HTTP con toggle por environment
    │   ├── 03-mapeo-database-contratos.md # Mapeo SQL <-> DTO <-> TypeScript
    │   └── 04-criterios-aceptacion-y-calidad.md # Estándares DoD y Quality Gates
    │
    ├── 01-fase1-entidades-y-servicios/ # Capa Entities (Dominio e infraestructura API)
    │   └── README.md
    │
    ├── 02-fase2-hu15-expedientes/    # HU15: Features, casos de uso y página de expedientes
    │   └── README.md
    │
    ├── 03-fase3-hu16-turnos/         # HU16: Features, casos de uso y matriz de turnos
    │   └── README.md
    │
    ├── 04-fase4-hu17-asistencia/     # HU17: Features, reloj virtual, GPS y regularizaciones
    │   └── README.md
    │
    ├── 05-fase5-hu18-ausencias/      # HU18: Features, balance de vacaciones y aprobaciones
    │   └── README.md
    │
    │   └── README.md
    │
    ├── 06-fase6-integracion-rutas/   # Integración en app.routes.ts, sidebar y RBAC
    │   └── README.md
    │
    ├── 07-analisis-integracion-backend-hu15-hu16.md # Análisis a profundidad y plan de sincronización backend HU15/HU16
    └── 08-analisis-integracion-backend-hu17-hu18.md # Análisis a profundidad y plan de sincronización backend HU17/HU18
```

---

### 3. Matriz de Trazabilidad por Fase

| Fase | Directorio / Documento | Alcance y Entregables | Relación con Base de Datos |
| :--- | :--- | :--- | :--- |
| **Arquitectura** | [00-arquitectura-transversal](./00-arquitectura-transversal/) | [FSD](./00-arquitectura-transversal/01-fsd-guidelines.md), [Mocking](./00-arquitectura-transversal/02-estrategia-mocking.md), [Mapeo DB](./00-arquitectura-transversal/03-mapeo-database-contratos.md) y [DoD & Calidad](./00-arquitectura-transversal/04-criterios-aceptacion-y-calidad.md). | Transversal |
| **Fase 1** | [01-fase1-entidades-y-servicios](./01-fase1-entidades-y-servicios/README.md) | Interfaces tipadas y servicios `HttpClient` para los 4 slices en `src/app/entities/`. Criterios de calidad y BDD incluidos. | Tablas: `document_type`, `shift`, `attendance_record`, `leave_type`, etc. |
| **Fase 2** | [02-fase2-hu15-expedientes](./02-fase2-hu15-expedientes/README.md) | Casos de uso de subida (10MB), visor de documentos, auditoría inmutable y vista `/dashboard/dossiers`. DoD y escenarios BDD. | Tablas: `employee_document`, `employee_document_audit`. |
| **Fase 3** | [03-fase3-hu16-turnos](./03-fase3-hu16-turnos/README.md) | Reglas LGT Bolivia, cruce de medianoche, tolerancia 5-15 min, asignación y vista `/dashboard/shifts`. DoD y escenarios BDD. | Tablas: `shift`, `shift_day`, `employee_shift_assignment`. |
| **Fase 4** | [04-fase4-hu17-asistencia](./04-fase4-hu17-asistencia/README.md) | Reloj virtual, geolocalización vs radio sucursal, debounce 2 min, regularizaciones y vista `/dashboard/attendance`. DoD y escenarios BDD. | Tablas: `work_location`, `attendance_record`, `attendance_punch`, `attendance_correction`. |
| **Fase 5** | [05-fase5-hu18-ausencias](./05-fase5-hu18-ausencias/README.md) | Escala legal 15/20/30 días, cómputo de días hábiles, adjuntos vinculados con HU15 y vista `/dashboard/absences`. DoD y escenarios BDD. | Tablas: `holiday`, `leave_balance`, `leave_request`. |
| **Fase 6** | [06-fase6-integracion-rutas](./06-fase6-integracion-rutas/README.md) | Enrutamiento seguro en `app.routes.ts`, menú lateral en `sidebar.component.ts`, permisos RBAC y verificación de build. DoD y escenarios BDD. | Tablas: `tenant_role_permission`. |
| **Integración Back HU15/HU16** | [07-analisis-integracion-backend-hu15-hu16.md](./07-analisis-integracion-backend-hu15-hu16.md) | Análisis exhaustivo de los controladores reales Spring Boot (`/api/employee-files`, `/api/shifts`), matriz de brechas y migración. | Dominio 6 y Dominio 7 (`hrcore-api`). |
| **Integración Back HU17/HU18** | [08-analisis-integracion-backend-hu17-hu18.md](./08-analisis-integracion-backend-hu17-hu18.md) | Análisis exhaustivo de los controladores reales Spring Boot (`/api/attendance`, `/api/leaves`), matriz de brechas y plan de 5 fases. | Dominio 8 y Dominio 9 (`hrcore-api`). |

---

### 4. Estrategia de Conexión Dual: Mocks vs Backend Real

El frontend está diseñado para funcionar de manera inmediata en dos modalidades:

1. **Modo Simulado / Mocking (`environment.useMocks: true`):**
   * El `MockHttpInterceptor` intercepta peticiones a `/api/v1/...` y responde con los conjuntos de datos de [`SDD/BackEnd/mocks/`](file:///c:/Users/hp/web/proyectos/proyecto%20U/si2/proyecto_grupalS2_2-2026/hrcore-system/SDD/BackEnd/mocks).
   * Permite desarrollar y validar visualmente todos los flujos sin depender de la base de datos ni del backend Java/Spring Boot.
2. **Modo Producción / Backend Real (`environment.useMocks: false`):**
   * Las peticiones pasan sin modificaciones directamente a `http://localhost:8080/api/v1` a través del proxy configurado en `proxy.conf.json`.
   * **Cero refactorización:** La lógica de casos de uso y componentes permanece 100% inalterada.

---

### 5. Marco de Calidad, Código Limpio y Definición de Terminado (DoD)

Cada fase cuenta con criterios de aceptación explícitos detallados en su respectivo documento y fundamentados en [04-criterios-aceptacion-y-calidad.md](./00-arquitectura-transversal/04-criterios-aceptacion-y-calidad.md):
* **Código Limpio y Componentización:** Componentes pequeños ($\le 250$ líneas), desacoplados y con responsabilidad única. Tipado 100% estricto sin tipos `any`.
* **Zero Fix y Verificación Pre-Entrega:** `npm run build` con 0 errores TypeScript, inspección en consola de navegador libre de excepciones antes de dar por cerrada cada fase.
* **Alineación Arquitectónica:** Cumplimiento de Feature-Sliced Design (FSD) y reactividad nativa de Angular 20 (`signal`, `computed`).
* **Escenarios BDD (Gherkin):** Cada caso de uso cuenta con escenarios formales `Dado-Cuando-Entonces` que deben probarse y validarse funcionalmente.

