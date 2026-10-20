# HRCore AI Reports — Fase 7

## Funcionalidad y contrato

El chat permite descargar CSV, Excel y PDF desde un resultado actual o desde un mensaje histórico con `reportRunId`. La descarga ejecuta los filtros, fechas, agrupaciones, métricas y orden del QueryPlan persistido sobre los datos actuales. Exporta el conjunto completo que coincide con el plan; la página guardada no restringe el archivo.

```http
POST /api/hr/ai/conversations/{conversationId}/reports/{reportRunId}/export?format=csv
```

`format` admite exactamente `csv`, `xlsx` o `pdf`. No admite cuerpo, parámetros adicionales, formatos repetidos, SQL ni planes del navegador. Los identificadores son UUID y se validan mediante las convenciones existentes.

La respuesta exitosa contiene el archivo binario, `Content-Length`, `Content-Disposition` con un nombre `hrcore-reporte-yyyyMMdd-HHmmss.ext`, `Cache-Control: no-store` y `X-Content-Type-Options: nosniff`. MIME:

| Formato | Content-Type |
| --- | --- |
| CSV | `text/csv;charset=UTF-8` |
| XLSX | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` |
| PDF | `application/pdf` |

Los errores conservan `success`, `code`, `message`, `errors` y `timestamp` del módulo de conversaciones. Se mantienen los códigos de autenticación y consulta existentes. La extensión usa `AI_EXPORT_LIMIT_EXCEEDED` / 413 para límites de representación, volumen o tamaño, `AI_EXPORT_BUSY` / 429 cuando los cupos están ocupados y `QUERY_TIMEOUT` / 504 para tiempo. Un plan incompatible produce 409; formato o entrada inválida, 400; falta de permisos, 403; conversación o run no disponibles para el propietario/tenant, 404.

## Autorización y privacidad

Spring verifica JWT, sesión y tenant mediante sus filtros actuales. El servicio requiere `hr.reports.export`, carga el run exitoso mediante `AiConversationTurnPersistence.loadRun`, comprueba propietario y relación conversación/run y verifica las versiones guardadas y embebidas del plan.

`AnalyticQueryService.export` vuelve a validar el QueryPlan y aplica `AnalyticAuthorizationPolicy.Action.EXPORT`. Esta política exige el permiso del dataset, detalle cuando corresponde, y la unión vigente de permisos de campos, filtros, agrupaciones, métricas, orden y visualización. También conserva la comprobación del esquema efectivo del tenant. No utiliza permisos guardados en el run.

El servicio no depende del orquestador ni del cliente del planner. No llama a FastAPI o Gemini, no guarda nuevos runs, mensajes, metadatos de descarga ni filas personales. Tampoco modifica la autorización o los archivos de los cinco reportes operativos.

La consulta, generación y copia al servlet se ejecutan en el hilo autenticado. No se utiliza `StreamingResponseBody`, procesamiento asíncrono ni propagación manual de contextos. El archivo se termina antes de establecer la respuesta de descarga y se elimina al terminar la copia o ante error. Los mensajes nuevos de log son estáticos y no incluyen filas, filtros, identificadores personales, credenciales o rutas temporales.

## Consulta consistente y recursos

El compilador reutiliza las expresiones y parámetros del motor existente. Su variante interna para exportación utiliza `LIMIT máximo + 1`, `OFFSET 0` y conserva el orden estable por claves de grupo o identificador final. El límite público de páginas de 100 filas y el timeout de 3 segundos de las consultas normales permanecen vigentes.

La exportación usa una única sentencia analítica en una transacción nueva de solo lectura y un cursor JDBC con lotes de 100 filas. PostgreSQL conserva el snapshot de esa sentencia durante los fetches. No ejecuta páginas OFFSET sucesivas, COUNT adicionales ni recálculos Java de métricas. Una fila centinela adicional provoca el rechazo completo, nunca un archivo truncado.

El timeout PostgreSQL se ajusta al tiempo restante antes de la consulta y de cada nuevo lote del cursor. Un presupuesto adicional controla el tiempo total de generación, bytes de entrada y bytes escritos. Los límites de datos de entrada ayudan a acotar los temporales de SXSSF. No se construye una lista con todo el resultado.

Propiedades bajo `app.ai.reports.export`, con validación al iniciar:

| Propiedad | Variable de entorno | Predeterminado |
| --- | --- | --- |
| `tabular-max-rows` | `AI_EXPORT_TABULAR_MAX_ROWS` | 10.000 |
| `tabular-max-bytes` | `AI_EXPORT_TABULAR_MAX_BYTES` | 26.214.400 (25 MiB) |
| `pdf-max-rows` | `AI_EXPORT_PDF_MAX_ROWS` | 500 |
| `pdf-max-columns` | `AI_EXPORT_PDF_MAX_COLUMNS` | 8 |
| `pdf-max-bytes` | `AI_EXPORT_PDF_MAX_BYTES` | 10.485.760 (10 MiB) |
| `timeout-seconds` | `AI_EXPORT_TIMEOUT_SECONDS` | 30 |
| `max-concurrent` | `AI_EXPORT_MAX_CONCURRENT` | 2 por instancia |

La configuración rechaza valores nulos de capacidad: filas tabulares 1–100.000, PDF 1–2.000 filas y 1–8 columnas; bytes tabulares 1 KiB–100 MiB y PDF 1 KiB–20 MiB; tiempo 1–120 segundos y concurrencia 1–8. El cupo se mantiene hasta cerrar la descarga, incluyendo la copia HTTP. Si se alcanza un límite, se elimina el temporal y se recomienda filtrar o cambiar de formato.

El presupuesto de tiempo corresponde a consulta y generación. La transferencia HTTP depende del cliente y de los timeouts del servidor/proxy. La limpieza cubre los caminos normales y excepcionales; un cierre forzado del proceso requiere la política habitual de limpieza del directorio temporal del despliegue.

## Formatos

**CSV:** UTF-8 sin BOM, CRLF y campos entre comillas con escape de comillas duplicadas. Nulos como campos vacíos, fechas ISO, horas locales y enteros sin formato regional. Solo los valores textuales reciben un apóstrofo protector cuando pueden interpretarse como fórmulas, incluidos prefijos de espacio, controles, BOM, espacios Unicode y signos de ancho completo. Las cifras negativas siguen siendo cifras. El CSV legacy no se modifica.

**Excel:** POI `SXSSFWorkbook`, ventana de 100 filas, temporales comprimidos, encabezados sobrios, anchos fijos y primera fila congelada. Hoja fija `Reporte`. Números como celdas numéricas, fechas como fechas con formato `dd/mm/yyyy`, horas locales y UUID como texto, nulos como celdas vacías. Los textos se escriben como texto, nunca como fórmulas. No hay macros, enlaces activos generados ni autoajuste de todas las columnas. Se rechazan enteros de más de 15 dígitos que Excel no podría conservar fielmente y fechas fuera de 1900–9999; CSV conserva esos valores. Por seguridad y compatibilidad con XLSX, ningún formato admite valores de más de 32.767 unidades UTF-16 por celda.

**PDF:** OpenPDF, A4 horizontal, marca HRCore, título por dataset, fecha/hora con zona IANA del tenant, fechas y filtros autorizados del plan y tabla dinámica con encabezados repetidos, ajuste de texto y páginas numeradas. Se procesan bloques de 25 filas para limitar la tabla en memoria. Nulos como «—» y resultado vacío con explicación explícita. No incluye gráficos ni resúmenes de IA.

Se incrustan fuentes Liberation para Unicode. Se verifican acentos españoles, griego y cirílico; los caracteres sin glifo disponible provocan un rechazo explícito con recomendación de Excel/CSV, en lugar de desaparecer del PDF.

Las dependencias existentes se actualizan a POI 5.5.1 (Apache 2.0) y OpenPDF 2.2.5 (LGPL/MPL, elección confirmada para este encargo). Se agrega únicamente el módulo de fuentes `openpdf-fonts-extra` 2.2.5 necesario para Unicode. No se incorporan generadores de documentos al frontend. Referencias: [Apache POI](https://poi.apache.org/download.cgi), [OpenPDF](https://github.com/LibrePDF/OpenPDF).

## Angular

El cliente HTTP agrega un método binario por conversación/run/formato y conserva el interceptor JWT/tenant y la URL `/api` existente. Convierte los errores JSON recibidos como Blob al mismo error HTTP estructurado para mostrar su mensaje mediante el toast actual.

La fachada comprueba `PermissionService`, evita descargas duplicadas, muestra preparación por formato y cancela/invalida solicitudes al cambiar conversación, historial, identidad o destruir el panel. También descarta una respuesta si cambió el run visible o desapareció el permiso. No cambia el resultado mostrado ni ejecuta un refresh para poder exportar.

Los botones se integran en el mensaje de reporte, con disposición flexible, tokens existentes, estados accesibles y la advertencia de que se consultan datos actuales. Funcionan aunque las filas no estén cargadas al reabrir el historial. No hay rutas nuevas, rediseño, lectura manual de tokens ni persistencia de archivos/filas en localStorage o sessionStorage. Las URLs temporales de Blob y los enlaces de descarga se liberan.

## Verificación

La batería Spring de integración y regresiones terminó con 80 pruebas aprobadas, sin errores ni omisiones. Tras revisar visualmente el PDF de ocho columnas se ajustó la paginación para mantener juntas las filas normales y se repitieron las pruebas de archivos y servicio.

| Comprobación | Resultado |
| --- | --- |
| Angular, Chrome Headless 155 en Windows | 105 pruebas aprobadas |
| Build Angular de desarrollo | Correcto |
| Build Angular de producción | Correcto; initial 660,23 kB |
| Lectura real de CSV/XLSX/PDF | CSV parseado, XLSX reabierto con POI, PDF reabierto y texto/páginas comprobados |
| Spring, PostgreSQL 16.15 y FastAPI mock | 80 pruebas aprobadas, 0 fallos, 0 errores, 0 omitidas |
| Pruebas de archivos y servicio tras el ajuste visual final | 12 aprobadas, 0 fallos, 0 errores, 0 omitidas |
| Revisión visual PDF con ocho columnas | Seis páginas renderizadas con Poppler; 70 filas exactamente una vez, filas normales completas y cabeceras/pies correctos |
| `git diff --check` | Correcto |

La batería incluye autorización/revocación, propietario ajeno, run de otra conversación, tenants, versiones incompatibles, formato/cuerpo/parámetros inválidos, archivo vacío, nulos, fechas/horas/números, caracteres y fórmulas, representación de Excel, columnas y volumen de PDF, bytes de entrada/salida, concurrencia y limpieza. La integración HTTP reabre los tres formatos reales, exporta agregaciones y verifica cero llamadas al planner y ausencia de nuevos runs. También inserta datos ficticios entre la consulta inicial y la exportación y comprueba que se exportan aunque la página guardada sea de una fila.

La prueba del cursor procesa 205 filas, mientras otra conexión inserta, elimina y cambia filas entre fetches, y exige exactamente las 205 originales, ordenadas y sin duplicados. Incluye rechazo por fila centinela, cancelación de una sentencia lenta y rechazo antes de otra tanda cuando se consume el plazo total. Las regresiones descargan los cinco CSV operativos por HTTP, conservan las pruebas Angular de Fase 6 y reabren un PDF generado por el servicio de contratos existente.

Comandos utilizados:

```text
mvn "-Dtest=*Analytic*,*QueryPlan*,*SemanticCatalog*,*AiConversation*,*AiQuery*,AiPlannerClientTest,AiReportExportServiceTest,ReportExportDocumentTest,ContractPdfCompatibilityTest,ContractControllerPermissionsTest" test
mvn "-Dtest=ReportExportDocumentTest,AiReportExportServiceTest" test
npm test -- --watch=false --browsers=ChromeHeadless
npm run build -- --configuration development
npm run build
git diff --check
```

Spring requiere las variables de test existentes `AI_REPORTS_TEST_JDBC_URL`, `AI_REPORTS_TEST_DB_USER`, `AI_REPORTS_TEST_DB_PASSWORD` y `AI_PLANNER_TEST_URL`, apuntando exclusivamente a `ai_reports_test` local desechable. FastAPI se ejecuta con `APP_ENV=test`, `AI_PLANNER_MODE=mock` y `GEMINI_ENABLED=false`, sin credenciales externas.

La muestra visual usa 70 filas ficticias con nombres largos, UUID, fechas, horas, números y Unicode, al límite de ocho columnas. Se revisaron las seis páginas renderizadas y se verificó independientemente con pypdf que todas las filas aparecen exactamente una vez y que sus textos permanecen en la misma página. Los dos contenedores desechables se retiran al cerrar la validación; los servicios previos del proyecto se conservan.

Evidencia ignorada por Git: `hrcore-api/target/ai-reports-phase7-tests.log`, `hrcore-api/target/ai-reports-phase7-pdf-final.log`, `hrcore-api/target/ai-reports-phase7-validation/` y `hrcore-web/.angular/ai-reports-phase7-validation/`. Las muestras contienen exclusivamente datos ficticios. Se preservan las advertencias existentes de Angular NG8113 y del presupuesto inicial de 500 kB. No se modifica código de asistencia ni sus once fallos históricos, y no se afirma que la batería completa del repositorio esté aprobada.

## Archivos y preparación para Fase 8

Nuevos en backend:

- `reports/application/ai/{AnalyticExportSink,AiExportLimitException}.java`.
- `reports/application/export/{AiExportProperties,ExportFormat,ExportLabels,ExportBudget,ReportExportDocument,CsvExportDocument,XlsxExportDocument,PdfExportDocument,AiExportBusyException,AiReportExportService}.java`.
- Pruebas `reports/application/export/{AiReportExportServiceTest,ReportExportDocumentTest}.java` y `contracts/application/service/ContractPdfCompatibilityTest.java`.

Modificados en backend:

- `AnalyticQueryService`, `AnalyticQueryExecutor`, `QueryPlanSqlCompiler`, `AiConversationController` y `AiConversationExceptionHandler`.
- Pruebas `AnalyticQueryServiceTest`, `AnalyticQueryPostgresIntegrationTest` y `AiQueryHttpIntegrationTest`.
- `hrcore-api/pom.xml` y un bloque aditivo en `application.yml`, preservando su configuración local previa.

Nuevos en frontend: `features/ai-reports/application/report-download.service.{ts,spec.ts}`.

Modificados en frontend: modelo `ai-conversation.model.ts`, cliente `ai-conversation.service` y prueba, fachada `ai-conversation.facade` y prueba, componente `ai-conversation-chat` TS/HTML y prueba. Documentación nueva: este archivo.

No se necesitan migraciones. Se trabaja en `codex/ai-reports-phase6`, conservando los cambios locales previos de Docker, planner y documentación; no se realizan reset, merge, cambios de autenticación o modificaciones de los CSV legacy.

La Fase 7 queda preparada para revisión de despliegue y para servir de base a la Fase 8, que no se inicia. Quedan como comprobaciones de despliegue la calibración de límites con el hardware real, timeouts del proxy y un recorrido completo del navegador contra Spring; las pruebas del frontend y las pruebas HTTP reales de Spring se ejecutan por separado.
