# Estándares de Calidad, Código Limpio y Criterios de Aceptación (DoD)
## Marco Transversal de Validación para el Frontend `hrcore-web`

---

### 1. Propósito y Alcance

Este documento define las **Reglas de Calidad y Criterios de Aceptación Transversales (Definition of Done - DoD)** que rigen de forma obligatoria la implementación de todas las fases del Frontend (`HU15`, `HU16`, `HU17`, `HU18`).

Ninguna fase se considera terminada ni apta para entrega o merge si no supera rigurosamente los 4 pilares de verificación técnica descritos a continuación.

---

### 2. Los 4 Pilares de Aceptación Técnica

```
┌────────────────────────────────────────────────────────────────────────┐
│                   PILAR 1: CÓDIGO LIMPIO Y COMPONENTIZADO               │
│   • Principio de Responsabilidad Única (SRP)                           │
│   • Standalone Components modulares y altamente desacoplados           │
│   • Tipado estricto (cero 'any' arbitrarios)                           │
├────────────────────────────────────────────────────────────────────────┤
│                   PILAR 2: RESPETO ARQUITECTÓNICO (FSD + SIGNALS)      │
│   • Capas unidireccionales (pages -> features -> entities -> shared)   │
│   • Casos de uso aislados en 'application/*.usecase.ts'                │
│   • Reactividad moderna: Signals y Computed en componentes UI          │
├────────────────────────────────────────────────────────────────────────┤
│                   PILAR 3: VERIFICACIÓN PRE-ENTREGA (ZERO FIX)         │
│   • 'npm run build' con 0 errores TypeScript y 0 warnings críticos     │
│   • Consola de navegador 100% limpia de errores o memory leaks         │
│   • Pruebas funcionales de extremo a extremo previas a la entrega      │
├────────────────────────────────────────────────────────────────────────┤
│                   PILAR 4: NO INJERENCIA Y MULTI-TENANCY               │
│   • Cero modificaciones a componentes preexistentes de colegas         │
│   • Respeto del discriminator 'tenant_id' y aislamiento de sesión      │
│   • Protección RBAC estricta mediante 'permissionGuard'                │
└────────────────────────────────────────────────────────────────────────┘
```

---

### 3. Matriz Detallada de Criterios de Aceptación Transversales

#### 3.1. Código Limpio y Componentización (Clean Code)
1. **Componentes Pequeños y Enfocados:** Ningún componente UI debe exceder las 250 líneas de código. Las tareas complejas se dividen en subcomponentes especializados (ej: `attendance-table`, `clock-in-widget`, `attendance-status-badge`).
2. **Sin Código Duplicado (DRY):** Formateadores de fecha, divisas, llamadas de alerta y cálculos compartidos se extraen a casos de uso o servicios de `shared/`.
3. **Tipado Estricto de Datos:** Queda terminantemente prohibido el uso de `any` para modelos de negocio. Se utilizan exclusivamente las interfaces definidas en `entities/` sincronizadas con `dateBaseDiagram.md`.
4. **Nomenclatura Estandarizada:**
   * Archivos: `kebab-case` (`employee-file-list.usecase.ts`, `shift-form-modal.component.ts`).
   * Clases/Componentes: `PascalCase` (`EmployeeFileListComponent`).
   * Métodos/Signals: `camelCase` (`remainingDebounceSeconds`, `loadShifts()`).
   * Constantes de entorno: `UPPER_SNAKE_CASE` (`MAX_FILE_SIZE_BYTES`).

#### 3.2. Arquitectura y Reactividad
1. **Barreras FSD Inviolables:**
   * Un componente en `entities/` **nunca** importa nada de `features/` ni `pages/`.
   * Un componente en `features/` **nunca** importa de `pages/`.
   * La UI se comunica con la lógica de negocio exclusivamente a través del caso de uso inyectado (`application/*.usecase.ts`).
2. **Gestión de Estado Reactivo:**
   * Estados mutables se modelan con `signal<T>()`.
   * Valores derivados se computan con `computed()`.
   * Se prohíbe el uso de `.subscribe()` manual dentro del template de los componentes; si se usa RxJS en el servicio HTTP, se resuelve con `firstValueFrom()` dentro del caso de uso antes de llegar a la UI.

#### 3.3. Control de Calidad y "Zero Fix" Pre-Entrega
Antes de marcar cualquier fase como completada o entregarla para revisión, el desarrollador debe ejecutar la siguiente batería de comprobaciones:

```bash
# 1. Compilación estricta
cd hrcore-web
npm run build

# Criterio: Build exitoso sin ningún error de compilación.
```

1. **Inspección de Consola en Tiempo de Ejecución:** Abrir las DevTools del navegador y validar que durante la navegación, creación, edición o eliminación no aparezcan `Unhandled Promise Rejections`, `ExpressionChangedAfterItHasBeenCheckedError`, ni errores de CORS.
2. **Validación Dual (Mocks vs Backend Real):**
   * Con `environment.useMocks: true`: Todas las vistas deben cargar, filtrar y reaccionar con datos de prueba realistas desde `SDD/BackEnd/mocks/`.
   * Con `environment.useMocks: false`: Las peticiones deben dirigirse limpiamente a `http://localhost:8080/api/v1` a través del proxy sin URLs hardcodeadas.

#### 3.4. No Injerencia y Protección RBAC
1. **Integridad del Trabajo de Colegas:**
   * Los archivos pertenecientes a otros módulos (`pages/users-page/`, `pages/roles-page/`, etc.) permanecen **intactos**.
   * Las rutas se agregan de forma estrictamente aditiva como rutas hijas en `app.routes.ts`.
2. **Control de Acceso (RBAC):** Cada ruta y botón de acción sensible está protegido por su respectivo permiso (`permissionGuard` o directiva de permiso).

---

### 4. Formato Estándar para Criterios de Aceptación por Fase

Cada fase específica del SDD debe incluir:
1. **Lista de Chequeo Técnico (Definition of Done):** Verificación de arquitectura, componentes y build.
2. **Criterios Funcionales en formato BDD (Behavior-Driven Development / Gherkin):**
   * `DADO QUE (Given)`: Contexto o precondición.
   * `CUANDO (When)`: Acción del usuario o evento del sistema.
   * `ENTONCES (Then)`: Resultado esperado verificable.
3. **Criterios de Excepción / Casos Borde:** Respuestas ante datos inválidos, cortes de red o límites de tamaño.
