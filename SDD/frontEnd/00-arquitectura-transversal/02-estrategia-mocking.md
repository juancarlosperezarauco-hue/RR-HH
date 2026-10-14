# Estrategia de Mocking y Simulación de Backend en Frontend
## Arquitectura de Aislamiento y Transición Transparente a Backend Real

---

### 1. Justificación y Objetivos

Durante el ciclo de desarrollo actual, el equipo de Frontend (`hrcore-web`) debe implementar y probar interacciones complejas de usuario (subida de documentos, línea de tiempo, cálculo de jornadas, marcación con geolocalización, regularizaciones y solicitudes de vacaciones) **sin depender de que los endpoints del Backend (`hrcore-api`) estén completamente construidos o desplegados**.

Para lograr esto sin generar deuda técnica ni reescribir código posteriormente, se implementa una **estrategia de Mocking basada en Interceptor HTTP**:
1. Los servicios Angular (`ShiftService`, `AttendanceService`, etc.) se programan **exactamente como si el backend estuviera en producción**, usando `HttpClient` y las URLs canónicas `/api/v1/...`.
2. Un interceptor (`MockHttpInterceptor`) evalúa la bandera `environment.useMocks`.
3. Si está activa (`true`), el interceptor intercepta las peticiones y responde con los datos simulados provenientes de los archivos de [`SDD/BackEnd/mocks/`](file:///c:/Users/hp/web/proyectos/proyecto%20U/si2/proyecto_grupalS2_2-2026/hrcore-system/SDD/BackEnd/mocks), simulando latencia de red (200-300ms) y mutaciones de estado en memoria.
4. Cuando el backend esté listo, cambiar `environment.useMocks: false` activa la comunicación real sin cambiar ni una sola línea de lógica en los componentes o casos de uso.

---

### 2. Configuración en Entornos (`src/environments/`)

#### Archivo: `src/environments/environment.ts` (Desarrollo)
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
  useMocks: true // <-- Toggle central: false activa el backend real vía proxy.conf.json
};
```

#### Archivo: `src/environments/environment.prod.ts` (Producción)
```typescript
export const environment = {
  production: true,
  apiUrl: '/api/v1',
  useMocks: false // En producción siempre es false
};
```

---

### 3. Implementación del `MockHttpInterceptor`

Ubicación recomendada: `src/app/shared/api/interceptors/mock-http.interceptor.ts`

```typescript
import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { of, delay } from 'rxjs';
import { environment } from '../../../../environments/environment';

// Repositorios de datos en memoria para simular mutaciones en la sesión
import { MOCK_DOSSIERS } from './data/mock-dossiers.data';
import { MOCK_SHIFTS } from './data/mock-shifts.data';
import { MOCK_ATTENDANCE } from './data/mock-attendance.data';
import { MOCK_LEAVES } from './data/mock-leaves.data';

export const mockHttpInterceptor: HttpInterceptorFn = (req, next) => {
  // Si la bandera está desactivada o la URL no es de la API, continuar normal
  if (!environment.useMocks || !req.url.includes('/api/v1')) {
    return next(req);
  }

  const url = req.url;
  const method = req.method;

  // ============================================================
  // HU15: EXPEDIENTES Y DOCUMENTOS
  // ============================================================
  if (url.includes('/document-types') && method === 'GET') {
    return of(new HttpResponse({
      status: 200,
      body: { success: true, message: 'Tipos obtenidos', data: MOCK_DOSSIERS.documentTypes }
    })).pipe(delay(250));
  }

  if (url.includes('/documents') && url.includes('/preview') && method === 'GET') {
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'URL firmada generada',
        data: {
          downloadUrl: 'https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf',
          expiresInMinutes: 15
        }
      }
    })).pipe(delay(200));
  }

  if (url.includes('/documents') && method === 'GET') {
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Documentos obtenidos',
        data: {
          content: MOCK_DOSSIERS.employeeDocuments,
          page: 0,
          size: 20,
          totalElements: MOCK_DOSSIERS.employeeDocuments.length,
          totalPages: 1
        }
      }
    })).pipe(delay(300));
  }

  // ============================================================
  // HU16: TURNOS DE TRABAJO
  // ============================================================
  if (url.includes('/shifts') && method === 'GET') {
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Turnos obtenidos',
        data: {
          content: MOCK_SHIFTS.shifts,
          page: 0,
          size: 20,
          totalElements: MOCK_SHIFTS.shifts.length,
          totalPages: 1
        }
      }
    })).pipe(delay(250));
  }

  if (url.includes('/shifts/assignments') && method === 'GET') {
    return of(new HttpResponse({
      status: 200,
      body: { success: true, message: 'Asignaciones obtenidas', data: MOCK_SHIFTS.employeeShiftAssignments }
    })).pipe(delay(250));
  }

  // ============================================================
  // HU17: CONTROL DE ASISTENCIA
  // ============================================================
  if (url.includes('/attendance/records') && method === 'GET') {
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Registros de asistencia obtenidos',
        data: {
          content: MOCK_ATTENDANCE.attendanceRecords,
          page: 0,
          size: 20,
          totalElements: MOCK_ATTENDANCE.attendanceRecords.length,
          totalPages: 1
        }
      }
    })).pipe(delay(300));
  }

  if (url.includes('/attendance/clock') && method === 'POST') {
    const body = req.body as any;
    const newRecord = {
      id: 'rec-' + Date.now(),
      workDate: new Date().toISOString().substring(0, 10),
      checkInTime: new Date().toISOString(),
      workedHours: 0,
      status: 'PRESENT',
      source: body.method || 'WEB',
      latitude: body.latitude,
      longitude: body.longitude
    };
    return of(new HttpResponse({
      status: 201,
      body: { success: true, message: 'Marcación registrada con éxito (Timestamp del Servidor)', data: newRecord }
    })).pipe(delay(350));
  }

  // ============================================================
  // HU18: AUSENCIAS Y VACACIONES
  // ============================================================
  if (url.includes('/leaves/balances') && method === 'GET') {
    return of(new HttpResponse({
      status: 200,
      body: { success: true, message: 'Saldo obtenido', data: MOCK_LEAVES.leaveBalances[0] }
    })).pipe(delay(250));
  }

  if (url.includes('/leaves/requests') && method === 'GET') {
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Solicitudes obtenidas',
        data: {
          content: MOCK_LEAVES.leaveRequests,
          page: 0,
          size: 20,
          totalElements: MOCK_LEAVES.leaveRequests.length,
          totalPages: 1
        }
      }
    })).pipe(delay(250));
  }

  // Si ninguna ruta mock coincide, dejar pasar la petición al backend
  return next(req);
};
```

---

### 4. Registro Limpio en `app.config.ts`

El interceptor se agrega en la cadena de `provideHttpClient(withInterceptors([...]))` de manera transparente:

```typescript
export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([
        authInterceptor,      // Agrega JWT y Tenant ID
        mockHttpInterceptor,  // Si useMocks == true, responde con mocks JSON
        errorInterceptor      // Manejo de errores 401/403/500
      ])
    )
  ]
};
```

---

### 5. Beneficios de Esta Arquitectura
1. **Desarrollo Desconectado:** Los desarrolladores frontend pueden programar y validar flujos completos sin tener una instancia de Postgres ni Spring Boot levantadas localmente.
2. **Cero Refactorización Posterior:** La estructura de llamadas `HttpClient` no cambia en absoluto cuando el backend entra en funcionamiento; solo se conmuta la variable `environment.useMocks`.
3. **Pruebas de Borde Realistas:** Los mocks simulan latencia y respuestas con el contrato exacto de `dateBaseDiagram.md`.
