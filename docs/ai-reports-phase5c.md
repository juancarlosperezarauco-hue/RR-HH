# HRCore AI Reports — Subfase 5C

## Alcance

5C añade contexto conversacional acotado al flujo de turnos existente de 5B. No añade endpoints públicos, tablas ni migraciones. Spring conserva la autoridad sobre el tenant, el propietario, las capacidades, la validación y la ejecución de cada QueryPlan.

## Contrato interno Spring–FastAPI

`POST /internal/v1/plan` acepta el campo opcional `conversationContext`. Las peticiones antiguas que no lo envían conservan su contrato. Cuando existe, el objeto contiene:

- `hasUsablePlan` y, si es compatible, `lastSuccessfulPlan`, `planVersion` y `catalogVersion`.
- `recentMessages`, con hasta cuatro objetos que solo incluyen `role`, `messageType` y `state`.

Spring recalcula y envía las capacidades y el contexto semántico actuales en cada turno. FastAPI usa el plan previo solo como contexto para proponer un QueryPlan v1 completo; nunca recibe tenant, credenciales, texto histórico, filas, resultados ni permisos delegados. Los estados de respuesta siguen siendo `query_plan`, `needs_clarification` y `unsupported`. La respuesta se valida y ejecuta en Spring mediante `AnalyticQueryService`.

## Selección y seguridad del contexto

Durante la reserva transaccional breve del turno, Spring captura el último `ai_report_runs` exitoso y hasta cuatro metadatos de mensajes previos, sin cargar ni serializar su contenido. No hay una transacción abierta durante las llamadas al planificador ni la ejecución analítica.

Antes de incluir el QueryPlan, Spring comprueba versión del plan y catálogo, valida el contrato con el validador actual y vuelve a autorizarlo con las capacidades efectivas del usuario. Si falta autorización o compatibilidad, elimina el plan del contexto. También lo elimina si tiene filtros sobre `employee.id`, `employee.full_name` o `employment_period.id`. FastAPI aplica una validación defensiva adicional contra las capacidades recibidas.

El límite inicial es de cuatro mensajes y 4.000 caracteres serializados de contexto. Se configura mediante `AI_CONVERSATION_CONTEXT_MAX_MESSAGES` y `AI_CONVERSATION_CONTEXT_MAX_CHARS`; el cliente elimina primero el plan y, si hace falta, todo el contexto para respetar el límite. La memoria no contiene resúmenes generados ni resultados personales.

## Seguimientos

La instrucción de planificación pide devolver siempre un plan completo. En un seguimiento debe conservar condiciones compatibles, añadir condiciones nuevas y reemplazar únicamente las condiciones o el período que el usuario cambia explícitamente. Ante un cambio claro de tema debe ignorar el plan previo. Sin plan compatible, una frase demasiado breve necesita aclaración para que el usuario formule el informe completo. Estas instrucciones reducen ambigüedad, pero la precisión del modelo real no se considera verificada por las pruebas simuladas.

## Concurrencia, idempotencia y persistencia

El contexto se fija junto con la reserva del turno. La reserva sigue usando el bloqueo optimista de `ai_conversations`, impide otra pregunta pendiente y preserva el replay por `clientMessageId`. Al completar, Spring compara la versión de conversación reservada; una versión obsoleta se rechaza aunque el planificador responda tarde.

Solo se guarda el nuevo `ai_report_runs` cuando el QueryPlan fue validado y ejecutado correctamente. Aclaraciones, rechazos y errores no sustituyen el último plan exitoso. `refresh`, lectura e historial siguen consultando persistencia y no llaman al planificador.

## Gemini

`GEMINI_MAX_OUTPUT_TOKENS` controla la salida y conserva 1.024 como valor inicial configurable, no como cuota oficial. Una respuesta de cuota agotada (429 o códigos equivalentes) no se reintenta; se conservan los intentos limitados existentes para otros fallos transitorios.

Gemini está bloqueado cuando `APP_ENV` es `prod` o `production`, salvo que `GEMINI_PRODUCTION_APPROVED=true`. El valor de aprobación de muestra es `false`. Las pruebas y el desarrollo siguen usando el modo mock con datos ficticios; esta fase no realiza llamadas reales al proveedor ni habilita datos de producción para un modelo gratuito.
