# SDD - Fase 6: Integración de Rutas, Navegación Segura y RBAC
## Integración No Intrusiva en `app.routes.ts`, `sidebar.component.ts` y Verificación

---

### 1. Propósito y Principio de No Injerencia

Conectar las 4 páginas implementadas (`employee-files-page`, `shifts-page`, `attendance-page`, `absences-page`) al sistema principal **sin modificar ni desestabilizar** los módulos existentes desarrollados por tus compañeros (`users`, `roles`, `permissions`, `subscription`, `backups`, etc.).

---

### 2. Configuración en `src/app/app.routes.ts`

Las nuevas rutas se definen como hijas de `/dashboard` con `canActivate: [permissionGuard(...)]` y carga diferida (`loadComponent`):

```typescript
// Fragmento aditivo dentro de children de 'dashboard' en app.routes.ts
{
  path: 'dashboard',
  canActivate: [authGuard],
  loadComponent: () => import('./pages/dashboard-page/dashboard-page.component').then(m => m.DashboardPageComponent),
  children: [
    // --- Rutas preexistentes de tus colegas (NO SE TOCAN) ---
    { path: 'users', canActivate: [permissionGuard('users.list')], ... },
    { path: 'roles', canActivate: [permissionGuard('access.roles.read')], ... },
    { path: 'permissions', canActivate: [permissionGuard('access.permissions.read')], ... },
    // ...

    // ============================================================
    // NUEVAS RUTAS ADITIVAS (HU15, HU16, HU17, HU18)
    // ============================================================
    {
      path: 'dossiers', // HU15: Gestión de Expedientes
      canActivate: [permissionGuard('dossiers.read')],
      loadComponent: () => import('./pages/employee-files-page/employee-files-page.component')
        .then(m => m.EmployeeFilesPageComponent)
    },
    {
      path: 'shifts', // HU16: Gestión de Turnos
      canActivate: [permissionGuard('shifts.read')],
      loadComponent: () => import('./pages/shifts-page/shifts-page.component')
        .then(m => m.ShiftsPageComponent)
    },
    {
      path: 'attendance', // HU17: Control de Asistencia
      canActivate: [permissionGuard('attendance.read')],
      loadComponent: () => import('./pages/attendance-page/attendance-page.component')
        .then(m => m.AttendancePageComponent)
    },
    {
      path: 'absences', // HU18: Permisos y Ausencias
      canActivate: [permissionGuard('absences.read')],
      loadComponent: () => import('./pages/absences-page/absences-page.component')
        .then(m => m.AbsencesPageComponent)
    }
  ]
}
```

---

### 3. Barra Lateral (`sidebar.component.ts`)

En `src/app/widgets/layoutAdmin/sidebar/sidebar.component.ts`, se incorporan los 4 nuevos accesos en `tenantItems`:

```typescript
readonly tenantItems: MenuItem[] = [
  // Módulos base existentes
  { label: 'Usuarios', route: '/dashboard/users', icon: 'users' },
  { label: 'Roles', route: '/dashboard/roles', icon: 'key' },
  { label: 'Permisos', route: '/dashboard/permissions', icon: 'badge-check' },

  // ============================================================
  // NUEVOS MÓDULOS DE GESTIÓN OPERATIVA Y TALENTO
  // ============================================================
  { label: 'Expedientes', route: '/dashboard/dossiers', icon: 'folder-archive' }, // HU15
  { label: 'Turnos', route: '/dashboard/shifts', icon: 'calendar-clock' },       // HU16
  { label: 'Asistencia', route: '/dashboard/attendance', icon: 'user-check' },    // HU17
  { label: 'Ausencias', route: '/dashboard/absences', icon: 'calendar-off' },      // HU18

  // Módulos de cuenta existentes
  { label: 'Suscripción', route: '/dashboard/subscription', icon: 'credit-card' },
  { label: 'Perfil', route: '/dashboard/profile', icon: 'user-circle-2' },
  { label: 'Configuración', route: '/dashboard/settings', icon: 'settings' }
];
```

---

### 4. Matriz de Permisos RBAC

| Permiso Backend | Módulo | Acciones Permitidas |
| :--- | :--- | :--- |
| `dossiers.read` | HU15 | Ver expedientes y documentos públicos |
| `dossiers.write` | HU15 | Subir documentos y actualizar legajos |
| `dossiers.sensitive.read` | HU15 | Acceder a documentos médicos y salariales |
| `dossiers.delete` | HU15 | Dar de baja lógica (soft delete) a documentos |
| `shifts.read` | HU16 | Ver turnos y asignaciones de colaboradores |
| `shifts.write` | HU16 | Crear, editar y desactivar turnos de trabajo |
| `shifts.assign` | HU16 | Asignar turnos masiva o individualmente |
| `attendance.read` | HU17 | Consultar registros y auditorías de asistencia |
| `attendance.clock` | HU17 | Registrar marcaciones de entrada o salida |
| `attendance.regularize`| HU17 | Solicitar / autorizar regularizaciones manuales |
| `absences.read` | HU18 | Consultar solicitudes y saldo de vacaciones |
| `absences.request` | HU18 | Solicitar permisos y adjuntar comprobantes |
| `absences.review` | HU18 | Aprobar o rechazar ausencias de colaboradores |

---

### 5. Criterios de Aceptación y Definición de Terminado (DoD) - Fase 6

#### 5.1. Checklist de Calidad Técnica y Código Limpio (Quality Gate)
- [ ] **No Injerencia Verificada:** Los componentes y rutas de otros miembros del equipo (`users`, `roles`, `permissions`, `subscription`, `backups`) permanecen 100% intactos sin alteraciones destructivas ni modificaciones de imports.
- [ ] **Carga Perezosa (Lazy Loading):** Las 4 páginas (`employee-files-page`, `shifts-page`, `attendance-page`, `absences-page`) se importan mediante `loadComponent: () => import(...)` optimizando el tamaño del bundle inicial.
- [ ] **Protección por Guards (RBAC):** Cada ruta hija cuenta con su respectivo `canActivate: [permissionGuard('...')]`.
- [ ] **Navegación Unificada y Accesible:** Los nuevos ítems en `sidebar.component.ts` se integran con `routerLinkActive="sidebar-active bg-primary text-primary-foreground font-semibold"`, iconos Lucide correspondientes y cierre automático en vista móvil.
- [ ] **Verificación Pre-Entrega (Zero-Bug Gate):**
  * Ejecutar `npm run build` en `hrcore-web` con salida exitosa `0` (cero errores de compilación TypeScript).
  * Inspección en DevTools de la navegación entre las 4 nuevas rutas y las rutas preexistentes sin recarga completa de página (SPA) ni errores de consola.
  * Comprobación en modo mock (`useMocks: true`) con datos representativos de `SDD/BackEnd/mocks/`.

#### 5.2. Escenarios BDD de Aceptación Funcional (Gherkin)

##### Escenario 1: Acceso Autorizado con Permiso Específico
* **DADO QUE** un usuario autenticado posee el permiso `shifts.read`.
* **CUANDO** navega hacia la ruta `/dashboard/shifts` o hace clic en el ícono de "Turnos" en el sidebar.
* **ENTONCES** el guard `permissionGuard` permite el acceso, el componente `ShiftsPageComponent` se carga de forma diferida y el ítem del menú se resalta como activo.

##### Escenario 2: Bloqueo de Acceso No Autorizado por RBAC
* **DADO QUE** un colaborador autenticado carece del permiso `dossiers.read`.
* **CUANDO** intenta ingresar directamente por URL a `/dashboard/dossiers`.
* **ENTONCES** el guard deniega la navegación y redirige al usuario a su ruta por defecto (o muestra la vista de acceso restringido), registrando el intento en consola de seguridad.

##### Escenario 3: No Regresión de Módulos Preexistentes de Colegas
* **DADO QUE** se han incorporado las 4 nuevas rutas y los botones del menú.
* **CUANDO** el usuario navega a `/dashboard/users`, `/dashboard/roles` o `/dashboard/subscription`.
* **ENTONCES** todos los módulos preexistentes funcionan exactamente como antes, sin advertencias de dependencias ni cambios visuales en sus vistas.

##### Escenario 4: Verificación Exitosa de Compilación Global
* **DADO QUE** se completó la integración de todas las fases.
* **CUANDO** se ejecuta el script de empaquetado `npm run build`.
* **ENTONCES** la compilación de Angular genera todos los bundles sin errores de tipado, dependencias circulares ni advertencias críticas.

