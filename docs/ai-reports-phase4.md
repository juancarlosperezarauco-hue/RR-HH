# HRCore AI Reports — Fase 4

## Flujo

`POST /api/hr/ai/query` conserva su contrato público. Spring valida JWT, sesión, tenant y permisos. Para cada petición construye capacidades filtradas con `AnalyticAuthorizationPolicy.authorizedCatalog()` y una proyección semántica del `SemanticCatalogV1`: tipos, operadores, claves de grupo, métricas y relaciones temporales solo de los datasets y campos autorizados. Incluye fecha y zona IANA empresarial cuando ambas están configuradas. No envía JWT, roles, tenant, esquema, filas ni credenciales al planificador. Spring vuelve a validar y autorizar el plan antes de ejecutarlo mediante el motor de Fase 2.

El servicio FastAPI acepta el mensaje interno anterior sin `semanticContext` para conservar compatibilidad con el mock. El adaptador Gemini usa ese contexto por petición; Python no guarda otro catálogo. Las tres variantes de respuesta siguen siendo `query_plan`, `needs_clarification` y `unsupported`. Se agregaron motivos de aclaración y rechazo sin cambiar el cuerpo público de la pregunta.

## Configuración

- `AI_PLANNER_MODE=mock` es el valor por defecto. El mock solo funciona con `APP_ENV=development` o `test` y no requiere clave ni acceso externo.
- `AI_PLANNER_MODE=gemini` requiere además `GEMINI_ENABLED=true`, `GEMINI_MODEL` configurado y `GEMINI_API_KEY` suministrada externamente al proceso. Sin cualquiera de ellos responde `503`; no cambia automáticamente al mock. El repositorio y Docker Compose no almacenan ni inyectan una clave. Un operador autorizado debe gestionarla mediante los mecanismos seguros de despliegue y comprobar las condiciones de uso del proveedor.
- Spring usa 3 s de espera de lectura en modo mock y `AI_PLANNER_READ_TIMEOUT_SECONDS` (65 s por defecto) en modo Gemini. Cada intento del adaptador usa `GEMINI_REQUEST_TIMEOUT_MS` (30 s por defecto, rango admitido de 10 a 120 s), con un máximo de dos intentos para timeout o códigos HTTP transitorios. Deshabilita los reintentos y continuaciones automáticas del SDK. Cada proceso admite como máximo dos solicitudes simultáneas y diez intentos por minuto. Estos límites son **por proceso**, no una cuota distribuida.
- El servicio permanece en el perfil optativo `ai-reports` de Docker Compose y el puerto 8000 no se publica en el host.

## Límites semánticos y privacidad

El planificador pide aclaración cuando se menciona un mes sin año o una fecha relativa sin fecha y zona empresariales disponibles. Las preguntas que requieren combinar dos datasets en un solo QueryPlan, nómina, operaciones de escritura u otras capacidades no publicadas deben responder `unsupported`. Las etiquetas de departamento y cargo son nombres actuales; la atribución histórica sigue en el motor de Spring.

El filtro previo reconoce correos, documentos, teléfonos explícitos, UUID y algunas menciones nominales. Reduce el envío accidental de identificadores personales, pero **no puede reconocer todos los nombres ni todas las formas de datos personales en lenguaje libre**. La activación con tráfico real exige evaluación de privacidad y controles de uso adicionales. El prompt y la respuesta del modelo se tratan como datos no confiables. JSON inválido, claves duplicadas, variantes incorrectas, identificadores fuera de las capacidades y respuestas mayores a 128 KiB se rechazan antes de llegar a Spring; la validación definitiva continúa en Fase 1.

## Verificación pendiente

Las pruebas automatizadas usan un proveedor falso y datos sintéticos; no llaman a la API real ni miden precisión del modelo. Antes de habilitar Gemini en producción se debe confirmar disponibilidad del modelo elegido, compatibilidad de JSON estructurado con la cuenta y región, comportamiento del SDK real, privacidad y calidad de interpretación. Los once fallos antiguos de asistencia permanecen como deuda técnica fuera del alcance de esta fase. Historial, chat, nuevos datasets y Fase 5 quedan pendientes.
