# HRCore AI Reports: Fase 3

`POST /api/hr/ai/query` acepta únicamente `{"question":"..."}`. Es una petición
independiente, sin historial. El filtro tenant, el JWT y la sesión de Spring se aplican
antes del controlador. El controlador admite usuarios con permiso de reporte de empleados
o asistencia; el plan propuesto pasa después por la validación y autorización completas
de `AnalyticQueryService`. FastAPI solo propone un plan y no consulta PostgreSQL.

## Contratos

- Éxito: `{"status":"ok","result":QueryResultV1}`.
- Aclaración: `{"status":"needs_clarification","question":"...","reasonCode":"MISSING_DATE_RANGE"}`.
- No soportado: `{"status":"unsupported","message":"...","reasonCode":"..."}`.
- Errores propios de esta ruta: `success:false`, `code`, `message`, `errors:[]` y
  `timestamp`. Pregunta inválida: 400; plan interno inválido: 502; permiso insuficiente:
  403; servicio indisponible: 503; timeout: 504. Los errores de los filtros existentes
  conservan su formato previo.

El servicio interno recibe `contractVersion`, `question`, `catalogVersion` y
`capabilities`, con capacidades organizadas por dataset. Nunca recibe el JWT, tenant,
esquema, roles, credenciales de BD o filas. El contrato de respuesta tiene las tres
variantes estrictas de [ai-planner-v1.schema.json](../hrcore-ai-planner/contracts/ai-planner-v1.schema.json).
El contenido `plan` se valida con el validador QueryPlan v1 de Spring.

## Simulador y configuración local

El simulador reconoce estas frases con fechas ISO:

1. `listar empleados al 2026-01-15` — detalle de `employees.roster`.
2. `retrasos por departamento desde 2026-01-01 hasta antes de 2026-03-01` —
   agregado de `attendance.daily`; la segunda fecha es exclusiva.
3. `retrasos por departamento` — solicita un rango de fechas.
4. Una pregunta de nómina — devuelve `unsupported`.

Las dos primeras frases aceptan otras fechas ISO válidas, pero no son interpretación
libre del lenguaje natural. El mock emite el plan solo si todas las capacidades que
usará están presentes. Está habilitado únicamente con `APP_ENV=development` o `test`
y `AI_PLANNER_MODE=mock`; en otro caso responde 503.

Configurar `AI_PLANNER_URL` en Spring y el mismo `AI_PLANNER_SERVICE_TOKEN` de al menos
32 caracteres en ambos servicios. El planificador forma parte del arranque normal de
Compose y permanece en la red interna, sin publicar su puerto. La URL predeterminada es
`http://serv-hrcore-ai-planner:8000`; la credencial se establece en el archivo local
`.env`, nunca en Git. El cliente espera 1 segundo para conectar y 3 para leer,
rechaza redirecciones y respuestas mayores que 128 KiB. No reintenta automáticamente.

La ruta no funciona si el planificador no está disponible o falta su credencial. Las preguntas completas
y los resultados no se registran. La credencial compartida de PostgreSQL sigue sin
constituir separación de privilegios entre esquemas; el aislamiento se comprueba por
la infraestructura tenant de Spring.

## Línea base y pendientes

La deuda previa de asistencia sigue siendo 11 casos: 7 errores de
`AttendanceCalculationServiceTest`, 1 fallo y 1 error de
`AttendanceOvertimeServiceTest` y 2 errores de `AttendancePunchServiceTest`.
No se modifican en esta fase. Gemini, historial, conversaciones, Angular y
exportaciones quedan fuera de Fase 3.
