# HRCore AI Reports — Fase 6

## Funcionalidad y arquitectura

La ruta existente `/dashboard/hr-reports` incorpora las pestañas «Reportes operativos» y «Asistente de reportes». La primera es la inicial y conserva los cinco reportes, fechas, indicadores, actualización y CSV. La instancia del asistente se crea al abrirla por primera vez y se conserva al alternar pestañas.

La integración usa componentes standalone, signals, RxJS, HttpClient, Tailwind y Lucide existentes. No añade dependencias. No cambia rutas, guards, sidebar, interceptores, estilos globales, despliegue ni autenticación. `ReportsService` permanece intacto.

- `entities/reports/model/ai-conversation.model.ts`: contratos de conversaciones, mensajes, turnos, columnas, resultados y refresh.
- `entities/reports/api/ai-conversation.service.ts`: cliente HTTP independiente del servicio legacy.
- `features/ai-reports/application/ai-conversation.facade.ts`: estado limitado a la vida de la pantalla, selección, historial, borradores, intentos idempotentes y resultados actuales.
- `features/ai-reports/application/ai-report-error.ts`: mensajes de error usando el contrato y HTTP reales.
- `features/ai-reports/application/report-presentation.ts`: formato y comprobación de compatibilidad de gráficos.
- `features/ai-reports/ui`: panel, chat, resultado, tabla y gráfico SVG.

## Conversaciones y chat

Permite crear, listar en páginas de 20, seleccionar, recargar y eliminar con confirmación mediante `dialog` nativo. El historial se consulta en páginas de 50; al abrir se localiza su última página sin descargar todas las anteriores. Hay controles para recorrer y recargar páginas manualmente.

La entrada admite Enter para enviar, Shift+Enter para un salto de línea y composición IME. Conserva borradores en memoria. El mensaje en proceso es visible y el envío duplicado se bloquea. Aclaraciones, rechazos, fallos y estados pendientes tienen presentación explícita. No hay polling, streaming ni WebSockets.

El contrato público de turnos utiliza `ok`, `needs_clarification`, `unsupported`, `pending` y `error`. `query_plan` es interno al planificador; no se inventa ese estado en las respuestas HTTP de conversación.

Cada intento lógico tiene un `clientMessageId`. Fallos de red o servicio conservan ese ID para el botón de reintento. El interceptor JWT existente conserva el cuerpo al refrescar sesión. Los rechazos de validación o autorización permiten corregir el borrador. Una nueva pregunta recibe otro ID. Un error persistido por backend puede reproducirse como `error`; iniciar otra consulta requiere una nueva acción del usuario.

La fachada serializa las operaciones que cambian una conversación: bloquea refresh/paginación mientras hay un turno en curso o sin confirmar, y bloquea el envío mientras se actualizan resultados. Así evita invalidar el contexto de un turno desde la propia pantalla. El backend conserva la protección de concurrencia entre pestañas e instancias.

El almacenamiento de conversación admite hasta 4.000 caracteres, pero `PlannerRequest.question` de FastAPI admite 2.000. La UI y la fachada aplican el límite efectivo de **2.000**; no se amplió el contrato ni el consumo del proveedor.

## Contrato HTTP

Todos los clientes usan la URL base existente, las rutas `/api/hr/ai/conversations`, `ApiResponse<T>` y el interceptor de JWT/tenant. El cliente adapta `number` de Spring a `page` del `PageResponse<T>` compartido, sin modificar ese modelo ni otras entidades.

| Acción | Método/ruta | Entrada |
|---|---|---|
| Crear | POST `/api/hr/ai/conversations` | Sin cuerpo |
| Listar | GET `/api/hr/ai/conversations` | `page`, `size` |
| Abrir | GET `/api/hr/ai/conversations/{id}` | Sin cuerpo |
| Historial | GET `/api/hr/ai/conversations/{id}/messages` | `page`, `size` |
| Eliminar | DELETE `/api/hr/ai/conversations/{id}` | Sin cuerpo |
| Enviar | POST `/api/hr/ai/conversations/{id}/messages` | `clientMessageId`, `message` |
| Actualizar/paginar | POST `/api/hr/ai/conversations/{id}/reports/{reportRunId}/refresh` | `page`, `size` opcionales; sin cuerpo |

La ampliación backend autorizada es aditiva y no necesita migraciones:

1. Los mensajes `REPORT` del historial incluyen `reportRunId` de la última ejecución exitosa de su solicitud. Se obtiene en una consulta de proyección por lote para los mensajes de esa página, después de validar conversación y propietario. No expone planes ni filas.
2. Refresh admite `page` entre 0 y 1000 y `size` entre 1 y 100, respetando también el motor. Sin parámetros conserva la página y tamaño del plan guardado.
3. La ejecución modifica una copia del plan, la valida y autoriza mediante `AnalyticQueryService`, y guarda el plan exactamente ejecutado en una nueva ejecución. Conserva las ejecuciones anteriores y no llama al planner.

Una página de resultados no es una instantánea: cada cambio de página consulta datos actuales y puede variar si otro usuario modifica los datos. No se inventa un total de filas; se utilizan `pageNumber`, `pageSize` y `hasMore`.

Los errores conservan códigos y mensajes HTTP del backend. Un `503` no se presenta automáticamente como cuota agotada, porque el contrato actual no distingue siempre esa causa. No hay reintentos adicionales automáticos de preguntas o refresh.

## Tablas, gráficos y reportes históricos

La tabla usa columnas dinámicas tipadas, cabeceras legibles, formato numérico `es-BO`, fechas DATE sin desplazamiento de día y «—» para nulos. Los textos se interpolan; no se renderiza HTML recibido. Las tablas pueden desplazarse horizontalmente mediante teclado.

Los gráficos SVG no añaden librerías. La tabla es la vista inicial. Barras requiere una categoría inequívoca, valores seguros y una métrica conocida del catálogo; acepta como una dimensión un ID y su etiqueta. Líneas requiere fechas válidas, únicas y al menos dos puntos. Puede elegirse la métrica cuando hay varias.

No se representan páginas incompletas, grupos repetidos o nulos, detalles numéricos sin métricas agregadas, números no finitos/inseguros ni más de 30 categorías en barras. No se suman filas, completan períodos, mezclan unidades ni se rellenan nulos. Si el resultado cambia y la visualización elegida deja de corresponder, se muestra la tabla con explicación. La selección de gráfico es local: el DTO de resultados no expone la preferencia visual del QueryPlan.

El historial no contiene filas antiguas. Al reabrir una conversación se muestran mensajes y la opción «Consultar datos actuales». El `reportRunId` recuperado permite una nueva ejecución sin IA. El resultado actual muestra su identificador y la fecha disponible de ejecución. Actualizar no crea preguntas ni reinterpreta su texto.

## Identidad, privacidad y permisos

Se reutilizan `AuthFacade` y la coordinación de sesión. La fachada observa usuario, tenant y versiones de autorización/seguridad; invalida peticiones y limpia mensajes, borradores, intentos y filas al cambiar identidad o destruirse. También comprueba la identidad y selección al recibir respuestas tardías y descarta resultados de otra conversación.

No se leen tokens desde la funcionalidad, no se envían owner/tenant en el cuerpo y no se añaden localStorage, sessionStorage, cachés persistentes o conexiones directas a FastAPI/Gemini/PostgreSQL. Los intentos solo se recuperan mientras vive el panel; después de una recarga completa se consulta el historial, ya que el contrato no expone `clientMessageId` histórico.

La ruta y menú mantienen `hr.reports.employees.read`. Los usuarios con permiso exclusivo de asistencia aún no pueden entrar por esta ruta, aunque el backend AI admita ese permiso. Una evolución posterior puede ofrecer una entrada analítica separada con permisos alternativos sin abrir los reportes legacy; no forma parte de este cambio.

## Verificación ejecutada

Entorno: rama `codex/ai-reports-phase6`, Node 24, dependencias existentes montadas en modo de solo lectura, Chromium Headless 155 en contenedor temporal. La línea base de tests se conservó también en una copia temporal del frontend intacto.

| Comprobación | Resultado |
|---|---|
| Build de desarrollo antes de cambios | Correcto |
| Build de producción antes de cambios | Correcto; initial 658,28 kB |
| Tests Angular de línea base | 55 aprobados |
| Etapa 6A: contratos, interceptor y fachada | 17 aprobados |
| Etapa 6B: chat y regresión legacy focalizada | 25 aprobados |
| Batería final Angular | 96 aprobados, 0 fallos |
| Build final de desarrollo | Correcto |
| Build final de producción | Correcto; initial 660,23 kB |
| Regresiones Spring + PostgreSQL 16.15 + FastAPI mock | 57 pruebas, 0 fallos, 0 errores, 0 omitidas |
| `git diff --check` | Correcto |

Comandos Angular: `npm run build -- --configuration development`, `npm run build`, `npm test -- --watch=false --browsers=ChromeHeadless`. Se usó un wrapper Chromium con `--no-sandbox` únicamente dentro del contenedor desechable de tests; no se cambió la configuración del proyecto.

La evidencia local de la línea base de tests y las verificaciones Angular definitivas se conserva en `.angular/ai-reports-phase6-validation/` (ignorado por Git). El log Spring queda en `hrcore-api/target/ai-reports-phase6-tests.log`. Los builds intermedios se repitieron después de estabilizar los cambios; los resultados de la tabla corresponden a la fuente final.

Las pruebas cubren rutas/cuerpos HTTP, headers JWT/tenant y retry 401 con el mismo ID; creación, selección, paginación, pendientes, errores y limpieza de identidad; respuestas tardías; teclado/IME, confirmación y renderizado seguro; formatos, barras, líneas, fallback y refresh. Las regresiones legacy comprueban los cinco tipos, indicadores, fechas, actualización y las cinco exportaciones CSV, además de la conservación de la instancia del asistente al alternar pestañas.

La integración Spring usa base `ai_reports_test` desechable y FastAPI `APP_ENV=test`, `AI_PLANNER_MODE=mock`, `GEMINI_ENABLED=false`. Incluye lectura del runId histórico, segunda pregunta con contexto capturado, inserción de un empleado ficticio entre ejecuciones, refresh y página siguiente con plan persistido exacto, permisos, aislamiento y verificación Mockito de cero llamadas al planner durante historial/refresh/paginación.

La revisión visual del bundle compilado utilizó un servidor API de fixtures separado, no datos reales. Se comprobó login con identidad ficticia, chat, aclaraciones, solicitudes no soportadas, barras, líneas, actualización, historial reabierto sin filas, cinco reportes, fechas, CSV, navegación de pestañas con teclado, eliminación con Escape/confirmación y cierre de sesión. Se revisaron escritorio a 1280 px y móvil a 390 px sin desbordamiento horizontal del documento. El tema oscuro se comprobó aplicando la clase existente `dark` al HTML del host temporal, sin modificar CSS ni agregar un selector de tema a HRCore. La consola no registró errores.

Estas verificaciones complementan las pruebas HTTP reales de Spring; no constituyen un único recorrido de navegador conectado al Spring desechable. Ese recorrido completo y la revisión de lectores de pantalla siguen como comprobaciones manuales de despliegue.

Incidencias del entorno: el primer arranque de Chrome se intentó antes de terminar su instalación; la línea base se repitió correctamente. Tres tests Java inicialmente encontraron el bloqueo de sockets del sandbox; se repitieron con loopback permitido. El nuevo fixture PostgreSQL inicialmente intentaba eliminar un empleado antes de su período laboral; se corrigió únicamente su limpieza y se repitió el lote completo con éxito.

Permanecen las advertencias NG8113 de componentes landing sin usar y el warning del presupuesto inicial de 500 kB. Ya existían en la línea base; no se alteraron los presupuestos ni se deshabilitaron verificaciones. Los once fallos de asistencia quedan fuera del alcance. No se hicieron llamadas reales a Gemini y no se afirma validación de su precisión.

## Archivos

Nuevos frontend, además de los modelos/cliente/fachada mencionados:

- `features/ai-reports/ui/ai-conversation-panel/ai-conversation-panel.component.{ts,html}`
- `features/ai-reports/ui/ai-conversation-chat/ai-conversation-chat.component.{ts,html}`
- `features/ai-reports/ui/ai-report-result/ai-report-result.component.{ts,html}`
- `features/ai-reports/ui/ai-report-table/ai-report-table.component.ts`
- `features/ai-reports/ui/ai-report-chart/ai-report-chart.component.ts`
- `entities/reports/api/ai-conversation.service.spec.ts`
- `features/ai-reports/application/ai-conversation.facade.spec.ts`
- `features/ai-reports/application/report-presentation.spec.ts`
- `features/ai-reports/ui/ai-conversation-chat/ai-conversation-chat.component.spec.ts`
- `features/ai-reports/ui/ai-report-result/ai-report-result.component.spec.ts`
- `pages/hr-reports-page/hr-reports-page.component.spec.ts`

Modificados frontend: `entities/reports/index.ts` y `pages/hr-reports-page/hr-reports-page.component.ts`.

Modificados backend dentro de `modules/tenant/reports`: `AiConversationMessageResponse`, `AiConversationService`, `AiReportRunJpaRepository`, `AiConversationController`, `AiConversationTurnService`, `AiConversationTurnPersistence`, y sus pruebas `AiConversationServiceTest`, `AiConversationTurnServiceTest`, `AiQueryHttpIntegrationTest`.

## Inicio y comprobación

1. Iniciar Spring y el planner mock según la configuración existente. Para mock se necesita `APP_ENV=development` o `test`; no habilitar Gemini real. Mantener el token interno de servicio configurado entre Spring y FastAPI.
2. En `hrcore-web`, usar las dependencias del lockfile (`npm ci` si no están instaladas) y `npm start`. El proxy existente envía `/api` a `BACKEND_URL` o al backend local; Docker utiliza su destino existente.
3. Entrar con un usuario de prueba autorizado, abrir Reportes de RR. HH. y seleccionar Asistente de reportes.
4. Crear conversación. El mock actual reconoce, entre otras, `listar empleados al 2026-01-15`, `retrasos por departamento desde 2026-01-01 hasta antes de 2026-02-01`, `retrasos por departamento` (aclaración) y `reporte de nómina` (no soportado).
5. Reabrir el historial y pulsar «Consultar datos actuales»; modificar datos ficticios entre ejecuciones y actualizar para comprobar los cambios. El simulador actual reconoce frases limitadas: no es una validación de interpretación libre de seguimientos por Gemini.

No se implementaron exportaciones AI ni Fase 7. Los contenedores PostgreSQL, planner, navegador/fixtures y herramientas de prueba usados aquí se retiran al finalizar; los servicios existentes se conservan.
